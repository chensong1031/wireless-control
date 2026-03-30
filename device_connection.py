#!/usr/bin/env python3
"""
设备注册与连接管理模块
提供二维码生成、设备注册、心跳、消息接收等功能
"""

from flask import Blueprint, jsonify, request, current_app
from app import db
from app.models import Device
import uuid
import time
import base64
import io

try:
    from qrcodegen import QrCode
    QRCODE_AVAILABLE = True
except ImportError:
    QRCODE_AVAILABLE = False
    print("Warning: qrcodegen not installed, QR code will be disabled")

device_conn_bp = Blueprint('device_conn', __name__)

# 内存中的设备连接信息
connected_devices = {}  # {device_id: {token, last_heartbeat, info}}

def generate_token():
    """生成设备认证token"""
    return str(uuid.uuid4())

def generate_qr_data(server_url, token=None):
    """生成二维码数据"""
    if token is None:
        token = generate_token()
    # 二维码包含服务器地址和初始token
    qr_data = f"{server_url}|{token}"
    return qr_data, token

def generate_qr_image(data):
    """生成二维码图片（Base64）- 使用简单SVG格式"""
    if not QRCODE_AVAILABLE:
        return None
    
    qr = QrCode.encode_text(data, QrCode.Ecc.MEDIUM)
    size = qr.get_size()
    
    # 生成简单的SVG二维码
    svg_parts = []
    svg_parts.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" shape-rendering="crispEdges">')
    svg_parts.append(f'<rect width="{size}" height="{size}" fill="white"/>')
    
    for y in range(size):
        for x in range(size):
            if qr.get_module(x, y):
                svg_parts.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="black"/>')
    
    svg_parts.append('</svg>')
    svg = ''.join(svg_parts)
    return f"data:image/svg+xml;base64,{base64.b64encode(svg.encode()).decode()}"


@device_conn_bp.route('/qrcode', methods=['GET'])
def get_qrcode():
    """获取设备注册二维码"""
    server_url = request.host_url.rstrip('/')
    
    # 生成二维码数据
    token = generate_token()
    qr_data = f"{server_url}|{token}"
    
    # 生成二维码图片
    qr_image = generate_qr_image(qr_data)
    
    return jsonify({
        'code': 200,
        'data': {
            'token': token,
            'qr_data': qr_data,
            'qr_image': qr_image,
            'server_url': server_url,
            'expire_at': int(time.time()) + 300  # 5分钟有效
        }
    })


@device_conn_bp.route('/register', methods=['POST'])
def register_device():
    """设备注册"""
    data = request.get_json()
    
    # 验证token
    token = data.get('token')
    if not token:
        return jsonify({'code': 400, 'message': 'Missing token'}), 400
    
    # 检查token是否有效（在二维码中生成但未注册）
    # 这里简化为直接接受token，实际应该存储token验证
    
    device_info = data.get('device_info', {})
    
    # 创建设备记录
    device = Device(
        name=device_info.get('name', f'Device-{device_info.get("model", "Unknown")}'),
        ip=request.remote_addr,
        status='online',
        description=f"{device_info.get('brand', '')} {device_info.get('model', '')} Android {device_info.get('version', '')}"
    )
    db.session.add(device)
    db.session.commit()
    
    # 生成设备认证token
    device_token = generate_token()
    
    # 存储连接信息
    connected_devices[device.id] = {
        'token': device_token,
        'last_heartbeat': int(time.time()),
        'info': device_info,
        'device': device
    }
    
    return jsonify({
        'code': 200,
        'data': {
            'device_id': device.id,
            'device_token': device_token,
            'server_time': int(time.time())
        }
    })


@device_conn_bp.route('/heartbeat', methods=['POST'])
def device_heartbeat():
    """设备心跳"""
    data = request.get_json()
    
    device_id = data.get('device_id')
    device_token = data.get('token')
    
    if not device_id or not device_token:
        return jsonify({'code': 400, 'message': 'Missing device_id or token'}), 400
    
    # 验证token
    conn_info = connected_devices.get(device_id)
    if not conn_info or conn_info['token'] != device_token:
        return jsonify({'code': 401, 'message': 'Invalid token'}), 401
    
    # 更新心跳时间
    conn_info['last_heartbeat'] = int(time.time())
    
    # 更新设备在线状态
    device = Device.query.get(device_id)
    if device:
        device.status = 'online'
        db.session.commit()
    
    return jsonify({
        'code': 200,
        'data': {
            'server_time': int(time.time()),
            'message': 'Heartbeat received'
        }
    })


@device_conn_bp.route('/message', methods=['POST'])
def receive_message():
    """接收设备上报的消息"""
    data = request.get_json()
    
    device_id = data.get('device_id')
    device_token = data.get('token')
    message_type = data.get('type')  # wechat, qq, notification
    content = data.get('content', {})
    
    if not device_id or not device_token:
        return jsonify({'code': 400, 'message': 'Missing device_id or token'}), 400
    
    # 验证token
    conn_info = connected_devices.get(device_id)
    if not conn_info or conn_info['token'] != device_token:
        return jsonify({'code': 401, 'message': 'Invalid token'}), 401
    
    # 处理消息（这里可以存储到数据库或转发给前端）
    print(f"[Device {device_id}] Received {message_type} message: {content}")
    
    # 通知前端（通过WebSocket或轮询）
    current_app.notify_device_message({
        'device_id': device_id,
        'type': message_type,
        'content': content,
        'timestamp': int(time.time())
    })
    
    return jsonify({
        'code': 200,
        'data': {'message': 'Message received'}
    })


@device_conn_bp.route('/command', methods=['GET'])
def get_command():
    """获取待执行命令（设备轮询）"""
    device_id = request.args.get('device_id')
    device_token = request.args.get('token')
    
    if not device_id or not device_token:
        return jsonify({'code': 400, 'message': 'Missing parameters'}), 400
    
    # 验证token
    conn_info = connected_devices.get(int(device_id))
    if not conn_info or conn_info['token'] != device_token:
        return jsonify({'code': 401, 'message': 'Invalid token'}), 401
    
    # TODO: 从任务队列获取待执行命令
    # 这里返回空，实际需要结合任务系统
    
    return jsonify({
        'code': 200,
        'data': {
            'commands': [],
            'server_time': int(time.time())
        }
    })


@device_conn_bp.route('/report/result', methods=['POST'])
def report_result():
    """上报命令执行结果"""
    data = request.get_json()
    
    device_id = data.get('device_id')
    device_token = data.get('token')
    command_id = data.get('command_id')
    result = data.get('result')
    error = data.get('error')
    
    if not device_id or not device_token:
        return jsonify({'code': 400, 'message': 'Missing parameters'}), 400
    
    # 验证token
    conn_info = connected_devices.get(device_id)
    if not conn_info or conn_info['token'] != device_token:
        return jsonify({'code': 401, 'message': 'Invalid token'}), 401
    
    # TODO: 更新命令状态
    print(f"[Device {device_id}] Command {command_id} result: {result}, error: {error}")
    
    return jsonify({
        'code': 200,
        'data': {'message': 'Result received'}
    })


@device_conn_bp.route('/status', methods=['GET'])
def get_device_status():
    """获取设备连接状态"""
    device_id = request.args.get('device_id')
    
    if not device_id:
        return jsonify({'code': 400, 'message': 'Missing device_id'}), 400
    
    conn_info = connected_devices.get(int(device_id))
    if not conn_info:
        return jsonify({'code': 404, 'message': 'Device not connected'}), 404
    
    return jsonify({
        'code': 200,
        'data': {
            'device_id': device_id,
            'last_heartbeat': conn_info['last_heartbeat'],
            'info': conn_info['info'],
            'online': True
        }
    })


@device_conn_bp.route('/disconnect', methods=['POST'])
def device_disconnect():
    """设备断开连接"""
    data = request.get_json()
    
    device_id = data.get('device_id')
    device_token = data.get('token')
    
    if not device_id or not device_token:
        return jsonify({'code': 400, 'message': 'Missing parameters'}), 400
    
    # 验证并移除
    conn_info = connected_devices.get(device_id)
    if conn_info and conn_info['token'] == device_token:
        del connected_devices[device_id]
        
        # 更新设备状态
        device = Device.query.get(device_id)
        if device:
            device.status = 'offline'
            db.session.commit()
    
    return jsonify({
        'code': 200,
        'data': {'message': 'Disconnected'}
    })


def get_connected_devices():
    """获取所有已连接设备"""
    return connected_devices
