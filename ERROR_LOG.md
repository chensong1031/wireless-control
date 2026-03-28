# 开发错误日志 - 无线群控项目

记录开发过程中遇到的所有错误、原因和解决方案，避免重复犯错。

**最后更新：** 2026-03-28  
**维护规则：** 后期所有错误都会记录到这个文件，持续更新

---

## 错误 001: Node.js 20 Deprecated 警告

**日期：** 2026-03-28  
**严重程度：** 警告（不影响构建）

### 错误信息
```
Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on Node.js 24: actions/checkout@v4, actions/setup-java@v4, android-actions/setup-android@v2.
```

### 原因
GitHub Actions 默认强制使用 Node.js 24，但使用的 actions 还只支持 Node.js 20。

### 解决方案
在 `.github/workflows/build.yml` 中添加环境变量：
```yaml
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

### 预防措施
- 使用最新版本的 actions（v4+）
- 定期检查 GitHub Actions 的兼容性

---

## 错误 002: jcenter() 仓库废弃

**日期：** 2026-03-28  
**严重程度：** 错误（阻塞构建）

### 错误信息
```
jcenter() repository is deprecated and may cause build failures.
```

### 原因
JCenter 仓库已停止服务，无法下载依赖。

### 解决方案
移除 `jcenter()`，使用 Maven Central：
```gradle
repositories {
    google()
    mavenCentral()
}
```

### 预防措施
- 新项目不要使用 jcenter()
- 使用 `mavenCentral()` 替代

---

## 错误 003: FAIL_ON_PROJECT_REPOS 配置错误

**日期：** 2026-03-28  
**严重程度：** 错误（阻塞构建）

### 错误信息
```
Build was configured to prefer settings repositories over project repositories 
but repository 'Google' was added by build file 'build.gradle'
```

### 原因
`settings.gradle` 设置了 `FAIL_ON_PROJECT_REPOS`，但 `build.gradle` 中又有 `allprojects.repositories`，两者冲突。

### 解决方案
移除 `build.gradle` 中的 `allprojects.repositories`，只在 `settings.gradle` 中配置：
```gradle
// build.gradle 中不要有 allprojects.repositories

// settings.gradle 中配置
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### 预防措施
- 使用 Gradle 8+ 时，统一在 `settings.gradle` 管理仓库
- 不要在 `build.gradle` 中使用 `allprojects.repositories`

---

## 错误 004: Xposed API 依赖找不到

**日期：** 2026-03-28  
**严重程度：** 错误（阻塞构建）

### 错误信息
```
Could not find de.robv.android.xposed:api:82.
Searched in the following locations:
 - https://dl.google.com/dl/android/maven2/
 - https://repo1.maven.org/maven2/
```

### 原因
Xposed API 不在标准的 Maven 仓库中。

### 解决方案
添加多个仓库：
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        maven { url 'https://repo1.maven.org/maven2/' }
        maven { url 'https://jitpack.io' }
        maven { url 'https://maven.aliyun.com/repository/public/' }
    }
}
```

### 预防措施
- 非标准库需要查找正确的 Maven 仓库
- 使用 jitpack.io 作为备选方案

---

## 错误 005: InetAddress 引用错误

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
Unresolved reference: InetAddress
```

### 原因
使用了 `InetAddress` 但没有 import。

### 解决方案
添加 import：
```kotlin
import java.net.InetAddress
```

### 预防措施
- 使用 IDE 自动导入功能
- 编译前检查所有的 import 语句

---

## 错误 006: Toast.makeText 参数类型错误

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
None of the following functions can be called with the arguments supplied:
public open fun makeText(p0: Context!, p1: CharSequence!, p2: Int): Toast!
public open fun makeText(p0: Context!, p1: Int, p2: Int): Toast!
```

### 原因
传递的参数类型不匹配，可能是传递了 `String?` 而不是 `CharSequence`。

### 解决方案
确保参数类型正确：
```kotlin
// 错误
Toast.makeText(this, null, Toast.LENGTH_SHORT).show()

// 正确
val message = "Hello"
Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
```

### 预防措施
- Kotlin 中注意空安全
- 使用非空类型或进行空检查

---

## 错误 007: 重复的 companion object 声明

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
Conflicting declarations: public companion object, public companion object
Only one companion object is allowed per class
```

### 原因
在同一个类中声明了两个 `companion object`。

### 解决方案
合并为一个：
```kotlin
// 错误
class MyClass {
    companion object {
        const val A = 1
    }
    companion object {
        const val B = 2
    }
}

// 正确
class MyClass {
    companion object {
        const val A = 1
        const val B = 2
    }
}
```

### 预防措施
- 每个类只能有一个 companion object
- 所有静态变量和方法都放在同一个 companion object 中

---

## 错误 008: onAccessibilityEvent 返回类型错误

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
Return type of 'onAccessibilityEvent' is not a subtype of the return type of the overridden member 
'public abstract fun onAccessibilityEvent(p0: AccessibilityEvent!): Unit defined in android.accessibilityservice.AccessibilityService'
```

### 原因
`onAccessibilityEvent` 返回了 `Boolean`，但父类要求返回 `Unit`。

### 解决方案
返回 `Unit`（不返回任何值）：
```kotlin
// 错误
override fun onAccessibilityEvent(event: AccessibilityEvent): Boolean {
    return true
}

// 正确
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    // 处理事件
}
```

### 预防措施
- 重写方法时检查父类的返回类型
- 使用 IDE 的 Override 功能确保签名正确

---

## 错误 009: R.drawable 资源引用错误

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
Unresolved reference: R
```

### 原因
项目中没有 `ic_launcher_foreground` 资源。

### 解决方案
使用系统资源：
```kotlin
// 错误
.setSmallIcon(R.drawable.ic_launcher_foreground)

// 正确
.setSmallIcon(android.R.drawable.ic_menu_info)
```

### 预防措施
- 先确认资源文件是否存在
- 或创建相应的资源文件

---

## 错误 010: Gradle 版本不兼容

**日期：** 2026-03-28  
**严重程度：** 错误（阻塞构建）

### 错误信息
```
You are using Gradle 4.4.1. If using the gradle wrapper, try editing the distributionUrl 
in /root/.gradle/wrapper/dists/gradle-8.2-bin.zip to gradle-5.6.4-all.zip
```

### 原因
系统 Gradle 版本太旧，项目需要 Gradle 8.2+。

### 解决方案
使用 Gradle wrapper，让项目自己管理 Gradle 版本：
```gradle
// gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
```

### 预防措施
- 始终使用 Gradle wrapper
- 不要依赖系统的 Gradle

---

## 错误 011: 为了编译成功移除核心功能

**日期：** 2026-03-28  
**严重程度：** 严重（设计错误）

### 错误描述
为了解决编译问题，我错误地移除了 Xposed API 依赖和 Hook 功能。

### 原因
没有明确项目核心目标，为了短期目标牺牲了长期价值。

### 解决方案
恢复所有 Xposed 功能：
- 保留 `IXposedHookLoadPackage` 实现
- 保留 Xposed API 依赖
- 保留 `assets/xposed_init` 文件

### 预防措施
- 明确项目的核心功能，绝不为了解决编译问题而移除
- 遇到编译错误时，寻找正确的解决方案，而不是移除功能
- 每次修改前问自己：这是否会影响核心功能？

---

## 错误 012: 未在修改后检查就推送

**日期：** 2026-03-28  
**严重程度：** 严重（流程错误）

### 错误描述
修改代码后没有自己检查一遍就直接提交推送，导致多次重复修改相同的错误。

### 原因
忽视了自己强调的一级指令：每次修改后自己检查一遍再推送。

### 解决方案
建立检查清单，每次推送前确认：
- [ ] 代码语法正确
- [ ] 没有 import 错误
- [ ] 没有重复声明
- [ ] 资源引用正确
- [ ] 核心功能保留
- [ ] git status 确认

### 预防措施
- **一级指令：每次修改后自己检查一遍，牢牢刻在记忆里**
- 不要急于推送，先验证
- 在本地测试后再提交

---

## 错误 013: NotificationCompat.Builder 资源引用错误

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
Unresolved reference: ic_menu_info
```

### 原因
`NotificationCompat.Builder` 在某些情况下无法正确解析 `android.R.drawable.ic_menu_info`，需要将资源 ID 提取为变量。

### 解决方案
使用原生的 `Notification.Builder` 并将资源 ID 提取为变量：
```kotlin
// 错误
.setSmallIcon(android.R.drawable.ic_menu_info)

// 正确
val notificationId = android.R.drawable.ic_menu_info
return Notification.Builder(this, CHANNEL_ID)
    .setSmallIcon(notificationId)
    .build()
```

### 预防措施
- 使用资源 ID 时，添加 `.toInt()` 进行显式类型转换
- 不要依赖 Kotlin 的类型推断，尤其是对于 Android 资源
- 使用原生 API 而不是兼容库
- 确保使用正确的重载方法

---

## 错误 015: GitHub Actions 使用缓存的代码

**日期：** 2026-03-28  
**严重程度：** 流程错误

### 错误描述
本地修改了代码并推送，但 GitHub Actions 仍然使用旧版本的代码进行构建，导致相同的错误重复出现。

### 原因
1. 本地修改后推送成功，但 GitHub Actions 可能使用了缓存的构建环境
2. 或者修改没有正确提交/推送
3. 或者 GitHub Actions 的构建缓存问题

### 解决方案
1. 确认本地修改已提交（`git commit`）
2. 确认修改已推送（`git push`）
3. 触发新的构建，不使用缓存
4. 检查 GitHub Actions 的缓存设置

### 预防措施
- 每次推送后，确认 GitHub 上的最新提交
- 使用 `git log origin/main` 检查远程分支
- 确认 GitHub Actions 使用最新的代码

---

## 错误 014: setSmallIcon 类型歧义和未使用变量

**日期：** 2026-03-28  
**严重程度：** 编译错误

### 错误信息
```
Unresolved reference: ic_menu_info
Overload resolution ambiguity:
public open fun setSmallIcon(p0: Icon!): Notification.Builder defined in android.app.Notification.Builder
public open fun setSmallIcon(p0: Int): Notification.Builder defined in android.app.Notification.Builder
```

### 原因
1. 直接在 `setSmallIcon()` 中使用资源 ID，Kotlin 无法推断类型
2. 定义了未使用的变量 `val notificationId = android.R.drawable.ic_menu_info`
3. `setSmallIcon()` 需要 Int 类型，但 Kotlin 推断为 Icon

### 解决方案
直接在 `setSmallIcon()` 中使用资源 ID，不要提取为变量：
```kotlin
// 错误 1
.setSmallIcon(android.R.drawable.ic_menu_info)  // 无法推断类型

// 错误 2
val notificationId = android.R.drawable.ic_menu_info
.setSmallIcon(notificationId)  // 未使用变量

// 正确
.setSmallIcon(android.R.drawable.ic_menu_info)  // 但要避免类型歧义
```

### 预防措施
- 直接使用 `android.R.drawable.xxx` 资源
- 不要定义未使用的变量
- 使用原生 API 而不是兼容库
- 确保使用正确的重载方法

---

## 总结和原则

### 开发原则

1. **功能优先** - 核心功能不能为了解决编译问题而移除
2. **先理解后修改** - 修改前先理解代码的作用
3. **全面检查** - 每次修改后自己检查一遍，确认正确性
4. **记录错误** - 记录所有错误和解决方案，避免重复
5. **持续更新** - 后期所有错误都记录到这个文件

### 构建检查清单

每次推送前检查：
- [ ] Gradle 配置正确
- [ ] 依赖都已添加
- [ ] 没有 import 错误
- [ ] 没有语法错误
- [ ] 没有重复声明
- [ ] 资源引用正确
- [ ] 核心功能保留
- [ ] git status 确认
- [ ] 自己检查一遍修改（一级指令）

### 常见错误类型

1. **依赖错误** - 库找不到或版本不匹配
2. **配置错误** - Gradle/仓库配置问题
3. **语法错误** - Kotlin 语法不正确
4. **引用错误** - 资源/类型引用错误
5. **逻辑错误** - 为了编译牺牲功能
6. **流程错误** - 未检查就推送

### 更新规则

每次遇到新错误时：
1. 记录错误信息
2. 分析错误原因
3. 提供解决方案
4. 总结预防措施
5. 更新本文件

---

**最后更新：** 2026-03-28  
**维护人：** AI Assistant  
**总错误数：** 14

---

## 错误 016: Android 资源链接错误（缺少图标资源和不支持的属性）

**日期：** 2026-03-28  
**严重程度：** 错误（阻塞构建）

### 错误信息
```
AAPT: error: resource mipmap/ic_launcher (aka com.wireless.control.device:mipmap/ic_launcher) not found.
AAPT: error: resource mipmap/ic_launcher_round (aka com.wireless.control.device:mipmap/ic_launcher_round) not found.
AAPT: error: attribute android:usesCleartextForLocalization not found.
```

### 原因
1. **AndroidManifest.xml** 引用了不存在的图标资源 `@mipmap/ic_launcher` 和 `@mipmap/ic_launcher_round`
2. 使用了 Android 不支持的属性 `android:usesCleartextForLocalization`
3. **application 标签语法错误** - 使用了自关闭标签 `/>` 但内部还有子元素

### 解决方案
1. **移除图标引用**：对于 Xposed 模块，不需要启动图标
2. **移除不支持的属性**：删除 `android:usesCleartextForLocalization`
3. **使用硬编码的应用名称**：将 `@string/app_name` 改为 `"无线群控"`
4. **修复 XML 语法**：将 `application />` 改为 `application>...</application>`

**修复后的 AndroidManifest.xml：**
```xml
<application
    android:allowBackup="true"
    android:label="无线群控"
    android:supportsRtl="true">

    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

</application>
```

**修改的文件：**
- `device-module/app/src/main/AndroidManifest.xml`
- `device-module/app/src/main/java/com/wireless/control/device/MainActivity.kt`（简化代码）
- 删除了 `device-module/app/src/main/res/layout/activity_main.xml`（不需要）

### 预防措施
1. **检查资源文件存在性**：引用任何资源前，确认文件存在
2. **使用 Android 官方文档验证属性**：不使用未经验证的自定义属性
3. **验证 XML 语法**：自关闭标签不能包含子元素
4. **Xposed 模块特殊要求**：
   - Xposed 模块不需要图标资源
   - 可以简化为纯 Xposed Hook 功能
   - Activity 不是必需的，但推荐保留用于调试

### 关键知识点
- **Xposed 模块的特殊性**：作为系统模块，不需要启动图标
- **资源管理**：Android 资源必须先在 `res/` 目录下创建才能引用
- **XML 语法**：自关闭标签 `/>` 不能包含子元素，使用 `>...</...>` 封闭标签
- **属性兼容性**：只使用 Android 官方文档中列出的属性

### 经验教训
- 简化不必要的资源引用可以快速解决构建问题
- Xposed 模块的核心功能是 Hook，UI 和资源可以最小化
- 每次修改 XML 后，先验证语法再提交

---

**维护规则：**
1. 每次遇到新的错误都记录到这里
2. 提供详细的原因分析
3. 给出具体的解决方案
4. 总结预防措施
5. 更新总错误数

---

**最后更新：** 2026-03-28 16:00  
**维护人：** AI Assistant  
**总错误数：** 16

---

## 错误 017: MainActivity 和 Service 编译错误

**日期：** 2026-03-28  
**严重程度：** 错误（阻塞构建）

### 错误信息
```
MainActivity.kt:9:22 Unresolved reference: AppCompatActivity
MainActivity.kt:20:5 'onCreate' overrides nothing
MainActivity.kt:21:15 Unresolved reference: onCreate
MainActivity.kt:31:5 'onDestroy' overrides nothing
MainActivity.kt:32:15 Unresolved reference: onDestroy
Service.kt:84:46 Unresolved reference: ic_menu_info (5个 Service 类)
```

### 原因分析
1. **MainActivity.kt 缺少 import**：
   - 继承了 `AppCompatActivity` 但没有导入 `androidx.appcompat.app.AppCompatActivity`
   - 导致无法解析基类，onCreate/onDestroy 方法也无法识别

2. **Service 类使用了不存在的资源**：
   - `android.R.drawable.ic_menu_info` 在 Android 10 (API 29) 中不存在
   - 该资源可能在更高版本的 Android 中才引入
   - 导致编译器无法解析资源引用

### 解决方案
1. **添加缺失的 import**：
```kotlin
import androidx.appcompat.app.AppCompatActivity
```

2. **使用 Android 10 兼容的资源**：
```kotlin
// 修改前（不存在）
.setSmallIcon(android.R.drawable.ic_menu_info.toInt())

// 修改后（Android 10 兼容）
.setSmallIcon(android.R.drawable.ic_dialog_info.toInt())
```

### 修改的文件
- `device-module/app/src/main/java/com/wireless/control/device/MainActivity.kt` - 添加 import
- `device-module/app/src/main/java/com/wireless/control/device/service/DeviceAccessibilityService.kt` - 替换资源
- `device-module/app/src/main/java/com/wireless/control/device/service/DeviceControlService.kt` - 替换资源
- `device-module/app/src/main/java/com/wireless/control/device/service/DeviceMonitorService.kt` - 替换资源
- `device-module/app/src/main/java/com/wireless/control/device/service/HeartbeatService.kt` - 替换资源
- `device-module/app/src/main/java/com/wireless/control/device/service/NotificationListenerService.kt` - 替换资源

### 关键知识点
1. **Android 资源兼容性**：
   - 不同 Android 版本提供的系统资源不同
   - 必须使用目标 API 级别支持的标准资源
   - Android 10 (API 29) 支持的系统资源可以查官方文档

2. **常用 Android 系统图标**（Android 10 兼容）：
   - `android.R.drawable.ic_dialog_info` - 信息对话框图标
   - `android.R.drawable.ic_dialog_alert` - 警告对话框图标
   - `android.R.drawable.ic_menu_camera` - 相机图标
   - `android.R.drawable.ic_menu_call` - 电话图标

3. **Kotlin import 规则**：
   - 继承类必须先导入才能使用
   - 基类方法（如 onCreate）需要基类才能正确解析
   - IDE 通常会自动提示缺失的 import

### 预防措施
1. **使用 IDE 自动导入**：让 IDE 自动添加缺失的 import
2. **验证资源存在性**：使用 Android Studio 查看系统资源列表
3. **参考官方文档**：Android Developers 文档列出了所有系统资源
4. **测试最低版本**：确保使用的资源在最低 API 级别可用

### 经验教训
- 简化代码时要保留必要的 import
- Android 系统资源不是所有版本都一样
- 使用 GitHub Actions 时要确保所有修复都已推送
- 本地构建成功不等于远程构建成功

---

**维护规则：**
1. 每次遇到新的错误都记录到这里
2. 提供详细的原因分析
3. 给出具体的解决方案
4. 总结预防措施
5. 更新总错误数

---

**最后更新：** 2026-03-28 16:20  
**维护人：** AI Assistant  
**总错误数：** 17
