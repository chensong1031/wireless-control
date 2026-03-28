# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# 不压缩native库
-dontwarn native.**

# 保留所有Xposed相关类和成员
-keep class de.robv.android.xposed.** { *; }
-keep class * extends de.robv.android.xposed.IXposedHookLoadPackage
-keep class * extends de.robv.android.xposed.IXposedHookZygoteInit
-keep @de.robv.android.xposed.XposedModule class *

# 保留无障碍服务
-keep class com.wireless.control.device.service.DeviceAccessibilityService { *; }
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public <init>();
}

# 保留通知监听服务
-keep class com.wireless.control.device.service.NotificationListenerService { *; }
-keepclassmembers class * extends android.service.notification.NotificationListenerService {
    public <init>();
}

# 保留所有服务
-keep class com.wireless.control.device.service.** { *; }
-keepclassmembers class * extends android.app.Service {
    public <init>();
}

# 保留HTTP服务器
-keep class com.wireless.control.device.server.** { *; }

# 保留工具类
-keep class com.wireless.control.device.utils.** { *; }

# 保留所有public方法和字段
-keepclassmembers class * {
    public <methods>;
    public <fields>;
}

# 保留所有native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# NanoHTTPD相关
-dontwarn fi.iki.elonen.**
-keep class fi.iki.elonen.** { *; }

# OkHttp相关
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson相关
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留Gson使用的字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留R类
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留MainActivity
-keep class com.wireless.control.device.MainActivity { *; }

# 保留入口点
-keep class com.wireless.control.device.WirelessControlModule { *; }

# 优化：不优化Xposed相关的类
-keep class de.robv.android.xposed.** { *; }

# 删除日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 移除日志（仅在release版本）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
