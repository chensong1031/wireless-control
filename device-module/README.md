# 无线群控系统 - 设备端Xposed模块

## 项目简介
这是一个Android 10-14设备的Xposed Hook模块，用于接收主控服务器的指令并执行设备操作。

## 技术栈
- **开发语言：** Kotlin
- **最低SDK：** Android 10 (API 29)
- **目标SDK：** Android 14 (API 34)
- **Xposed框架：** LSPosed v1.9.3
- **Magisk版本：** v25.2
- **HTTP服务器：** NanoHTTPD (端口8080)

## 功能特性

### 1. Xposed Hook功能
- ✅ Hook微信消息发送
- ✅ Hook微信联系人获取
- ✅ Hook QQ消息发送
- ✅ Hook系统UI通知栏
- ✅ 自动上报消息/联系人/通知

### 2. HTTP API接口 (端口8080)
- ✅ GET `/api/status` - 获取设备状态
- ✅ GET `/api/device/info` - 获取设备详细信息
- ✅ POST `/api/execute` - 执行设备命令
- ✅ GET `/api/apps` - 获取应用列表
- ✅ POST `/api/apps/start` - 启动应用
- ✅ POST `/api/apps/stop` - 停止应用
- ✅ POST `/api/sms/send` - 发送短信
- ✅ POST `/api/call/make` - 拨打电话
- ✅ GET `/api/contacts` - 获取联系人
- ✅ GET `/api/sms/list` - 获取短信列表
- ✅ GET `/api/calls/list` - 获取通话记录
- ✅ GET `/api/notifications` - 获取通知列表
- ✅ GET `/api/heartbeat` - 心跳检测

### 3. 后台服务
- ✅ DeviceControlService - 设备控制服务
- ✅ DeviceMonitorService - 设备监控服务
- ✅ HeartbeatService - 心跳服务
- ✅ DeviceAccessibilityService - 无障碍服务（点击/滑动/输入）
- ✅ NotificationListenerService - 通知监听服务

### 4. UI自动化功能
- ✅ 点击操作
- ✅ 长按操作
- ✅ 滑动操作
- ✅ 双指缩放
- ✅ 文本输入
- ✅ 按键模拟
- ✅ 节点查找和操作

### 5. 系统功能
- ✅ 截图功能（screencap命令）
- ✅ 通知监听和上报
- ✅ 设备状态监控
- ✅ 应用管理

## 安装方法

### 前置条件
1. Android 10-14 设备
2. Bootloader已解锁
3. 已安装 Magisk v25.2
4. 已安装 LSPosed v1.9.3

### 构建APK

#### 方法1：使用构建脚本（推荐）
```bash
cd device-module
chmod +x build.sh
./build.sh
```

构建完成后，APK文件位于 `deploy/` 目录：
- `app-debug.apk` - Debug版本（带调试信息）
- `app-release-signed.apk` - Release版本（已签名）
- `app-release-unsigned.apk` - Release版本（未签名）

#### 方法2：手动构建
```bash
cd device-module

# 清理之前的构建
./gradlew clean

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

构建结果位于 `app/build/outputs/apk/` 目录。

### 生成签名密钥

如果需要生成签名密钥，可以使用以下脚本：

```bash
cd device-module
chmod +x generate-keystore.sh
./generate-keystore.sh
```

按照提示输入密钥信息，生成完成后，需要配置 `local.properties` 文件：

```properties
RELEASE_STORE_FILE=keystore/wireless-control.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=wireless_control
RELEASE_KEY_PASSWORD=your_key_password
```

**重要：** 请妥善保管签名密钥，不要提交到版本控制系统！

### 部署APK到设备

#### 方法1：通过Magisk安装
```bash
# 1. 复制APK到设备
adb push deploy/app-release-signed.apk /sdcard/

# 2. 在Magisk中安装
# 打开Magisk应用 → 模块 → 从本地安装 → 选择APK文件

# 3. 重启设备
adb reboot
```

#### 方法2：通过ADB安装
```bash
# 安装Debug版本
adb install -r deploy/app-debug.apk

# 安装Release版本
adb install -r deploy/app-release-signed.apk
```

### 首次配置

#### 1. 在LSPosed中启用模块
1. 打开LSPosed应用
2. 找到"Wireless Control"模块
3. 勾选需要Hook的应用（微信、QQ、系统UI）
4. 重启设备使模块生效

#### 2. 授予必要权限
1. 打开"Wireless Control"应用
2. 按照提示授予以下权限：
   - 无障碍服务权限
   - 通知访问权限
   - 存储权限
   - 电话权限（如需拨打电话）
   - 短信权限（如需发送短信）
   - 相机权限（如需截图）

#### 3. 配置服务器地址
默认配置：
- 主控服务器：192.168.1.1:5000
- 设备服务器：192.168.1.100:8080

如需修改，编辑以下文件中的配置：
- `DeviceControlServer.kt` - HTTP服务器配置
- `HeartbeatService.kt` - 心跳服务配置

## 测试方法

### 自动化测试
使用提供的测试脚本：
```bash
cd device-module
chmod +x test-functions.sh

# 测试本地设备（使用默认IP）
./test-functions.sh

# 测试远程设备（指定IP）
DEVICE_IP=192.168.1.100 ./test-functions.sh
```

### 手动测试

#### 1. 检查设备状态
```bash
curl http://192.168.1.100:8080/api/status
```

预期输出：
```json
{
  "status": "online",
  "timestamp": 1679928000000
}
```

#### 2. 获取设备信息
```bash
curl http://192.168.1.100:8080/api/device/info
```

预期输出：
```json
{
  "device_id": "1234567890abcdef",
  "manufacturer": "Xiaomi",
  "model": "Redmi K40",
  "android_version": "13",
  "api_level": 33,
  "ip": "192.168.1.100",
  "port": 8080,
  "status": "online"
}
```

#### 3. 获取应用列表
```bash
curl http://192.168.1.100:8080/api/apps
```

#### 4. 执行点击命令
```bash
curl -X POST http://192.168.1.100:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"action":"click","x":500,"y":500}'
```

#### 5. 启动应用
```bash
curl -X POST http://192.168.1.100:8080/api/apps/start \
  -H "Content-Type: application/json" \
  -d '{"package":"com.tencent.mm"}'
```

### 调试方法

#### 查看应用日志
```bash
# 查看所有日志
adb logcat | grep -i wireless_control

# 查看特定模块日志
adb logcat -s WirelessControl:I WirelessControl:D

# 查看Xposed相关日志
adb logcat | grep -i xposed
```

#### 连接调试器
```bash
# 连接设备
adb connect 192.168.1.100:5555

# 查看设备状态
adb devices

# 进入shell
adb shell
```

#### 检查模块状态
```bash
# 检查LSPosed模块列表
adb shell am start -n io.github.lsposed.manager/.ui.MainActivity

# 检查服务是否运行
adb shell dumpsys activity services | grep wireless_control
```

## 配置说明

### 主控服务器配置
默认配置：
- **服务器IP：** 192.168.1.1
- **服务器端口：** 5000
- **设备端口：** 8080

如需修改，编辑以下文件中的配置：
- `DeviceControlServer.kt` - HTTP服务器配置
- `HeartbeatService.kt` - 心跳服务配置
- `DeviceMonitorService.kt` - 监控服务配置
- `DeviceControlService.kt` - 任务检查配置

### 网络配置
确保设备可以访问主控服务器：
1. 连接到同一WiFi网络
2. 禁用防火墙
3. 允许后台运行
4. 检查IP地址是否冲突

### 性能配置
优化性能可调整以下参数：
- 心跳间隔（HeartbeatService.kt）
- 任务检查间隔（DeviceControlService.kt）
- 监控频率（DeviceMonitorService.kt）

## 故障排查

### 模块未加载

**症状：** LSPosed中看不到模块或模块未生效

**解决方案：**
1. 检查Magisk是否正确安装：`adb shell magisk -v`
2. 检查LSPosed是否正确安装：`adb shell pm list packages | grep lsposed`
3. 重新安装模块
4. 重启设备
5. 查看Xposed日志：`adb logcat | grep -i xposed`

### HTTP服务无法访问

**症状：** 无法访问 http://设备IP:8080

**解决方案：**
1. 检查应用是否已安装：`adb shell pm list packages | grep wireless_control`
2. 检查应用是否已启动：`adb shell dumpsys activity | grep wireless_control`
3. 检查8080端口是否被占用：`adb shell netstat -tlnp | grep 8080`
4. 检查防火墙设置
5. 检查设备IP地址：`adb shell ip addr show wlan0`
6. 尝试重启应用：`adb shell am force-stop com.wireless.control.device && adb shell am start -n com.wireless.control.device/.MainActivity`

### Hook不生效

**症状：** 微信/QQ消息未上报

**解决方案：**
1. 检查LSPosed中是否勾选了目标应用
2. 检查应用包名是否正确
3. 查看Hook日志：`adb logcat | grep -E "(Hook|WeChat|QQ)"`
4. 重新启用模块
5. 重启设备

### 无障碍服务无法使用

**症状：** 点击/滑动操作无响应

**解决方案：**
1. 检查是否已开启无障碍服务：设置 → 无障碍 → Wireless Control
2. 检查是否有权限
3. 重启应用
4. 查看日志：`adb logcat | grep -i accessibility`

### 通知监听无法使用

**症状：** 通知未上报

**解决方案：**
1. 检查是否已开启通知访问权限：设置 → 应用和通知 → 通知访问
2. 检查应用是否有通知权限
3. 重启应用
4. 查看日志：`adb logcat | grep -i notification`

### 心跳失败

**症状：** 主控服务器收不到心跳

**解决方案：**
1. 检查网络连接：`ping 192.168.1.1`
2. 检查主控服务器是否运行
3. 检查服务器地址配置
4. 查看心跳日志：`adb logcat | grep -i heartbeat`

### 应用闪退

**症状：** 打开应用即闪退

**解决方案：**
1. 查看崩溃日志：`adb logcat -b crash`
2. 检查设备兼容性（仅支持Android 10-14）
3. 检查是否安装了LSPosed
4. 重新安装应用
5. 查看完整日志：`adb logcat | grep -i wireless_control`

## 开发说明

### 项目结构
```
app/
├── src/main/
│   ├── java/com/wireless/control/device/
│   │   ├── WirelessControlModule.kt      # Xposed主模块
│   │   ├── MainActivity.kt                # 主Activity
│   │   ├── server/
│   │   │   └── DeviceControlServer.kt    # HTTP服务器
│   │   ├── service/
│   │   │   ├── DeviceControlService.kt   # 设备控制服务
│   │   │   ├── DeviceMonitorService.kt   # 设备监控服务
│   │   │   ├── HeartbeatService.kt       # 心跳服务
│   │   │   ├── DeviceAccessibilityService.kt  # 无障碍服务
│   │   │   └── NotificationListenerService.kt  # 通知监听服务
│   │   └── utils/
│   │       ├── ScreenCapture.kt          # 截图工具
│   │       └── HttpClient.kt             # HTTP客户端
│   ├── res/
│   │   ├── xml/
│   │   │   ├── accessibility_service_config.xml
│   │   │   ├── data_extraction_rules.xml
│   │   │   └── backup_rules.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── themes.xml
│   │   │   └── colors.xml
│   │   └── layout/
│   │       └── activity_main.xml
│   └── AndroidManifest.xml
├── build.gradle
├── proguard-rules.pro
├── build.sh
├── test-functions.sh
└── generate-keystore.sh
```

### 代码统计
- **Kotlin文件：** 10个
- **总代码行数：** 约5000行
- **后台服务：** 5个
- **API接口：** 15个
- **Hook应用：** 3个

### 依赖项
```gradle
dependencies {
    // Android核心库
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // LSPosed API
    compileOnly 'de.robv.android.xposed:api:82'
    implementation 'de.robv.android.xposed:api:82:sources'
    kapt 'de.robv.android.xposed:api:82:sources'

    // HTTP服务器
    implementation 'org.nanohttpd:nanohttpd:2.3.1'

    // JSON处理
    implementation 'com.google.code.gson:gson:2.10.1'

    // 网络请求
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

### 扩展开发

#### 添加新的Hook
在 `WirelessControlModule.kt` 中添加新的Hook方法：
```kotlin
private fun hookTargetApp(lpparam: XC_LoadPackage.LoadPackageParam) {
    try {
        XposedHelpers.findAndHookMethod(
            "com.target.app.MainActivity",
            lpparam.classLoader,
            "targetMethod",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Hook逻辑
                }
            }
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to hook target app", e)
    }
}
```

#### 添加新的API接口
在 `DeviceControlServer.kt` 中添加新的路由：
```kotlin
when (uri) {
    "/api/new_endpoint" -> handleNewEndpoint(session)
    // ...
}
```

## 安全建议

1. **保护签名密钥**
   - 不要提交到版本控制系统
   - 使用强密码
   - 定期更换密钥

2. **网络安全**
   - 使用HTTPS（生产环境）
   - 验证服务器证书
   - 限制API访问

3. **权限管理**
   - 只授予必要的权限
   - 定期审查权限
   - 使用最小权限原则

4. **数据保护**
   - 不记录敏感信息
   - 加密传输数据
   - 安全存储密钥

## 许可证
MIT License

## 技术支持
- 问题反馈：请通过主控服务器反馈
- 日志收集：`adb logcat | grep -i wireless_control`
- 问题排查：参考"故障排查"章节

## 更新日志

### v1.0.0 (2026-03-27)
- ✅ 初始版本发布
- ✅ 实现基本Xposed Hook功能（微信、QQ、系统UI）
- ✅ 实现HTTP API服务器（15个接口）
- ✅ 实现5个后台服务
- ✅ 实现UI自动化功能（点击/滑动/输入）
- ✅ 实现截图功能
- ✅ 实现通知监听功能
- ✅ 完善构建和测试脚本
- ✅ 完善文档和配置

---

**开发版本：** v1.0.0
**最后更新：** 2026-03-27
**适用系统：** Android 10-14
**代码进度：** 95%（等待真机测试）
