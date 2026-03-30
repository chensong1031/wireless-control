from flask import Flask
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from flask_jwt_extended import JWTManager
from dotenv import load_dotenv
import os
import logging
from flask import jsonify

load_dotenv()

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

db = SQLAlchemy()
jwt = JWTManager()

# 任务通知回调列表
_task_callbacks = []


def notify_task_update(data):
    """通知所有注册的回调函数（任务更新）"""
    for callback in _task_callbacks:
        try:
            callback(data)
        except Exception as e:
            logger.error(f"任务通知回调失败: {e}")


def register_task_callback(callback):
    """注册任务通知回调"""
    _task_callbacks.append(callback)


def create_app():
    app = Flask(__name__)
    app.config['SQLALCHEMY_DATABASE_URI'] = os.getenv('DATABASE_URL', 'sqlite:///app.db')
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['JWT_SECRET_KEY'] = os.getenv('JWT_SECRET_KEY', 'secret')

    CORS(app, resources={r"/api/*": {"origins": "*"}})

    db.init_app(app)
    jwt.init_app(app)

    # 注册任务通知回调到全局
    app.notify_task_update = notify_task_update
    app.register_task_callback = register_task_callback

    # 导入路由
    from app.routes import (
        auth_bp, device_bp, task_bp, script_bp,
        statistics_bp, scheduled_task_bp, group_bp
    )
    
    # 设备消息通知回调列表
    _device_message_callbacks = []
    
    # 导入设备连接模块（在路由之后）
    import sys
    sys.path.insert(0, '/opt/wireless-control/backend')
    from device_connection import device_conn_bp

    def notify_device_message(data):
        """通知所有注册的回调函数（设备消息）"""
        for callback in _device_message_callbacks:
            try:
                callback(data)
            except Exception as e:
                logger.error(f"设备消息通知失败: {e}")

    # 注册蓝图
    app.register_blueprint(auth_bp, url_prefix='/api/auth')
    app.register_blueprint(device_bp, url_prefix='/api/devices')
    app.register_blueprint(task_bp, url_prefix='/api/tasks')
    app.register_blueprint(script_bp, url_prefix='/api/scripts')
    app.register_blueprint(statistics_bp, url_prefix='/api/statistics')
    app.register_blueprint(scheduled_task_bp, url_prefix='/api/scheduled-tasks')
    app.register_blueprint(group_bp, url_prefix='/api/groups')
    app.register_blueprint(device_conn_bp, url_prefix='/api/device-conn')

    # 注册设备消息回调
    app.notify_device_message = notify_device_message
    app.register_device_message_callback = lambda cb: _device_message_callbacks.append(cb)

    @app.route('/')
    def index():
        return jsonify({
            'code': 200,
            'message': '无线群控系统API v2.0 - 支持多任务并发',
            'status': 'running',
            'features': [
                '用户认证系统',
                '设备管理',
                '任务管理',
                '脚本管理',
                '定时任务调度',
                '多任务并发执行',
                '设备分组管理'
            ]
        })

    @app.route('/api/health')
    def health():
        """健康检查端点"""
        return jsonify({
            'code': 200,
            'status': 'healthy',
            'database': 'connected' if db.session.is_active else 'disconnected'
        })

    logger.info("Flask 应用初始化完成")

    return app


app = create_app()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
