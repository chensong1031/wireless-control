from flask_sqlalchemy import SQLAlchemy
import bcrypt

db = SQLAlchemy()

class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    email = db.Column(db.String(100), unique=True)
    role = db.Column(db.String(20), default='user')
    
    def set_password(self, password):
        self.password_hash = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
    
    def check_password(self, password):
        return bcrypt.checkpw(password.encode('utf-8'), self.password_hash.encode('utf-8'))
    
    def to_dict(self):
        return {'id': self.id, 'username': self.username, 'email': self.email, 'role': self.role}

class Group(db.Model):
    __tablename__ = 'groups'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False, unique=True)
    description = db.Column(db.Text)
    color = db.Column(db.String(20), default='#1890ff')
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    
    def to_dict(self):
        return {
            'id': self.id, 
            'name': self.name, 
            'description': self.description, 
            'color': self.color,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'device_count': len(self.devices) if hasattr(self, 'devices') else 0
        }

class Device(db.Model):
    __tablename__ = 'devices'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False)
    ip = db.Column(db.String(45), nullable=False)
    status = db.Column(db.String(20), default='offline')
    group_id = db.Column(db.Integer, db.ForeignKey('groups.id'), nullable=True)
    group = db.relationship('Group', backref='devices')
    
    def to_dict(self):
        return {
            'id': self.id, 
            'name': self.name, 
            'ip': self.ip, 
            'status': self.status,
            'group_id': self.group_id,
            'group_name': self.group.name if self.group else None,
            'group_color': self.group.color if self.group else None
        }

class Task(db.Model):
    __tablename__ = 'tasks'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False)
    task_type = db.Column(db.String(20), default='script')
    status = db.Column(db.String(20), default='pending')
    script_id = db.Column(db.Integer)
    
    def to_dict(self):
        return {'id': self.id, 'name': self.name, 'task_type': self.task_type, 'status': self.status, 'script_id': self.script_id}

class Script(db.Model):
    __tablename__ = 'scripts'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False)
    content = db.Column(db.Text, nullable=False)
    script_type = db.Column(db.String(20), default='javascript')
    
    def to_dict(self):
        return {'id': self.id, 'name': self.name, 'content': self.content, 'script_type': self.script_type}
