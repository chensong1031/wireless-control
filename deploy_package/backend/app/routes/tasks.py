from app.routes import task_bp
from flask import request, jsonify, current_app
from flask_jwt_extended import jwt_required
from app.models import Task
from app import db
from task_executor import task_executor, notify_task_update
import logging

logger = logging.getLogger(__name__)


@task_bp.route('/', methods=['GET'])
@jwt_required()
def get_tasks():
    """获取所有任务"""
    tasks = Task.query.all()
    return jsonify({
        'code': 200,
        'data': [t.to_dict() for t in tasks],
        'total': len(tasks)
    })


@task_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_task(id):
    """获取单个任务详情"""
    task = Task.query.get_or_404(id)
    return jsonify({'code': 200, 'data': task.to_dict()})


@task_bp.route('/', methods=['POST'])
@jwt_required()
def create_task():
    """创建任务"""
    data = request.json

    task = Task(
        name=data.get('name'),
        task_type=data.get('task_type', 'script'),
        status='pending',
        script_id=data.get('script_id'),
        device_id=data.get('device_id')
    )
    db.session.add(task)
    db.session.commit()

    logger.info(f"任务创建成功: {task.name} (ID: {task.id})")

    # 推送任务创建通知
    notify_task_update({'action': 'create', 'task': task.to_dict()})

    return jsonify({'code': 200, 'data': task.to_dict()}), 201


@task_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_task(id):
    """删除任务"""
    task = Task.query.get_or_404(id)
    task_data = task.to_dict()

    # 如果任务正在运行，先尝试取消
    if task.status == 'running':
        task_executor.cancel_task(id)

    db.session.delete(task)
    db.session.commit()

    logger.info(f"任务删除成功: {task.name} (ID: {id})")

    # 推送任务删除通知
    notify_task_update({'action': 'delete', 'task': task_data})

    return jsonify({'code': 200, 'message': '删除成功'})


@task_bp.route('/<int:id>/execute', methods=['POST'])
@jwt_required()
def execute_task(id):
    """执行单个任务（异步）"""
    task = Task.query.get_or_404(id)

    if task.status == 'running':
        return jsonify({'code': 400, 'message': '任务已在运行中'}), 400

    # 异步执行任务
    success = task_executor.execute_task_async(id)

    if success:
        logger.info(f"任务已提交执行: {task.name} (ID: {id})")
        return jsonify({
            'code': 200,
            'message': '任务已开始执行',
            'data': task.to_dict()
        })
    else:
        return jsonify({'code': 500, 'message': '任务提交失败'}), 500


@task_bp.route('/<int:id>/cancel', methods=['POST'])
@jwt_required()
def cancel_task(id):
    """取消任务执行"""
    success = task_executor.cancel_task(id)

    if success:
        logger.info(f"任务已取消: ID {id}")
        return jsonify({'code': 200, 'message': '任务已取消'})
    else:
        return jsonify({'code': 400, 'message': '任务无法取消（可能未运行）'}), 400


@task_bp.route('/batch/execute', methods=['POST'])
@jwt_required()
def execute_batch_tasks():
    """批量并发执行多个任务"""
    data = request.json
    task_ids = data.get('task_ids', [])

    if not task_ids:
        return jsonify({'code': 400, 'message': '请提供要执行的任务ID列表'}), 400

    logger.info(f"批量执行任务: {task_ids}")

    # 批量执行
    results = task_executor.execute_tasks_batch(task_ids)

    return jsonify({
        'code': 200,
        'message': f'已提交 {len(task_ids)} 个任务执行',
        'data': results
    })


@task_bp.route('/batch/create', methods=['POST'])
@jwt_required()
def create_batch_tasks():
    """批量创建任务（例如：为一个脚本创建多个设备任务）"""
    data = request.json

    script_id = data.get('script_id')
    device_ids = data.get('device_ids', [])

    if not script_id:
        return jsonify({'code': 400, 'message': '请提供脚本ID'}), 400

    if not device_ids:
        return jsonify({'code': 400, 'message': '请提供设备ID列表'}), 400

    tasks = []
    for device_id in device_ids:
        task = Task(
            name=f"批量任务-{device_id}",
            task_type='script',
            status='pending',
            script_id=script_id,
            device_id=device_id
        )
        db.session.add(task)
        tasks.append(task)

    db.session.commit()

    logger.info(f"批量创建任务: {len(tasks)} 个")

    return jsonify({
        'code': 200,
        'message': f'已创建 {len(tasks)} 个任务',
        'data': [t.to_dict() for t in tasks]
    })


@task_bp.route('/statistics', methods=['GET'])
@jwt_required()
def get_task_statistics():
    """获取任务统计信息"""
    total = Task.query.count()
    pending = Task.query.filter_by(status='pending').count()
    running = Task.query.filter_by(status='running').count()
    completed = Task.query.filter_by(status='completed').count()
    failed = Task.query.filter_by(status='failed').count()

    return jsonify({
        'code': 200,
        'data': {
            'total': total,
            'pending': pending,
            'running': running,
            'completed': completed,
            'failed': failed,
            'running_in_pool': task_executor.get_running_task_count()
        }
    })
