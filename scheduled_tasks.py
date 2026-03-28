"""
定时任务调度系统
使用 APScheduler 实现任务的定时执行
"""
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from flask import request, jsonify
from flask_jwt_extended import jwt_required
from app.models import ScheduledTask, Task
from app import db
from task_executor import task_executor, notify_task_update
from datetime import datetime
import pytz
import logging

logger = logging.getLogger(__name__)

# 创建全局调度器
scheduler = BackgroundScheduler(timezone='Asia/Shanghai')


def execute_scheduled_task(scheduled_task):
    """执行定时任务（通过并发执行器）"""
    try:
        with db.session.begin():
            # 重新加载定时任务对象
            scheduled_task = db.session.merge(scheduled_task)

            # 更新定时任务的执行记录
            scheduled_task.last_run = datetime.now(pytz.timezone('Asia/Shanghai'))
            db.session.commit()

            logger.info(f"执行定时任务: {scheduled_task.name} (ID: {scheduled_task.id})")

            # 通过并发执行器异步执行关联的任务
            task_id = scheduled_task.task_id
            success = task_executor.execute_task_async(task_id)

            if success:
                logger.info(f"定时任务已提交到执行器: {scheduled_task.name}")
            else:
                logger.error(f"定时任务提交失败: {scheduled_task.name}")

            # 计算下次执行时间
            job_id = f'scheduled_task_{scheduled_task.id}'
            job = scheduler.get_job(job_id)
            if job:
                scheduled_task.next_run = job.next_run_time
                db.session.commit()

    except Exception as e:
        logger.error(f"定时任务执行失败: {e}", exc_info=True)


@scheduled_task_bp.route('/', methods=['GET'])
@jwt_required()
def get_scheduled_tasks():
    """获取所有定时任务"""
    tasks = ScheduledTask.query.all()
    return jsonify({
        'code': 200,
        'data': [t.to_dict() for t in tasks],
        'total': len(tasks)
    })


@scheduled_task_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_scheduled_task(id):
    """获取单个定时任务详情"""
    scheduled_task = ScheduledTask.query.get_or_404(id)
    return jsonify({'code': 200, 'data': scheduled_task.to_dict()})


@scheduled_task_bp.route('/', methods=['POST'])
@jwt_required()
def create_scheduled_task():
    """创建定时任务"""
    data = request.json

    # 验证关联的任务是否存在
    task = Task.query.get(data.get('task_id'))
    if not task:
        return jsonify({'code': 400, 'message': '关联的任务不存在'}), 400

    # 验证 cron 表达式
    cron_expression = data.get('cron_expression')
    try:
        CronTrigger.from_crontab(cron_expression)
    except Exception as e:
        return jsonify({'code': 400, 'message': f'Cron表达式无效: {e}'}), 400

    scheduled_task = ScheduledTask(
        name=data.get('name'),
        description=data.get('description', ''),
        cron_expression=cron_expression,
        task_id=data.get('task_id'),
        enabled=data.get('enabled', True)
    )
    db.session.add(scheduled_task)
    db.session.commit()

    logger.info(f"定时任务创建成功: {scheduled_task.name} (ID: {scheduled_task.id})")

    # 添加到调度器
    if scheduled_task.enabled:
        scheduler.add_job(
            func=execute_scheduled_task,
            trigger=CronTrigger.from_crontab(scheduled_task.cron_expression),
            id=f'scheduled_task_{scheduled_task.id}',
            args=[scheduled_task]
        )

        # 计算下次执行时间
        job = scheduler.get_job(f'scheduled_task_{scheduled_task.id}')
        if job:
            scheduled_task.next_run = job.next_run_time
            db.session.commit()

    return jsonify({'code': 200, 'data': scheduled_task.to_dict()}), 201


@scheduled_task_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_scheduled_task(id):
    """更新定时任务"""
    scheduled_task = ScheduledTask.query.get_or_404(id)
    data = request.json

    if 'name' in data:
        scheduled_task.name = data['name']
    if 'description' in data:
        scheduled_task.description = data['description']
    if 'cron_expression' in data:
        # 验证新的 cron 表达式
        try:
            CronTrigger.from_crontab(data['cron_expression'])
            scheduled_task.cron_expression = data['cron_expression']
        except Exception as e:
            return jsonify({'code': 400, 'message': f'Cron表达式无效: {e}'}), 400
    if 'enabled' in data:
        scheduled_task.enabled = data['enabled']

    db.session.commit()

    logger.info(f"定时任务更新成功: {scheduled_task.name} (ID: {id})")

    # 重新调度
    job_id = f'scheduled_task_{scheduled_task.id}'
    if scheduler.get_job(job_id):
        scheduler.remove_job(job_id)

    if scheduled_task.enabled:
        scheduler.add_job(
            func=execute_scheduled_task,
            trigger=CronTrigger.from_crontab(scheduled_task.cron_expression),
            id=job_id,
            args=[scheduled_task]
        )

        job = scheduler.get_job(job_id)
        if job:
            scheduled_task.next_run = job.next_run_time
            db.session.commit()
    else:
        scheduled_task.next_run = None
        db.session.commit()

    return jsonify({'code': 200, 'data': scheduled_task.to_dict()})


@scheduled_task_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_scheduled_task(id):
    """删除定时任务"""
    scheduled_task = ScheduledTask.query.get_or_404(id)

    # 从调度器中移除
    job_id = f'scheduled_task_{id}'
    if scheduler.get_job(job_id):
        scheduler.remove_job(job_id)

    db.session.delete(scheduled_task)
    db.session.commit()

    logger.info(f"定时任务删除成功: ID {id}")

    return jsonify({'code': 200, 'message': '删除成功'})


@scheduled_task_bp.route('/<int:id>/run', methods=['POST'])
@jwt_required()
def run_scheduled_task_now(id):
    """立即执行定时任务"""
    scheduled_task = ScheduledTask.query.get_or_404(id)

    logger.info(f"立即执行定时任务: {scheduled_task.name}")

    # 立即执行
    execute_scheduled_task(scheduled_task)

    return jsonify({
        'code': 200,
        'message': '任务已执行',
        'data': scheduled_task.to_dict()
    })


@scheduled_task_bp.route('/<int:id>/toggle', methods=['POST'])
@jwt_required()
def toggle_scheduled_task(id):
    """启用/禁用定时任务"""
    scheduled_task = ScheduledTask.query.get_or_404(id)
    scheduled_task.enabled = not scheduled_task.enabled
    db.session.commit()

    job_id = f'scheduled_task_{id}'

    if scheduled_task.enabled:
        # 添加到调度器
        scheduler.add_job(
            func=execute_scheduled_task,
            trigger=CronTrigger.from_crontab(scheduled_task.cron_expression),
            id=job_id,
            args=[scheduled_task]
        )

        job = scheduler.get_job(job_id)
        if job:
            scheduled_task.next_run = job.next_run_time
            db.session.commit()

        logger.info(f"定时任务已启用: {scheduled_task.name}")
    else:
        # 从调度器移除
        if scheduler.get_job(job_id):
            scheduler.remove_job(job_id)

        scheduled_task.next_run = None
        db.session.commit()

        logger.info(f"定时任务已禁用: {scheduled_task.name}")

    return jsonify({
        'code': 200,
        'data': scheduled_task.to_dict()
    })


# 启动调度器
scheduler.start()
logger.info("定时任务调度器已启动")
