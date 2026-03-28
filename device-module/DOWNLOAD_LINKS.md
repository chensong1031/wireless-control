# Magisk 和 LSPosed 快速下载链接

**最后更新：** 2026-03-27

---

## 📦 Magisk 下载

### 官方渠道
- **官网：** https://magisk.dev/download/
- **GitHub：** https://github.com/topjohnwu/Magisk/releases
- **最新版本：** v30.7（2026-03-16）

### 直接下载链接

**APK文件（已ROOT设备）：**
```bash
# 官方链接
https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk

# 国内镜像（加速）
https://mirror.ghproxy.com/https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk
```

**Recovery刷入包（首次ROOT）：**
```bash
# 官方链接
https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.zip

# 国内镜像（加速）
https://mirror.ghproxy.com/https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.zip
```

### 文件信息
```
文件名：Magisk-v30.7.apk / Magisk-v30.7.zip
大小：约12MB
版本：v30.7 (30700)
更新：2026-03-16
```

---

## 🧩 LSPosed 下载

### 官方渠道
- **GitHub：** https://github.com/LSPosed/LSPosed/releases
- **Magisk模块站：** https://www.magiskmodule.com/lsposed-zygisk/
- **最新版本：** v1.11.0（2026-03-15）

### 直接下载链接

**Zygisk版本（推荐）：**
```bash
# 官方链接
https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

# 国内镜像（加速）
https://mirror.ghproxy.com/https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

# FastGit镜像
https://hub.fastgit.xyz/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip
```

### 文件信息
```
文件名：LSPosed-v1.11.0.zip
大小：约2.6MB
版本：v1.11.0
类型：Magisk模块
更新：2026-03-15
依赖：Magisk v23+ + Zygisk
```

---

## 🚀 快速安装命令

### 1. 下载文件

```bash
# 创建下载目录
mkdir -p ~/Downloads/android-tools
cd ~/Downloads/android-tools

# 下载Magisk
wget https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk

# 下载LSPosed
wget https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip
```

### 2. 安装Magisk

```bash
# 安装Magisk应用
adb install Magisk-v30.7.apk

# 或者如果需要刷入Recovery包
adb push Magisk-v30.7.zip /sdcard/
# 重启到Recovery并刷入
```

### 3. 安装LSPosed

```bash
# 推送到设备
adb push LSPosed-v1.11.0.zip /sdcard/Download/

# 通过Magisk应用安装
# 打开Magisk → 模块 → 从本地安装 → 选择LSPosed-v1.11.0.zip
```

---

## 📋 版本兼容性表

| 组件 | 版本 | 最低Android | 推荐Android | 大小 |
|------|------|------------|------------|------|
| Magisk | v30.7 | Android 5.0 | Android 10+ | 12MB |
| LSPosed | v1.11.0 | Android 10 | Android 10+ | 2.6MB |
| 无线群控模块 | v1.0.0 | Android 10 | Android 10-14 | 约5MB |

---

## ⚠️ 注意事项

1. **设备要求**
   - Bootloader已解锁
   - Android 10或更高版本
   - 至少2GB可用存储空间

2. **安装顺序**
   - 先安装Magisk
   - 启用Zygisk
   - 重启设备
   - 安装LSPosed
   - 重启设备

3. **备份重要数据**
   - 刷入前请备份所有重要数据
   - 建议使用TWRP进行完整备份

4. **兼容性**
   - LSPosed仅支持ARM/ARM64架构
   - 部分设备可能不兼容
   - 建议先在非主力设备上测试

---

## 🔗 更多资源

### 详细安装指南
- [Magisk和LSPosed完整安装指南](./MAGISK_LSPOSED_INSTALL.md)
- [快速开始指南](./QUICK_START.md)
- [测试清单](./TEST_CHECKLIST.md)

### 官方文档
- **Magisk Wiki：** https://topjohnwu.github.io/Magisk/
- **LSPosed文档：** https://lsposed.github.io/

### 社区支持
- **XDA Magisk：** https://forum.xda-developers.com/android/apps-games/app-magisk-root-v7-t3473233
- **LSPosed GitHub：** https://github.com/LSPosed/LSPosed/issues

---

## 📥 下载汇总

### 一键下载脚本

```bash
#!/bin/bash
# download-tools.sh - 一键下载安装包

echo "正在下载Magisk和LSPosed..."

# 下载目录
DOWNLOAD_DIR="$HOME/Downloads/android-tools"
mkdir -p "$DOWNLOAD_DIR"
cd "$DOWNLOAD_DIR"

# 下载Magisk
echo "[1/2] 下载Magisk v30.7..."
wget -O Magisk-v30.7.apk \
  https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk

# 下载LSPosed
echo "[2/2] 下载LSPosed v1.11.0..."
wget -O LSPosed-v1.11.0.zip \
  https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

echo ""
echo "下载完成！"
echo "文件位置：$DOWNLOAD_DIR"
ls -lh "$DOWNLOAD_DIR"
```

使用方法：
```bash
# 保存脚本
cat > download-tools.sh << 'EOF'
[上面的脚本内容]
EOF

# 添加执行权限
chmod +x download-tools.sh

# 运行脚本
./download-tools.sh
```

---

**更新时间：** 2026-03-27
**文档版本：** v1.0
**适用系统：** Android 10-14
