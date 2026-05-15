# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WorkManager
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.background.systemalarm.RescheduleReceiver { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryChargingProxy { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryNotLowProxy { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$StorageNotLowProxy { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$NetworkStateProxy { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class androidx.work.impl.foreground.SystemForegroundService { *; }
-keep class androidx.work.impl.diagnostics.DiagnosticsReceiver { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase_Impl { *; }

# SQLiteAssetHelper
-keep class com.readystatesoftware.sqliteasset.** { *; }

# ML Kit Digital Ink Recognition
-keep class com.google.mlkit.vision.digitalink.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_digital_ink_bom.** { *; }

# Keep all project classes to avoid reflection issues (MathParser) and database access issues
-keep class com.wade.** { *; }
-keep interface com.wade.** { *; }
-keepclassmembers class com.wade.** { *; }

# Keep androidx.preference classes
-keep class androidx.preference.** { *; }
