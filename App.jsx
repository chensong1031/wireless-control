import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation, useNavigate, Link } from 'react-router-dom';
import { Layout, Menu, message, Dropdown, Form, Input, Select, Button, Modal, Table, Avatar, Card, Space, Tag, ColorPicker, Statistic, Row, Col, Progress, Tooltip, Switch } from 'antd';
import { DesktopOutlined, AppstoreOutlined, FileTextOutlined, DashboardOutlined, UserOutlined, LogoutOutlined, LockOutlined, FolderOutlined, TeamOutlined, ClockCircleOutlined, BarChartOutlined, PlayCircleOutlined, PauseCircleOutlined, SyncOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons';
// import { io } from 'socket.io-client';  // 暂时禁用 WebSocket

const { Header, Content, Sider } = Layout;
const API_BASE = 'http://101.43.0.77';

// 简化的API调用
const fetchAPI = async (url, options = {}) => {
  const token = localStorage.getItem('access_token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  try {
    const res = await fetch(API_BASE + url, { ...options, headers });
    return res.json();
  } catch (err) {
    message.error('请求失败');
    return { code: 500 };
  }
};

// Dashboard组件
const Dashboard = () => {
  const [data, setData] = useState(null);
  const [groups, setGroups] = useState([]);
  useEffect(() => {
    fetchAPI('/api/statistics/overview').then(d => setData(d.data));
    fetchAPI('/api/groups/').then(d => { if (d.code === 200) setGroups(d.data || []); });
  }, []);
  return (
    <div>
      <h2>仪表盘</h2>
      {data && (
        <div style={{ display: 'flex', gap: 16 }}>
          <Card title="设备统计" style={{ width: 200 }}>
            <p>总数: {data.devices?.total || 0}</p>
            <p>在线: {data.devices?.online || 0}</p>
          </Card>
          <Card title="任务统计" style={{ width: 200 }}>
            <p>总数: {data.tasks?.total || 0}</p>
            <p>已完成: {data.tasks?.completed || 0}</p>
          </Card>
          <Card title="分组统计" style={{ width: 200 }}>
            <p>分组数: {groups.length}</p>
          </Card>
        </div>
      )}
    </div>
  );
};

// GroupList组件（分组管理）
const GroupList = () => {
  const [groups, setGroups] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [selectedGroup, setSelectedGroup] = useState(null);

  const fetchGroups = () => {
    fetchAPI('/api/groups/').then(d => { if (d.code === 200) setGroups(d.data || []); });
  };
  useEffect(() => { fetchGroups(); }, []);

  const handleCreate = async (values) => {
    const res = await fetchAPI('/api/groups/', { method: 'POST', body: JSON.stringify(values) });
    if (res.code === 200) {
      message.success('创建成功');
      setModalVisible(false);
      form.resetFields();
      fetchGroups();
    }
  };

  const handleUpdate = async (values) => {
    if (!selectedGroup) return;
    const res = await fetchAPI('/api/groups/' + selectedGroup.id, { method: 'PUT', body: JSON.stringify(values) });
    if (res.code === 200) {
      message.success('更新成功');
      setModalVisible(false);
      setSelectedGroup(null);
      form.resetFields();
      fetchGroups();
    }
  };

  const handleDelete = (id) => {
    if (!window.confirm('确定要删除该分组吗？分组内的设备将移除分组关联。')) return;
    fetchAPI('/api/groups/' + id, { method: 'DELETE' }).then(d => {
      if (d.code === 200) {
        message.success('删除成功');
        fetchGroups();
      }
    });
  };

  const handleBatchConnect = (id) => {
    fetchAPI('/api/groups/' + id + '/batch-connect', { method: 'POST' }).then(d => {
      if (d.code === 200) {
        message.success(d.message);
        fetchGroups();
      }
    });
  };

  const handleBatchDisconnect = (id) => {
    fetchAPI('/api/groups/' + id + '/batch-disconnect', { method: 'POST' }).then(d => {
      if (d.code === 200) {
        message.success(d.message);
        fetchGroups();
      }
    });
  };

  const openCreateModal = () => {
    form.resetFields();
    setSelectedGroup(null);
    setModalVisible(true);
  };

  const openUpdateModal = (group) => {
    setSelectedGroup(group);
    form.setFieldsValue(group);
    setModalVisible(true);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '名称', dataIndex: 'name', key: 'name', render: (text, record) => (
      <Space>
        <Tag color={record.color} icon={<FolderOutlined />}>{text}</Tag>
      </Space>
    )},
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '设备数', dataIndex: 'device_count', key: 'device_count', width: 80 },
    { title: '颜色', dataIndex: 'color', key: 'color', render: color => (
      <div style={{ width: 20, height: 20, backgroundColor: color, borderRadius: '50%' }} />
    )},
    { title: '操作', key: 'action', width: 220, render: (_, record) => (
      <Space>
        <Button size="small" onClick={() => openUpdateModal(record)}>编辑</Button>
        <Button size="small" onClick={() => handleBatchConnect(record.id)}>连接设备</Button>
        <Button size="small" onClick={() => handleBatchDisconnect(record.id)}>断开设备</Button>
        <Button size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
      </Space>
    ) }
  ];

  return (
    <div>
      <Button type="primary" onClick={openCreateModal} style={{ marginBottom: 16 }}>创建分组</Button>
      <Table dataSource={groups} columns={columns} rowKey="id" pagination={{ pageSize: 10 }} />
      <Modal 
        title={selectedGroup ? '编辑分组' : '创建分组'} 
        open={modalVisible} 
        onCancel={() => setModalVisible(false)} 
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={selectedGroup ? handleUpdate : handleCreate} layout="vertical">
          <Form.Item name="name" label="分组名称" rules={[{ required: true, message: '请输入分组名称' }]}>
            <Input placeholder="生产环境" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="分组描述（可选）" />
          </Form.Item>
          <Form.Item name="color" label="颜色" rules={[{ required: true, message: '请选择颜色' }]}>
            <Input type="color" style={{ width: 100 }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

// DeviceList组件（带分组）
const DeviceList = () => {
  const [devices, setDevices] = useState([]);
  const [groups, setGroups] = useState([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [batchGroupModalVisible, setBatchGroupModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [batchForm] = Form.useForm();

  const fetchDevices = () => {
    fetchAPI('/api/devices/').then(d => { if (d.code === 200) setDevices(d.data || []); });
  };
  const fetchGroups = () => {
    fetchAPI('/api/groups/').then(d => { if (d.code === 200) setGroups(d.data || []); });
  };
  useEffect(() => { fetchDevices(); fetchGroups(); }, []);

  const handleAdd = async (values) => {
    const res = await fetchAPI('/api/devices/', { method: 'POST', body: JSON.stringify(values) });
    if (res.code === 200) {
      message.success('添加成功');
      setModalVisible(false);
      form.resetFields();
      fetchDevices();
    }
  };

  const handleBatchConnect = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择设备');
      return;
    }
    const res = await fetchAPI('/api/devices/batch/connect', { method: 'POST', body: JSON.stringify({ ids: selectedRowKeys }) });
    if (res.code === 200) {
      message.success(res.message);
      setSelectedRowKeys([]);
      fetchDevices();
    }
  };

  const handleBatchDisconnect = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择设备');
      return;
    }
    const res = await fetchAPI('/api/devices/batch/disconnect', { method: 'POST', body: JSON.stringify({ ids: selectedRowKeys }) });
    if (res.code === 200) {
      message.success(res.message);
      setSelectedRowKeys([]);
      fetchDevices();
    }
  };

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择设备');
      return;
    }
    if (!window.confirm(`确定要删除 ${selectedRowKeys.length} 个设备吗？`)) {
      return;
    }
    const res = await fetchAPI('/api/devices/batch/delete', { method: 'POST', body: JSON.stringify({ ids: selectedRowKeys }) });
    if (res.code === 200) {
      message.success(res.message);
      setSelectedRowKeys([]);
      fetchDevices();
    }
  };

  const handleBatchAssignGroup = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择设备');
      return;
    }
    const values = await batchForm.validateFields();
    const res = await fetchAPI('/api/groups/batch/assign', { method: 'POST', body: JSON.stringify({
      group_id: values.group_id,
      device_ids: selectedRowKeys
    })});
    if (res.code === 200) {
      message.success(res.message);
      setBatchGroupModalVisible(false);
      batchForm.resetFields();
      setSelectedRowKeys([]);
      fetchDevices();
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP', dataIndex: 'ip', key: 'ip' },
    { title: '状态', dataIndex: 'status', key: 'status', render: s => <span style={{ color: s === 'online' ? 'green' : 'red' }}>{s === 'online' ? '在线' : '离线'}</span> },
    { title: '分组', dataIndex: 'group_name', key: 'group_name', render: (name, record) => (
      record.group_name ? <Tag color={record.group_color}>{name}</Tag> : <span style={{ color: '#999' }}>未分组</span>
    )},
    { title: '操作', key: 'action', width: 200, render: (_, r) => (
      <Space size="small">
        <Button size="small" onClick={() => fetchAPI('/api/devices/' + r.id + '/connect', { method: 'POST' }).then(fetchDevices)}>连接</Button>
        <Button size="small" onClick={() => fetchAPI('/api/devices/' + r.id + '/disconnect', { method: 'POST' }).then(fetchDevices)}>断开</Button>
        <Button size="small" danger onClick={() => fetchAPI('/api/devices/' + r.id, { method: 'DELETE' }).then(fetchDevices)}>删除</Button>
      </Space>
    ) }
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  };

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => setModalVisible(true)}>添加设备</Button>
        <Button onClick={handleBatchConnect} disabled={selectedRowKeys.length === 0}>批量连接</Button>
        <Button onClick={handleBatchDisconnect} disabled={selectedRowKeys.length === 0}>批量断开</Button>
        <Button onClick={() => setBatchGroupModalVisible(true)} disabled={selectedRowKeys.length === 0}>分配分组</Button>
        <Button danger onClick={handleBatchDelete} disabled={selectedRowKeys.length === 0}>批量删除</Button>
      </Space>
      <Table 
        dataSource={devices} 
        columns={columns} 
        rowKey="id" 
        rowSelection={rowSelection}
        pagination={{ pageSize: 10 }} 
      />
      <Modal title="添加设备" open={modalVisible} onCancel={() => setModalVisible(false)} footer={null}>
        <Form form={form} onFinish={handleAdd} layout="vertical">
          <Form.Item name="name" label="设备名称" rules={[{ required: true }]}><Input placeholder="设备名称" /></Form.Item>
          <Form.Item name="ip" label="IP地址" rules={[{ required: true }]}><Input placeholder="192.168.1.1" /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">添加</Button></Form.Item>
        </Form>
      </Modal>
      <Modal title="分配分组" open={batchGroupModalVisible} onCancel={() => setBatchGroupModalVisible(false)} onOk={handleBatchAssignGroup}>
        <Form form={batchForm} layout="vertical">
          <Form.Item name="group_id" label="选择分组" rules={[{ required: true, message: '请选择分组' }]}>
            <Select placeholder="选择分组">
              <Select.Option value="">未分组</Select.Option>
              {groups.map(g => (
                <Select.Option key={g.id} value={g.id}>{g.name}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">确定</Button></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

// ScheduledTaskList组件（定时任务管理）
const ScheduledTaskList = () => {
  const [scheduledTasks, setScheduledTasks] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [selectedTask, setSelectedTask] = useState(null);

  const fetchScheduledTasks = () => {
    setLoading(true);
    fetchAPI('/api/scheduled-tasks/').then(d => {
      if (d.code === 200) setScheduledTasks(d.data || []);
      setLoading(false);
    });
  };

  const fetchTasks = () => {
    fetchAPI('/api/tasks/').then(d => {
      if (d.code === 200) setTasks(d.data || []);
    });
  };

  useEffect(() => {
    fetchScheduledTasks();
    fetchTasks();
    // 每10秒刷新一次定时任务列表
    const interval = setInterval(fetchScheduledTasks, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleCreate = async (values) => {
    const res = await fetchAPI('/api/scheduled-tasks/', {
      method: 'POST',
      body: JSON.stringify(values)
    });
    if (res.code === 200) {
      message.success('创建成功');
      setModalVisible(false);
      form.resetFields();
      fetchScheduledTasks();
    }
  };

  const handleToggle = async (id, enabled) => {
    const res = await fetchAPI('/api/scheduled-tasks/' + id + '/toggle', {
      method: 'POST'
    });
    if (res.code === 200) {
      message.success(enabled ? '已启用' : '已禁用');
      fetchScheduledTasks();
    }
  };

  const handleRunNow = async (id) => {
    const res = await fetchAPI('/api/scheduled-tasks/' + id + '/run', {
      method: 'POST'
    });
    if (res.code === 200) {
      message.success('任务已立即执行');
    }
  };

  const handleDelete = (id) => {
    if (!window.confirm('确定要删除该定时任务吗？')) return;
    fetchAPI('/api/scheduled-tasks/' + id, {
      method: 'DELETE'
    }).then(d => {
      if (d.code === 200) {
        message.success('删除成功');
        fetchScheduledTasks();
      }
    });
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          {record.enabled ? <ClockCircleOutlined style={{ color: '#52c41a' }} /> : <ClockCircleOutlined style={{ color: '#999' }} />}
          {text}
        </Space>
      )
    },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: 'Cron表达式',
      dataIndex: 'cron_expression',
      key: 'cron_expression',
      render: text => (
        <Tooltip title={getCronDescription(text)}>
          <Tag color="blue">{text}</Tag>
        </Tooltip>
      )
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled, record) => (
        <Switch
          checked={enabled}
          onChange={(checked) => handleToggle(record.id, checked)}
        />
      )
    },
    {
      title: '上次执行',
      dataIndex: 'last_run',
      key: 'last_run',
      width: 160,
      render: (time) => time ? new Date(time).toLocaleString('zh-CN') : '-'
    },
    {
      title: '下次执行',
      dataIndex: 'next_run',
      key: 'next_run',
      width: 160,
      render: (time) => time ? new Date(time).toLocaleString('zh-CN') : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => handleRunNow(record.id)}
          >
            立即执行
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  const getCronDescription = (cron) => {
    const parts = cron.split(' ');
    if (parts.length < 5) return cron;
    const [min, hour, day, month, week] = parts;
    return `分:${min} 时:${hour} 日:${day} 月:${month} 周:${week}`;
  };

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => {
          form.resetFields();
          setModalVisible(true);
        }}>
          创建定时任务
        </Button>
        <Button icon={<SyncOutlined />} onClick={fetchScheduledTasks}>
          刷新
        </Button>
      </Space>
      <Table
        dataSource={scheduledTasks}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />
      <Modal
        title="创建定时任务"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item
            name="name"
            label="任务名称"
            rules={[{ required: true, message: '请输入任务名称' }]}
          >
            <Input placeholder="每小时备份" />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={2} placeholder="任务描述（可选）" />
          </Form.Item>
          <Form.Item
            name="cron_expression"
            label="Cron表达式"
            rules={[{ required: true, message: '请输入Cron表达式' }]}
            tooltip="格式：分 时 日 月 周，例如：0 * * * * 表示每小时执行"
          >
            <Input placeholder="0 * * * *" />
          </Form.Item>
          <Form.Item
            name="task_id"
            label="关联任务"
            rules={[{ required: true, message: '请选择关联任务' }]}
          >
            <Select placeholder="选择要执行的任务">
              {tasks.map(t => (
                <Select.Option key={t.id} value={t.id}>
                  {t.name} ({t.task_type})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginBottom: 16 }}>
            <strong>Cron表达式示例：</strong>
            <div style={{ marginTop: 8, fontSize: 12 }}>
              <Tag>0 * * * *</Tag> 每小时执行
              <Tag style={{ marginLeft: 8 }}>0 0 * * *</Tag> 每天零点执行
              <Tag style={{ marginLeft: 8 }}>*/30 * * * *</Tag> 每30分钟执行
              <Tag style={{ marginLeft: 8 }}>0 9 * * 1-5</Tag> 工作日9点执行
            </div>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

// TaskStatistics组件（任务统计）
const TaskStatistics = () => {
  const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchStatistics = () => {
    setLoading(true);
    fetchAPI('/api/tasks/statistics').then(d => {
      if (d.code === 200) setStatistics(d.data);
      setLoading(false);
    });
  };

  useEffect(() => {
    fetchStatistics();
    // 每5秒刷新一次统计信息
    const interval = setInterval(fetchStatistics, 5000);
    return () => clearInterval(interval);
  }, []);

  if (!statistics) {
    return <div style={{ textAlign: 'center', padding: 50 }}>加载中...</div>;
  }

  const total = statistics.total || 0;
  const pending = statistics.pending || 0;
  const running = statistics.running || 0;
  const completed = statistics.completed || 0;
  const failed = statistics.failed || 0;
  const runningInPool = statistics.running_in_pool || 0;

  const completedPercent = total > 0 ? Math.round((completed / total) * 100) : 0;
  const runningPercent = total > 0 ? Math.round((running / total) * 100) : 0;
  const failedPercent = total > 0 ? Math.round((failed / total) * 100) : 0;

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Button icon={<SyncOutlined />} onClick={fetchStatistics} loading={loading}>
          刷新统计
        </Button>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总任务数"
              value={total}
              prefix={<AppstoreOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待执行"
              value={pending}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="运行中"
              value={running}
              prefix={<SyncOutlined spin />}
              valueStyle={{ color: '#52c41a' }}
            />
            <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
              线程池: {runningInPool}
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已完成"
              value={completed}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={12}>
          <Card title="任务完成率" style={{ textAlign: 'center' }}>
            <Progress
              type="circle"
              percent={completedPercent}
              format={(percent) => `${percent}%`}
              strokeColor={{
                '0%': '#108ee9',
                '100%': '#87d068',
              }}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="任务状态分布">
            <div style={{ marginTop: 24 }}>
              <div style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>运行中</span>
                  <span>{running} ({runningPercent}%)</span>
                </div>
                <Progress percent={runningPercent} strokeColor="#52c41a" />
              </div>
              <div style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>已完成</span>
                  <span>{completed} ({completedPercent}%)</span>
                </div>
                <Progress percent={completedPercent} strokeColor="#722ed1" />
              </div>
              <div style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>失败</span>
                  <span>{failed} ({failedPercent}%)</span>
                </div>
                <Progress percent={failedPercent} strokeColor="#ff4d4f" />
              </div>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span>待执行</span>
                  <span>{pending} ({Math.round((pending / total) * 100) || 0}%)</span>
                </div>
                <Progress percent={Math.round((pending / total) * 100) || 0} strokeColor="#faad14" />
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Card title="并发执行状态">
        <Row gutter={16}>
          <Col span={8}>
            <Statistic
              title="线程池运行中"
              value={runningInPool}
              suffix={`/ 10`}
              valueStyle={{ color: runningInPool > 0 ? '#52c41a' : '#999' }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="线程池使用率"
              value={runningInPool * 10}
              suffix="%"
              valueStyle={{ color: runningInPool > 7 ? '#ff4d4f' : '#1890ff' }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="可用线程"
              value={10 - runningInPool}
              valueStyle={{ color: '#52c41a' }}
            />
          </Col>
        </Row>
      </Card>
    </div>
  );
};

// TaskList组件（增强版 - 支持批量执行、取消）
const TaskList = () => {
  const [tasks, setTasks] = useState([]);
  const [scripts, setScripts] = useState([]);
  const [devices, setDevices] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [batchModalVisible, setBatchModalVisible] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [form] = Form.useForm();
  const [batchForm] = Form.useForm();

  const fetchTasks = () => {
    fetchAPI('/api/tasks/').then(d => { if (d.code === 200) setTasks(d.data || []); });
  };

  const fetchScripts = () => {
    fetchAPI('/api/scripts/').then(d => { if (d.code === 200) setScripts(d.data || []); });
  };

  const fetchDevices = () => {
    fetchAPI('/api/devices/').then(d => { if (d.code === 200) setDevices(d.data || []); });
  };

  const fetchStatistics = () => {
    fetchAPI('/api/tasks/statistics').then(d => { if (d.code === 200) setStatistics(d.data); });
  };

  useEffect(() => {
    fetchTasks();
    fetchScripts();
    fetchDevices();
    fetchStatistics();
    // 每5秒刷新任务列表和统计
    const interval = setInterval(() => {
      fetchTasks();
      fetchStatistics();
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleCreate = async (values) => {
    const res = await fetchAPI('/api/tasks/', { method: 'POST', body: JSON.stringify(values) });
    if (res.code === 200) {
      message.success('创建成功');
      setModalVisible(false);
      form.resetFields();
      fetchTasks();
      fetchStatistics();
    }
  };

  const handleExecute = async (id) => {
    const res = await fetchAPI('/api/tasks/' + id + '/execute', { method: 'POST' });
    if (res.code === 200) {
      message.success('任务已提交执行');
      fetchTasks();
      fetchStatistics();
    }
  };

  const handleCancel = async (id) => {
    const res = await fetchAPI('/api/tasks/' + id + '/cancel', { method: 'POST' });
    if (res.code === 200) {
      message.success('任务已取消');
      fetchTasks();
      fetchStatistics();
    }
  };

  const handleBatchExecute = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择任务');
      return;
    }
    const res = await fetchAPI('/api/tasks/batch/execute', {
      method: 'POST',
      body: JSON.stringify({ task_ids: selectedRowKeys })
    });
    if (res.code === 200) {
      message.success(`已提交 ${selectedRowKeys.length} 个任务执行`);
      setSelectedRowKeys([]);
      fetchTasks();
      fetchStatistics();
    }
  };

  const handleBatchCreate = async () => {
    const values = await batchForm.validateFields();
    const selectedDeviceIds = values.device_ids || [];
    if (selectedDeviceIds.length === 0) {
      message.warning('请至少选择一个设备');
      return;
    }

    const res = await fetchAPI('/api/tasks/batch/create', {
      method: 'POST',
      body: JSON.stringify({
        script_id: values.script_id,
        device_ids: selectedDeviceIds
      })
    });
    if (res.code === 200) {
      message.success(`已创建 ${res.data.length} 个任务`);
      setBatchModalVisible(false);
      batchForm.resetFields();
      fetchTasks();
      fetchStatistics();
    }
  };

  const handleDelete = (id) => {
    if (!window.confirm('确定要删除该任务吗？')) return;
    fetchAPI('/api/tasks/' + id, { method: 'DELETE' }).then(d => {
      if (d.code === 200) {
        message.success('删除成功');
        fetchTasks();
        fetchStatistics();
      }
    });
  };

  const getStatusColor = (status) => {
    const colors = {
      pending: 'default',
      running: 'processing',
      completed: 'success',
      failed: 'error'
    };
    return colors[status] || 'default';
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '类型', dataIndex: 'task_type', key: 'task_type', render: t => <Tag>{t}</Tag> },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: s => <Tag color={getStatusColor(s)}>{s}</Tag>
    },
    {
      title: '创建时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 160,
      render: t => t ? new Date(t).toLocaleString('zh-CN') : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_, r) => (
        <Space>
          {r.status === 'pending' && (
            <Button
              size="small"
              icon={<PlayCircleOutlined />}
              type="primary"
              onClick={() => handleExecute(r.id)}
            >
              执行
            </Button>
          )}
          {r.status === 'running' && (
            <Button
              size="small"
              icon={<PauseCircleOutlined />}
              onClick={() => handleCancel(r.id)}
            >
              取消
            </Button>
          )}
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(r.id)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
    getCheckboxProps: (record) => ({
      disabled: record.status === 'running'
    })
  };

  return (
    <div>
      {statistics && (
        <Card style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={6}>
              <Statistic title="总数" value={statistics.total} />
            </Col>
            <Col span={6}>
              <Statistic title="运行中" value={statistics.running} valueStyle={{ color: '#52c41a' }} />
            </Col>
            <Col span={6}>
              <Statistic title="已完成" value={statistics.completed} valueStyle={{ color: '#722ed1' }} />
            </Col>
            <Col span={6}>
              <Statistic title="失败" value={statistics.failed} valueStyle={{ color: '#ff4d4f' }} />
            </Col>
          </Row>
        </Card>
      )}

      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => {
          form.resetFields();
          setModalVisible(true);
        }}>
          创建任务
        </Button>
        <Button onClick={() => {
          batchForm.resetFields();
          setBatchModalVisible(true);
        }}>
          批量创建
        </Button>
        <Button
          icon={<PlayCircleOutlined />}
          onClick={handleBatchExecute}
          disabled={selectedRowKeys.length === 0}
        >
          批量执行
        </Button>
        <Button icon={<SyncOutlined />} onClick={() => {
          fetchTasks();
          fetchStatistics();
        }}>
          刷新
        </Button>
      </Space>

      <Table
        dataSource={tasks}
        columns={columns}
        rowKey="id"
        rowSelection={rowSelection}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title="创建任务"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item name="name" label="任务名称" rules={[{ required: true }]}>
            <Input placeholder="任务名称" />
          </Form.Item>
          <Form.Item name="task_type" label="任务类型" rules={[{ required: true }]}>
            <Select placeholder="选择任务类型">
              <Select.Option value="script">脚本执行</Select.Option>
              <Select.Option value="device">设备操作</Select.Option>
              <Select.Option value="batch">批量任务</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="script_id" label="关联脚本">
            <Select placeholder="选择脚本（可选）">
              {scripts.map(s => (
                <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="device_id" label="关联设备">
            <Select placeholder="选择设备（可选）">
              {devices.map(d => (
                <Select.Option key={d.id} value={d.id}>{d.name} ({d.ip})</Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量创建任务"
        open={batchModalVisible}
        onCancel={() => setBatchModalVisible(false)}
        onOk={handleBatchCreate}
        width={600}
      >
        <Form form={batchForm} layout="vertical">
          <Form.Item
            name="script_id"
            label="选择脚本"
            rules={[{ required: true, message: '请选择脚本' }]}
          >
            <Select placeholder="选择要执行的脚本">
              {scripts.map(s => (
                <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="device_ids"
            label="选择设备"
            rules={[{ required: true, message: '请至少选择一个设备' }]}
          >
            <Select
              mode="multiple"
              placeholder="选择要执行任务的设备"
              optionFilterProp="children"
            >
              {devices.map(d => (
                <Select.Option key={d.id} value={d.id}>
                  {d.name} ({d.ip}) - {d.status}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

// Monitor组件
const ScriptList = () => {
  const [scripts, setScripts] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  const fetchScripts = () => {
    fetchAPI('/api/scripts/').then(d => { if (d.code === 200) setScripts(d.data || []); });
  };
  useEffect(() => { fetchScripts(); }, []);

  const handleUpload = async (values) => {
    const res = await fetchAPI('/api/scripts/', { method: 'POST', body: JSON.stringify(values) });
    if (res.code === 200) {
      message.success('上传成功');
      setModalVisible(false);
      form.resetFields();
      fetchScripts();
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '类型', dataIndex: 'script_type', key: 'script_type' },
    { title: '内容', dataIndex: 'content', key: 'content', render: c => (c || '').substring(0, 30) + '...' },
    { title: '操作', key: 'action', render: (_, r) => <Button size="small" danger onClick={() => fetchAPI('/api/scripts/' + r.id, { method: 'DELETE' }).then(fetchScripts)}>删除</Button> }
  ];

  return (
    <div>
      <Button type="primary" onClick={() => setModalVisible(true)} style={{ marginBottom: 16 }}>上传脚本</Button>
      <Table dataSource={scripts} columns={columns} rowKey="id" pagination={{ pageSize: 10 }} />
      <Modal title="上传脚本" open={modalVisible} onCancel={() => setModalVisible(false)} footer={null}>
        <Form form={form} onFinish={handleUpload} layout="vertical">
          <Form.Item name="name" label="脚本名称" rules={[{ required: true }]}><Input placeholder="脚本名称" /></Form.Item>
          <Form.Item name="script_type" label="脚本类型" rules={[{ required: true }]}>
            <Select placeholder="选择类型">
              <Select.Option value="javascript">JavaScript</Select.Option>
              <Select.Option value="python">Python</Select.Option>
              <Select.Option value="shell">Shell</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="content" label="脚本内容" rules={[{ required: true }]}><Input.TextArea rows={8} placeholder="脚本内容" /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">上传</Button></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

// Monitor组件
const Monitor = () => {
  const [status, setStatus] = useState(null);
  useEffect(() => {
    const interval = setInterval(() => {
      fetchAPI('/api/monitor/status').then(d => { if (d.code === 200) setStatus(d.data); });
    }, 3000);
    fetchAPI('/api/monitor/status').then(d => { if (d.code === 200) setStatus(d.data); });
    return () => clearInterval(interval);
  }, []);
  return (
    <div>
      <h2>系统监控</h2>
      {status && (
        <div style={{ display: 'flex', gap: 16 }}>
          <Card title="设备状态" style={{ width: 200 }}>
            <p>总数: {status.devices?.total || 0}</p>
            <p>在线: {status.devices?.online || 0}</p>
          </Card>
          <Card title="任务状态" style={{ width: 200 }}>
            <p>活跃任务: {status.active_tasks || 0}</p>
          </Card>
        </div>
      )}
    </div>
  );
};

// LoginPage组件
const LoginPage = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const onFinish = async (values) => {
    setLoading(true);
    try {
      const response = await fetch(API_BASE + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values)
      }).then(r => r.json());
      if (response.code === 200) {
        localStorage.setItem('access_token', response.data.access_token);
        localStorage.setItem('user', JSON.stringify(response.data.user));
        message.success('登录成功');
        window.location.href = '/';
      } else {
        message.error(response.message || '登录失败');
      }
    } catch (error) { 
      message.error('登录失败，请检查后端服务'); 
    } finally { 
      setLoading(false); 
    }
  };
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card title="无线群控系统" style={{ width: 400 }}>
        <Form onFinish={onFinish} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} size="large" block>
              登录
            </Button>
          </Form.Item>
        </Form>
        <div style={{ textAlign: 'center', color: '#999', fontSize: '12px', marginTop: 16 }}>
          默认账号：admin / admin123
        </div>
      </Card>
    </div>
  );
};

// AppContent组件
const AppContent = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const location = useLocation();

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    const savedUser = localStorage.getItem('user');
    if (token && savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        setUser(userData);
        setLoading(false);
      } catch (e) {
        setLoading(false);
      }
    } else {
      setLoading(false);
    }
  }, []);

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>加载中...</div>;
  }

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  const menuItems = [
    { key: '1', icon: <DashboardOutlined />, label: '仪表盘', path: '/' },
    { key: '2', icon: <DesktopOutlined />, label: '设备管理', path: '/devices' },
    { key: '3', icon: <FolderOutlined />, label: '分组管理', path: '/groups' },
    { key: '4', icon: <AppstoreOutlined />, label: '任务管理', path: '/tasks' },
    { key: '5', icon: <ClockCircleOutlined />, label: '定时任务', path: '/scheduled-tasks' },
    { key: '6', icon: <BarChartOutlined />, label: '任务统计', path: '/task-statistics' },
    { key: '7', icon: <FileTextOutlined />, label: '脚本管理', path: '/scripts' },
    { key: '8', icon: <DashboardOutlined />, label: '系统监控', path: '/monitor' },
  ];

  const handleLogout = () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
    setUser(null);
    message.success('退出成功');
    window.location.href = '/login';
  };

  const userMenu = (
    <Menu>
      <Menu.Item key="logout" icon={<LogoutOutlined />} onClick={handleLogout}>
        退出登录
      </Menu.Item>
    </Menu>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={200}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: 16, background: 'rgba(0, 0, 0, 0.06)' }}>
          无线群控系统
        </div>
        <Menu
          mode="inline"
          selectedKeys={[menuItems.find(i => i.path === location.pathname)?.key || '1']}
          style={{ borderRight: 0 }}
        >
          {menuItems.map(item => (
            <Menu.Item key={item.key} icon={item.icon}>
              <Link to={item.path}>{item.label}</Link>
            </Menu.Item>
          ))}
        </Menu>
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f0f0f0' }}>
          <h2 style={{ margin: 0, fontSize: 16, fontWeight: 500 }}>
            {menuItems.find(i => i.path === location.pathname)?.label || '仪表盘'}
          </h2>
          <Dropdown overlay={userMenu} placement="bottomRight">
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar icon={<UserOutlined />} size="small" />
              <span>{user.username}</span>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', minHeight: 280 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/devices" element={<DeviceList />} />
            <Route path="/groups" element={<GroupList />} />
            <Route path="/tasks" element={<TaskList />} />
            <Route path="/scheduled-tasks" element={<ScheduledTaskList />} />
            <Route path="/task-statistics" element={<TaskStatistics />} />
            <Route path="/scripts" element={<ScriptList />} />
            <Route path="/monitor" element={<Monitor />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
};

const App = () => <Router><AppContent /></Router>;

export default App;
