# 设备端模块测试清单

## 测试准备

### 环境检查
- [ ] 准备Android 10-14设备
- [ ] 确保已安装Magisk v25.2
- [ ] 确保已安装LSPosed v1.9.3
- [ ] 设备已连接WiFi
- [ ] ADB连接正常：`adb devices`

### 构建和安装
```bash
# 1. 生成签名密钥
cd device-module
./generate-keystore.sh

# 2. 配置签名
cp local.properties.example local.properties
# 编辑local.properties，填入密钥信息

# 3. 构建APK
./build.sh

# 4. 安装到设备
adb push deploy/app-release-signed.apk /sdcard/
# 在Magisk中安装：模块 → 从本地安装 → 选择APK
# 重启设备
```

### 首次配置
- [ ] 在LSPosed中启用模块
- [ ] 勾选需要Hook的应用（微信、QQ、系统UI）
- [ ] 授予无障碍服务权限
- [ ] 授予通知访问权限
- [ ] 授予存储权限
- [ ] 检查应用是否启动

## 功能测试

### 1. 基础功能
```bash
# 获取设备IP
adb shell ip addr show wlan0 | grep "inet "

# 测试HTTP服务器
curl http://设备IP:8080/api/status
```

**预期结果：**
```json
{
  "status": "online",
  "timestamp": 1679928000000
}
```

### 2. 设备信息
```bash
curl http://设备IP:8080/api/device/info
```

**预期结果：** 包含device_id、manufacturer、model等字段

### 3. 应用列表
```bash
curl http://设备IP:8080/api/apps
```

**预期结果：** 返回应用列表JSON数组

### 4. 心跳检测
```bash
curl http://设备IP:8080/api/heartbeat
```

**预期结果：** 返回包含timestamp的心跳信息

### 5. 执行命令
```bash
# 点击操作
curl -X POST http://设备IP:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"action":"click","x":500,"y":500}'
```

**预期结果：** {"success":true,"message":"..."}

### 6. 启动应用
```bash
# 启动设置
curl -X POST http://设备IP:8080/api/apps/start \
  -H "Content-Type: application/json" \
  -d '{"package":"com.android.settings"}'

# 启动微信
curl -X POST http://设备IP:8080/api/apps/start \
  -H "Content-Type: application/json" \
  -d '{"package":"com.tencent.mm"}'
```

**预期结果：** 应用成功启动

### 7. 数据获取
```bash
# 获取联系人
curl http://设备IP:8080/api/contacts

# 获取短信列表
curl http://设备IP:8080/api/sms/list

# 获取通话记录
curl http://设备IP:8080/api/calls/list

# 获取通知列表
curl http://设备IP:8080/api/notifications
```

**预期结果：** 返回对应的JSON数据

### 8. Xposed Hook测试

**微信消息Hook：**
1. 打开微信
2. 发送一条消息
3. 检查日志：`adb logcat | grep -i "WeChat message"`
4. 检查消息是否上报到服务器

**QQ消息Hook：**
1. 打开QQ
2. 发送一条消息
3. 检查日志：`adb logcat | grep -i "QQ message"`
4. 检查消息是否上报到服务器

### 9. UI自动化测试

**点击操作：**
```bash
curl -X POST http://设备IP:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"action":"click","x":500,"y":500}'
```
**预期：** 屏幕上对应位置被点击

**滑动操作：**
```bash
curl -X POST http://设备IP:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"action":"swipe","start_x":500,"start_y":1000,"end_x":500,"end_y":500,"duration":300}'
```
**预期：** 屏幕上执行滑动操作

**输入文本：**
```bash
curl -X POST http://设备IP:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"action":"input","text":"Hello World"}'
```
**预期：** 文本输入到当前焦点输入框

### 10. 通知监听测试
1. 发送一条通知到设备
2. 检查日志：`adb logcat | grep -i "Notification"`
3. 检查通知是否上报到服务器
4. 测试通知列表API

### 11. 自动化测试脚本
```bash
cd device-module

# 使用默认IP测试
./test-functions.sh

# 使用指定IP测试
DEVICE_IP=192.168.1.100 ./test-functions.sh
```

**预期结果：** 10个测试全部通过

## 日志检查

### 查看应用日志
```bash
# 所有日志
adb logcat | grep -i wireless_control

# Debug级别日志
adb logcat -s WirelessControl:D

# 实时日志
adb logcat -v time | grep -i wireless_control
```

### 查看服务状态
```bash
# 检查服务是否运行
adb shell dumpsys activity services | grep wireless_control

# 检查进程
adb shell ps | grep wireless_control
```

### 查看Xposed日志
```bash
adb logcat | grep -i xposed
adb logcat | grep -i "Xposed"
```

### 查看系统日志
```bash
# 查看崩溃日志
adb logcat -b crash

# 查看事件日志
adb logcat -b events

# 查看系统信息
adb logcat -b system
```

## 性能测试

### CPU占用
```bash
adb shell top | grep wireless_control
```

### 内存占用
```bash
adb shell dumpsys meminfo com.wireless.control.device
```

### 电池消耗
```bash
adb shell dumpsys batterystats | grep wireless_control
```

### 网络流量
```bash
adb shell cat /proc/net/dev | grep wlan0
```

## 常见问题

### 模块未生效
**检查步骤：**
1. `adb shell pm list packages | grep wireless_control`
2. 打开LSPosed，检查模块是否启用
3. 检查是否勾选了目标应用
4. 重启设备
5. 查看Xposed日志

### HTTP服务无法访问
**检查步骤：**
1. `adb shell netstat -tlnp | grep 8080`
2. 检查设备IP：`adb shell ip addr show wlan0`
3. 检查防火墙
4. 尝试重启应用
5. 查看应用日志

### 无障碍服务无法使用
**检查步骤：**
1. 检查设置中是否开启无障碍服务
2. 检查是否有权限
3. 重启应用
4. 查看无障碍服务日志

### 通知监听无法使用
**检查步骤：**
1. 检查设置中是否开启通知访问权限
2. 检查应用是否有通知权限
3. 重启应用
4. 查看通知监听日志

## 测试记录

### 设备信息
- **设备型号：** ___________
- **Android版本：** ___________
- **Magisk版本：** ___________
- **LSPosed版本：** ___________
- **测试日期：** ___________

### 功能测试结果
- [ ] HTTP服务器：□ 通过 □ 失败
- [ ] 心跳检测：□ 通过 □ 失败
- [ ] 应用列表：□ 通过 □ 失败
- [ ] 联系人获取：□ 通过 □ 失败
- [ ] 短信获取：□ 通过 □ 失败
- [ ] 通话记录：□ 通过 □ 失败
- [ ] 通知列表：□ 通过 □ 失败
- [ ] 微信Hook：□ 通过 □ 失败
- [ ] QQ Hook：□ 通过 □ 失败
- [ ] 点击操作：□ 通过 □ 失败
- [ ] 滑动操作：□ 通过 □ 失败
- [ ] 文本输入：□ 通过 □ 失败
- [ ] 截图功能：□ 通过 □ 失败
- [ ] 通知监听：□ 通过 □ 失败

### 自动化测试结果
- [ ] 总测试数：___ 个
- [ ] 通过：___ 个
- [ ] 失败：___ 个
- [ ] 通过率：___%

### 性能测试结果
- [ ] CPU占用：___%
- [ ] 内存占用：___MB
- [ ] 电池消耗：___mA
- [ ] 响应时间：___ms

### 发现的问题
1. _______________________________
2. _______________________________
3. _______________________________

### 备注
_________________________________

_________________________________

---

**测试版本：** v1.0.0
**测试人员：** ___________
**完成日期：** ___________
