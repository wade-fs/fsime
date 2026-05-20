# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep WorkManager and its initializer
-keep class androidx.work.WorkManagerInitializer { *; }
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.startup.InitializationProvider { *; }

# Keep ML Kit components
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep common classes that might be accessed via reflection
-keepattributes Signature,EnclosingMethod,InnerClasses,AnnotationDefault

