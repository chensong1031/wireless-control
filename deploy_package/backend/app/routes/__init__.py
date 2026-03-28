from flask import Blueprint

auth_bp = Blueprint('auth_bp', __name__)
device_bp = Blueprint('device_bp', __name__)
task_bp = Blueprint('task_bp', __name__)
script_bp = Blueprint('script_bp', __name__)
statistics_bp = Blueprint('statistics_bp', __name__)
scheduled_task_bp = Blueprint('scheduled_task_bp', __name__)
group_bp = Blueprint('group_bp', __name__)

