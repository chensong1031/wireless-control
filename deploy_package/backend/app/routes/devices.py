from app.routes import device_bp
from flask import request, jsonify, current_app
from flask_jwt_extended import jwt_required
from app.models import Device
from app import db

@device_bp.route('/', methods=['GET'])
@jwt_required()
def get_devices():
    return jsonify({'code': 200, 'data': [d.to_dict() for d in Device.query.all()]})

@device_bp.route('/', methods=['POST'])
@jwt_required()
def add_device():
    data = request.json
    device = Device(name=data.get('name'), ip=data.get('ip'), status='offline')
    db.session.add(device)
    db.session.commit()
    # 推送设备添加通知
    current_app.notify_device_update({'action': 'add', 'device': device.to_dict()})
    return jsonify({'code': 200, 'data': device.to_dict()}), 201

@device_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_device(id):
    device = Device.query.get_or_404(id)
    device_data = device.to_dict()
    db.session.delete(device)
    db.session.commit()
    # 推送设备删除通知
    current_app.notify_device_update({'action': 'delete', 'device': device_data})
    return jsonify({'code': 200, 'message': '删除成功'})

@device_bp.route('/<int:id>/connect', methods=['POST'])
@jwt_required()
def connect_device(id):
    device = Device.query.get_or_404(id)
    device.status = 'online'
    db.session.commit()
    # 推送设备状态变化
    current_app.notify_device_update({'action': 'status_change', 'device': device.to_dict()})
    return jsonify({'code': 200, 'data': device.to_dict()})

@device_bp.route('/<int:id>/disconnect', methods=['POST'])
@jwt_required()
def disconnect_device(id):
    device = Device.query.get_or_404(id)
    device.status = 'offline'
    db.session.commit()
    # 推送设备状态变化
    current_app.notify_device_update({'action': 'status_change', 'device': device.to_dict()})
    return jsonify({'code': 200, 'data': device.to_dict()})

# 批量操作端点
@device_bp.route('/batch/connect', methods=['POST'])
@jwt_required()
def batch_connect_devices():
    data = request.json
    device_ids = data.get('ids', [])
    updated_count = 0
    updated_devices = []
    
    for device_id in device_ids:
        device = Device.query.get(device_id)
        if device:
            device.status = 'online'
            updated_devices.append(device.to_dict())
            updated_count += 1
    
    db.session.commit()
    
    # 批量推送设备状态更新
    for device_data in updated_devices:
        current_app.notify_device_update({'action': 'status_change', 'device': device_data})
    
    return jsonify({
        'code': 200,
        'message': f'成功连接 {updated_count} 个设备',
        'data': updated_devices
    })

@device_bp.route('/batch/disconnect', methods=['POST'])
@jwt_required()
def batch_disconnect_devices():
    data = request.json
    device_ids = data.get('ids', [])
    updated_count = 0
    updated_devices = []
    
    for device_id in device_ids:
        device = Device.query.get(device_id)
        if device:
            device.status = 'offline'
            updated_devices.append(device.to_dict())
            updated_count += 1
    
    db.session.commit()
    
    # 批量推送设备状态更新
    for device_data in updated_devices:
        current_app.notify_device_update({'action': 'status_change', 'device': device_data})
    
    return jsonify({
        'code': 200,
        'message': f'成功断开 {updated_count} 个设备',
        'data': updated_devices
    })

@device_bp.route('/batch/delete', methods=['POST'])
@jwt_required()
def batch_delete_devices():
    data = request.json
    device_ids = data.get('ids', [])
    deleted_count = 0
    deleted_devices = []
    
    for device_id in device_ids:
        device = Device.query.get(device_id)
        if device:
            device_data = device.to_dict()
            db.session.delete(device)
            deleted_devices.append(device_data)
            deleted_count += 1
    
    db.session.commit()
    
    # 批量推送设备删除通知
    for device_data in deleted_devices:
        current_app.notify_device_update({'action': 'delete', 'device': device_data})
    
    return jsonify({
        'code': 200,
        'message': f'成功删除 {deleted_count} 个设备',
        'data': deleted_devices
    })
