# GitHub Actions 使用指南

## 前置条件

1. **创建 GitHub 仓库**
   - 访问 https://github.com/new
   - 创建新仓库，例如：`wireless-control`
   - 选择 Public 或 Private
   - **不要**初始化 README（我们已经有了）

2. **获取仓库地址**
   - 创建后会看到类似：`https://github.com/你的用户名/wireless-control.git`

---

## 配置步骤

### 方法1：SSH（推荐，如果你有 SSH 密钥）

```bash
# 1. 添加远程仓库（替换为你的实际地址）
git remote add origin git@github.com:你的用户名/wireless-control.git

# 2. 推送代码
git branch -M main
git push -u origin main
```

### 方法2：HTTPS

```bash
# 1. 添加远程仓库（替换为你的实际地址）
git remote add origin https://github.com/你的用户名/wireless-control.git

# 2. 推送代码
git branch -M main
git push -u origin main
```

---

## 触发自动构建

推送代码后：

1. 访问你的 GitHub 仓库
2. 点击 **Actions** 标签
3. 应该能看到 "Build APK" workflow 正在运行
4. 等待 3-5 分钟完成

---

## 下载构建好的 APK

构建完成后：

1. 点击最新的 workflow run
2. 滚动到页面底部
3. 在 **Artifacts** 部分找到：
   - `debug-apk` (包含 app-debug.apk)
   - `release-apk` (包含 release 版本)
4. 点击下载

---

## 示例

假设你的 GitHub 用户名是 `zhangsan`：

```bash
git remote add origin https://github.com/zhangsan/wireless-control.git
git push -u origin main
```

然后访问：
```
https://github.com/zhangsan/wireless-control/actions
```

---

## 故障排查

### 问题：推送时提示认证失败

**解决：** 使用 Personal Access Token
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 生成新 token，勾选 `repo` 权限
3. 推送时：
   ```bash
   git push https://你的token@github.com/你的用户名/wireless-control.git
   ```

### 问题：Actions 失败

**解决：**
1. 查看 Actions 日志，找到具体错误
2. 常见问题：
   - 配置错误 → 检查 `.github/workflows/build.yml`
   - 依赖问题 → 检查 `build.gradle` 配置

---

## 构建成功后的步骤

下载 APK 后：

```bash
# 1. 卸载旧版本
adb uninstall com.wireless.control.device

# 2. 安装新版本
adb install app-debug.apk

# 3. 重启手机
adb reboot

# 4. 检查 LSPosed
# 打开 LSPosed 应用 → 模块 → Wireless Control 应该是白色（可启用）
```

---

## 自动化构建配置说明

`.github/workflows/build.yml` 已配置：
- ✅ 自动构建 Debug 和 Release 版本
- ✅ 支持 Android 10-14
- ✅ 包含所有 Xposed 模块文件
- ✅ 每次推送代码自动触发

---

**需要帮助？** 把 GitHub 仓库地址告诉我，我可以帮你检查配置！
