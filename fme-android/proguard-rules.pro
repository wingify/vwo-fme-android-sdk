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
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-repackageclasses 'fme'
-allowaccessmodification
-keeppackagenames doNotKeepAThing

# Legacy public API (com.vwo)
-keep class com.vwo.VWO { *; }
-keep class com.vwo.VWO, com.vwo.VWO$Companion { public protected *; }
-keep class com.vwo.VWOClient { *; }
-keep class com.wingify.WingifyBuilder { *; }
-keep class com.vwo.interfaces.IVwoInitCallback { *; }
-keep class com.vwo.interfaces.IVwoListener { *; }
-keep class com.vwo.interfaces.logger.LogTransport { *; }
-keep class com.vwo.packages.logger.enums.LogLevelEnum { *; }
-keep class com.vwo.interfaces.integration.IntegrationCallback { *; }
-keep class com.vwo.models.** { *; }
-keep class com.vwo.packages.storage.Connector { *; }

# Wingify public API (com.wingify)
-keep class com.wingify.Wingify { *; }
-keep class com.wingify.Wingify, com.wingify.Wingify$Companion { public protected *; }
-keep class com.wingify.WingifyClient { *; }
-keep class com.wingify.WingifyBuilder { *; }
-keep class com.wingify.interfaces.IWingifyInitCallback { *; }
-keep class com.wingify.interfaces.IWingifyListener { *; }
-keep class com.wingify.interfaces.logger.LogTransport { *; }
-keep class com.wingify.interfaces.integration.IntegrationCallback { *; }
-keep class com.wingify.packages.logger.enums.LogLevelEnum { *; }
-keep class com.wingify.packages.storage.Connector { *; }
-keep class com.wingify.models.** { *; }