# Xposed API JAR 文件说明

## 问题

`de.robv.android.xposed:api:82` 不在标准的 Maven 仓库中，需要手动添加 JAR 文件。

## 解决方案

### 方法1：从 GitHub 获取（推荐）

```bash
cd device-module/app/libs

# 下载 Xposed API JAR
wget https://github.com/rovo89/XposedBridge/releases/download/82/XposedBridgeApi-82.jar -O xposed-api-82.jar
```

### 方法2：从本地 Xposed 框架提取

如果你的设备已经安装了 Xposed/LSPosed：

```bash
adb pull /system/framework/XposedBridge.jar device-module/app/libs/xposed-api-82.jar
```

### 方法3：手动编译 XposedBridge

```bash
git clone https://github.com/rovo89/XposedBridge.git
cd XposedBridge
./gradlew jar
cp build/libs/XposedBridge-*.jar ../device-module/app/libs/xposed-api-82.jar
```

## 临时解决方案

如果暂时无法获取 JAR 文件，可以先移除 Xposed 相关的代码和依赖，使用纯 Android 开发。

## 验证

添加 JAR 文件后，运行：

```bash
./gradlew assembleDebug
```

应该能成功编译。
