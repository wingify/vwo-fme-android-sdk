# Consumer ProGuard rules for apps that depend on this SDK (release minification).
# Shipped inside the AAR — applied automatically to the consuming application.

# --- Legacy public API (com.vwo) ---
-keep class com.vwo.VWO { *; }
-keep class com.vwo.VWO, com.vwo.VWO$Companion { public protected *; }
-keep class com.vwo.VWOClient { *; }
-keep class com.wingify.WingifyBuilder { *; }
-keep class com.vwo.interfaces.IVwoInitCallback { *; }
-keep class com.vwo.interfaces.IVwoListener { *; }
-keep class com.vwo.interfaces.logger.LogTransport { *; }
-keep class com.vwo.interfaces.integration.IntegrationCallback { *; }
-keep class com.vwo.packages.logger.enums.LogLevelEnum { *; }
-keep class com.vwo.models.** { *; }
-keep class com.vwo.packages.storage.Connector { *; }

# --- Wingify public API (com.wingify) ---
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

# Wingify public types are standalone classes; conversion to com.vwo types is internal.
