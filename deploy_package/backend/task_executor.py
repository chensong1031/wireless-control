"""
多任务并发执行器
使用线程池实现任务并发执行
"""
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading
import logging
from datetime import datetime
from app.models import Task, Device, Script
from app import db
from flask import current_app

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 全局任务通知回调
_task_notifications = []

def register_notification_callback(callback):
    """注册任务通知回调函数"""
    _task_notifications.append(callback)

def notify_task_update(data):
    """通知所有注册的回调函数"""
    for callback in _task_notifications:
        try:
            callback(data)
        except Exception as e:
            logger.error(f"通知回调失败: {e}")


class ConcurrentTaskExecutor:
    """并发任务执行器"""

    def __init__(self, max_workers=10):
        self.max_workers = max_workers
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self.running_tasks = {}
        self.lock = threading.Lock()
        logger.info(f"并发任务执行器已初始化 (最大并发数: {max_workers})")

    def execute_task(self, task_id):
        """执行单个任务"""
        try:
            with current_app.app_context():
                task = Task.query.get(task_id)
                if not task:
                    logger.error(f"任务不存在: {task_id}")
                    return None

                # 更新任务状态为运行中
                task.status = 'running'
                task.started_at = datetime.now()
                db.session.commit()

                # 通知任务开始
                notify_task_update({
                    'action': 'execute',
                    'task': task.to_dict()
                })

                logger.info(f"开始执行任务: {task.name} (ID: {task_id})")

                # 根据任务类型执行不同的逻辑
                if task.task_type == 'script' and task.script_id:
                    result = self._execute_script_task(task)
                elif task.task_type == 'device' and task.device_id:
                    result = self._execute_device_task(task)
                elif task.task_type == 'batch':
                    result = self._execute_batch_task(task)
                else:
                    raise ValueError(f"不支持的任务类型: {task.task_type}")

                # 更新任务状态为完成
                task.status = 'completed'
                task.completed_at = datetime.now()
                task.result = result
                db.session.commit()

                # 通知任务完成
                notify_task_update({
                    'action': 'complete',
                    'task': task.to_dict()
                })

                logger.info(f"任务完成: {task.name} (ID: {task_id})")
                return task.to_dict()

        except Exception as e:
            logger.error(f"任务执行失败: {e}", exc_info=True)
            with current_app.app_context():
                task = Task.query.get(task_id)
                if task:
                    task.status = 'failed'
                    task.completed_at = datetime.now()
                    task.error_message = str(e)
                    db.session.commit()

                    # 通知任务失败
                    notify_task_update({
                        'action': 'fail',
                        'task': task.to_dict()
                    })
            return None

    def _execute_script_task(self, task):
        """执行脚本任务"""
        script = Script.query.get(task.script_id)
        if not script:
            raise ValueError(f"脚本不存在: {task.script_id}")

        logger.info(f"执行脚本: {script.name}")
        # 这里可以集成真实的脚本执行引擎
        # 例如：SSH连接设备、执行命令、收集结果等
        result = f"脚本 '{script.name}' 执行成功\n内容: {script.content[:100]}..."
        return result

    def _execute_device_task(self, task):
        """执行设备操作任务"""
        device = Device.query.get(task.device_id)
        if not device:
            raise ValueError(f"设备不存在: {task.device_id}")

        logger.info(f"操作设备: {device.name} ({device.ip})")
        # 这里可以集成真实的设备控制逻辑
        result = f"设备 '{device.name}' 操作成功\nIP: {device.ip}"
        return result

    def _execute_batch_task(self, task):
        """执行批量任务（操作多个设备）"""
        logger.info(f"执行批量任务: {task.name}")
        # 查询所有在线设备
        devices = Device.query.filter_by(status='online').all()
        if not devices:
            return "没有在线设备可执行任务"

        results = []
        for device in devices:
            try:
                result = f"设备 {device.name} ({device.ip}) 操作成功"
                results.append(result)
            except Exception as e:
                results.append(f"设备 {device.name} 操作失败: {e}")

        return "\n".join(results)

    def execute_task_async(self, task_id):
        """异步执行任务（提交到线程池）"""
        with self.lock:
            if task_id in self.running_tasks:
                logger.warning(f"任务已在运行中: {task_id}")
                return False

            future = self.executor.submit(self.execute_task, task_id)
            self.running_tasks[task_id] = future
            logger.info(f"任务已提交到线程池: {task_id}")
            return True

    def execute_tasks_batch(self, task_ids):
        """批量并发执行多个任务"""
        logger.info(f"批量执行 {len(task_ids)} 个任务")
        results = {}

        # 提交所有任务
        futures = {}
        for task_id in task_ids:
            future = self.executor.submit(self.execute_task, task_id)
            futures[future] = task_id

        # 等待所有任务完成
        for future in as_completed(futures):
            task_id = futures[future]
            try:
                result = future.result()
                results[task_id] = result
            except Exception as e:
                logger.error(f"任务执行异常: {task_id}, {e}")
                results[task_id] = None

        return results

    def cancel_task(self, task_id):
        """取消任务执行"""
        with self.lock:
            if task_id in self.running_tasks:
                future = self.running_tasks[task_id]
                cancelled = future.cancel()
                if cancelled:
                    del self.running_tasks[task_id]
                    logger.info(f"任务已取消: {task_id}")
                return cancelled
            return False

    def get_running_task_count(self):
        """获取正在运行的任务数量"""
        with self.lock:
            return len(self.running_tasks)

    def shutdown(self, wait=True):
        """关闭执行器"""
        self.executor.shutdown(wait=wait)
        logger.info("并发任务执行器已关闭")


# 创建全局执行器实例
task_executor = ConcurrentTaskExecutor(max_workers=10)
