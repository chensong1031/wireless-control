from app.routes import group_bp
from flask import request, jsonify, current_app
from flask_jwt_extended import jwt_required
from app.models import DeviceGroup, Device
from app import db
import logging

logger = logging.getLogger(__name__)


@group_bp.route('/', methods=['GET'])
@jwt_required()
def get_groups():
    """获取所有分组"""
    groups = DeviceGroup.query.all()
    return jsonify({
        'code': 200,
        'data': [g.to_dict() for g in groups],
        'total': len(groups)
    })


@group_bp.route('/', methods=['POST'])
@jwt_required()
def create_group():
    """创建分组"""
    data = request.json

    group = DeviceGroup(
        name=data.get('name'),
        description=data.get('description', ''),
        color=data.get('color', '#1890ff')
    )
    db.session.add(group)
    db.session.commit()

    logger.info(f"分组创建成功: {group.name} (ID: {group.id})")

    # 推送分组创建通知
    if hasattr(current_app, 'notify_task_update'):
        current_app.notify_task_update({
            'type': 'group_created',
            'message': f'分组 "{group.name}" 已创建'
        })

    return jsonify({'code': 200, 'data': group.to_dict()}), 201


@group_bp.route('/<int:id>', methods=['GET'])
@jwt_required()
def get_group(id):
    """获取分组详情"""
    group = DeviceGroup.query.get_or_404(id)
    return jsonify({'code': 200, 'data': group.to_dict()})


@group_bp.route('/<int:id>', methods=['PUT'])
@jwt_required()
def update_group(id):
    """更新分组"""
    group = DeviceGroup.query.get_or_404(id)
    data = request.json

    if 'name' in data:
        group.name = data['name']
    if 'description' in data:
        group.description = data['description']
    if 'color' in data:
        group.color = data['color']

    db.session.commit()

    logger.info(f"分组更新成功: {group.name} (ID: {id})")

    # 推送分组更新通知
    if hasattr(current_app, 'notify_task_update'):
        current_app.notify_task_update({
            'type': 'group_updated',
            'message': f'分组 "{group.name}" 已更新'
        })

    return jsonify({'code': 200, 'data': group.to_dict()})


@group_bp.route('/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_group(id):
    """删除分组"""
    group = DeviceGroup.query.get_or_404(id)
    group_name = group.name

    # 先将属于该分组的设备移除分组
    Device.query.filter_by(group_id=id).update({'group_id': None})

    db.session.delete(group)
    db.session.commit()

    logger.info(f"分组删除成功: {group_name} (ID: {id})")

    # 推送分组删除通知
    if hasattr(current_app, 'notify_task_update'):
        current_app.notify_task_update({
            'type': 'group_deleted',
            'message': f'分组 "{group_name}" 已删除'
        })

    return jsonify({'code': 200, 'message': '删除成功'})


@group_bp.route('/<int:id>/devices', methods=['GET'])
@jwt_required()
def get_group_devices(id):
    """获取分组内的设备"""
    group = DeviceGroup.query.get_or_404(id)
    devices = Device.query.filter_by(group_id=id).all()
    return jsonify({
        'code': 200,
        'data': [d.to_dict() for d in devices],
        'total': len(devices)
    })


@group_bp.route('/batch/assign', methods=['POST'])
@jwt_required()
def batch_assign_devices():
    """批量分配设备到分组"""
    data = request.json
    group_id = data.get('group_id')
    device_ids = data.get('device_ids', [])

    updated_count = 0
    for device_id in device_ids:
        device = Device.query.get(device_id)
        if device:
            device.group_id = group_id
            updated_count += 1

    db.session.commit()

    logger.info(f"批量分配设备到分组: {updated_count} 个设备")

    # 推送设备分组更新通知
    if hasattr(current_app, 'notify_task_update'):
        current_app.notify_task_update({
            'type': 'devices_grouped',
            'message': f'{updated_count} 个设备已分配到分组',
            'count': updated_count
        })

    return jsonify({
        'code': 200,
        'message': f'成功分配 {updated_count} 个设备到分组',
        'count': updated_count
    })


@group_bp.route('/<int:id>/batch-connect', methods=['POST'])
@jwt_required()
def batch_connect_group_devices():
    """批量连接分组内的设备"""
    group = DeviceGroup.query.get_or_404(id)
    devices = Device.query.filter_by(group_id=id).all()

    updated_count = 0
    for device in devices:
        if device.status == 'offline':
            device.status = 'online'
            updated_count += 1

    db.session.commit()

    logger.info(f"批量连接设备: {updated_count} 个设备")

    return jsonify({
        'code': 200,
        'message': f'成功连接 {updated_count} 个设备',
        'count': updated_count
    })


@group_bp.route('/<int:id>/batch-disconnect', methods=['POST'])
@jwt_required()
def batch_disconnect_group_devices():
    """批量断开分组内的设备"""
    group = DeviceGroup.query.get_or_404(id)
    devices = Device.query.filter_by(group_id=id).all()

    updated_count = 0
    for device in devices:
        if device.status == 'online':
            device.status = 'offline'
            updated_count += 1

    db.session.commit()

    logger.info(f"批量断开设备: {updated_count} 个设备")

    return jsonify({
        'code': 200,
        'message': f'成功断开 {updated_count} 个设备',
        'count': updated_count
    })

