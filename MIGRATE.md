# Migrate to the Wingify Android FME SDK

This guide explains how to adopt the **Wingify** public API (`com.wingify`) on the Android FME SDK. Existing **VWO** integrations (`com.vwo`) continue to work without changes.

For installation, device support, and advanced configuration (storage, logger, polling, and more), see [README.md](README.md).

---

## Overview

The Android FME SDK is available under two Maven coordinates that ship the **same library binary** at the same version:

| Coordinate | Public API |
| --- | --- |
| `com.wingify.sdk:wingify-fme-android-sdk` | `com.wingify.*` (recommended for new apps) |
| `com.vwo.sdk:vwo-fme-android-sdk` | `com.vwo.*` (legacy, deprecated) |

Pick **one** dependency line in your app — do not add both.

New integrations should use the Wingify coordinate and types. When you initialize through `Wingify`, the SDK uses Wingify edge/collect endpoints and Wingify-branded logging (see [Runtime behavior](#runtime-behavior-wingify-mode) below).

---

## Wingify API — implementation guide

Use Maven coordinate `com.wingify.sdk:wingify-fme-android-sdk` and public types under `com.wingify`.

Legacy `com.vwo` APIs remain supported and are marked `@Deprecated` with IDE `ReplaceWith` hints pointing to Wingify equivalents.

### Implementation steps

1. **Add the dependency** — use only the Wingify coordinate:

   ```groovy
   implementation 'com.wingify.sdk:wingify-fme-android-sdk:<latestVersion>'
   ```

2. **Initialize** — create `WingifyInitOptions`, set `sdkKey` and `accountId`, then call `Wingify.init()` with `IWingifyInitCallback` (`wingifyInitSuccess` / `wingifyInitFailed`).

3. **Build user context** — create `WingifyUserContext` with a user `id` (or enable `shouldUseDeviceIdAsUserId` for anonymous users).

4. **Evaluate flags** — call `getFlag` (async with `IWingifyListener`, or sync without a listener). Results are `com.wingify.models.user.GetFlag`.

5. **Track and attribute** — use `trackEvent`, `setAttribute`, and `setAlias` on the initialized client.

6. **Reuse instances (optional)** — `Wingify.getInstance(accountId, sdkKey)` returns a cached client; use `clearInstance` / `clearAllInstances` to reset.

### Kotlin

```kotlin
import com.wingify.Wingify
import com.wingify.Wingify.init
import com.wingify.interfaces.IWingifyInitCallback
import com.wingify.interfaces.IWingifyListener
import com.wingify.models.user.GetFlag
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext

val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID

init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Initialization failed
    }
})

val context = WingifyUserContext()
context.id = "unique_user_id"
context.customVariables = mutableMapOf("key1" to 21, "key2" to 0)

wingifyClient.getFlag("feature_key", context, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        val featureFlag = data as? GetFlag
        val isEnabled = featureFlag?.isEnabled()
        val variable = featureFlag?.getVariable("feature_flag_variable", "default-value") as? String
    }

    override fun onFailure(message: String) {
        // Feature flag disabled or request failed
    }
})

// Synchronous evaluation (no listener)
val flag = wingifyClient.getFlag("feature_key", context)
val enabled = flag.isEnabled()

wingifyClient.trackEvent("event_name", context, mutableMapOf("cartvalue" to 10))
wingifyClient.setAttribute(mapOf("attributeName" to "attributeValue"), context)
wingifyClient.setAlias(context, "alias_id")

// Cached instance for the same account/sdkKey
val cached = Wingify.getInstance(ACCOUNT_ID, SDK_KEY)
```

### Java

```java
import com.wingify.Wingify;
import com.wingify.Wingify.init;
import com.wingify.interfaces.IWingifyInitCallback;
import com.wingify.interfaces.IWingifyListener;
import com.wingify.models.user.GetFlag;
import com.wingify.models.user.WingifyInitOptions;
import com.wingify.models.user.WingifyUserContext;

WingifyInitOptions initOptions = new WingifyInitOptions();
initOptions.setSdkKey(SDK_KEY);
initOptions.setAccountId(ACCOUNT_ID);

init(initOptions, new IWingifyInitCallback() {
    @Override
    public void wingifyInitSuccess(@NonNull Wingify wingifyClient, @NonNull String message) {
        MyActivity.this.wingifyClient = wingifyClient;
    }

    @Override
    public void wingifyInitFailed(@NonNull String message) {
        // Initialization failed
    }
});

WingifyUserContext context = new WingifyUserContext();
context.setId("unique_user_id");

wingifyClient.getFlag("feature_key", context, new IWingifyListener() {
    @Override
    public void onSuccess(Object data) {
        GetFlag featureFlag = (GetFlag) data;
        if (featureFlag != null && featureFlag.isEnabled()) {
            String variable = (String) featureFlag.getVariable("variable_key", "default-value");
        }
    }

    @Override
    public void onFailure(@NonNull String message) {
        // Feature flag disabled or request failed
    }
});

Map<String, Object> properties = new HashMap<>();
properties.put("cartvalue", 120);
wingifyClient.trackEvent("event_name", context, properties);

HashMap<String, Object> attributes = new HashMap<>();
attributes.put("attribute_key", "attribute_value");
wingifyClient.setAttribute(attributes, context);
wingifyClient.setAlias(context, "alias_id");
```

---

## Public API mapping

| Legacy (`com.vwo`) | Wingify (`com.wingify`) |
| --- | --- |
| `VWO` | `Wingify` |
| `VWO.init()` | `Wingify.init()` |
| `VWOInitOptions` | `WingifyInitOptions` |
| `VWOUserContext` | `WingifyUserContext` |
| `com.vwo.models.user.GetFlag` | `com.wingify.models.user.GetFlag` |
| `IVwoInitCallback` | `IWingifyInitCallback` |
| `vwoInitSuccess` / `vwoInitFailed` | `wingifyInitSuccess` / `wingifyInitFailed` |
| `IVwoListener` | `IWingifyListener` |
| `NetworkClientInterface` (VWO) | `com.wingify.interfaces.networking.NetworkClientInterface` |

### Wingify entry points

| Component | Package / type |
| --- | --- |
| SDK client | `com.wingify.Wingify` |
| Init options | `com.wingify.models.user.WingifyInitOptions` |
| User context | `com.wingify.models.user.WingifyUserContext` |
| Feature flag result | `com.wingify.models.user.GetFlag` |
| Init callback (Java) | `com.wingify.interfaces.IWingifyInitCallback` |
| Async callback (Java) | `com.wingify.interfaces.IWingifyListener` |
| Custom network client | `com.wingify.interfaces.networking.NetworkClientInterface` |
| HTTP verbs | `com.wingify.interfaces.networking.HttpMethods` |

`Wingify` supports the same operations as `VWO`: `init`, `getFlag`, `trackEvent`, `setAttribute`, `setAlias`, `updateSettings`, and instance cache helpers (`getInstance`, `clearInstance`, `clearAllInstances`).

### Extension interfaces

Some extension hooks are still declared under `com.vwo` and are accepted by `WingifyInitOptions` as-is:

- `IntegrationCallback`
- `com.vwo.interfaces.logger.LogTransport`
- `com.vwo.packages.logger.enums.LogLevelEnum`
- `com.vwo.packages.storage.Connector`

For custom storage when using the Wingify API, you can subclass `com.wingify.packages.storage.Connector`.

Advanced configuration examples in the [full SDK documentation](#full-sdk-documentation-readme) below use Wingify types.

---

## Legacy VWO API

The following remain available for existing apps:

- `com.vwo.VWO` and its public methods
- `VWOInitOptions`, `VWOUserContext`, `GetFlag`
- `IVwoInitCallback`, `IVwoListener`
- Maven coordinate `com.vwo.sdk:vwo-fme-android-sdk`

Legacy types are marked `@Deprecated` with IDE hints to migrate to Wingify equivalents. You can migrate imports at your own pace; no breaking change is required to stay on `com.vwo`.

---

## Runtime behavior (Wingify mode)

When you initialize through `Wingify` (not `VWO`), you may notice:

| Area | Behavior |
| --- | --- |
| Network | Settings and events use Wingify hosts (`edge.wingify.net`, `collect.wingify.net`) |
| Logcat tag | `Wingify-FME-Android` (legacy init uses `Vwo-fme-android`) |
| Log messages | User-visible log text uses Wingify branding where appropriate |

Event and API payload field names (for example `_vwo_meta`) are unchanged for compatibility with the FME platform.

---

## Migrating from `com.vwo` to `com.wingify`

1. Replace the Maven dependency with `com.wingify.sdk:wingify-fme-android-sdk` at the same version you use today.
2. Update imports using the [public API mapping](#public-api-mapping) table.
3. Rename init callbacks to `wingifyInitSuccess` / `wingifyInitFailed`.
5. Run your existing tests — SDK behavior for flags, events, and attributes is unchanged.

---

## Related documents

| Document | Content |
| --- | --- |
| [README.md](README.md) | Installation and advanced configuration (canonical copy also included below) |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

---

# Full SDK documentation (README)

The following is the complete SDK reference for installation, usage, and advanced configuration (adapted from [README.md](README.md) with Wingify API types).

# Wingify FME Android SDK

[![License](https://img.shields.io/github/license/wingify/vwo-fme-android-sdk?style=for-the-badge&color=blue)](http://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://img.shields.io/github/actions/workflow/status/wingify/vwo-fme-android-sdk/android-unit-tests.yml?style=for-the-badge&logo=github)](https://github.com/wingify/vwo-fme-node-sdk/actions?query=workflow%3ACI)

## Overview

The **Wingify Feature Management and Experimentation SDK** (Wingify FME Android SDK) enables Android developers to integrate feature flagging and experimentation into their applications across mobile, tablet and Android tv. This SDK provides full control over feature rollout, A/B testing, and event tracking, allowing teams to manage features dynamically and gain insights into user behavior.

## Requirements

The Android SDK supports: `Android API level 21 onwards`

## Device Support

This SDK supports the following devices:

- Mobile
- Tablet
- Android TV

## SDK Installation

Add below Maven dependency in your project's `build.gradle` file.

```groovy
implementation 'com.wingify.sdk:wingify-fme-android-sdk:<latestVersion>'
```

Latest version: [wingify-fme-android-sdk](https://mvnrepository.com/artifact/com.wingify.sdk/wingify-fme-android-sdk)

## Basic Usage

The following demonstrates initializing the SDK with a Wingify account ID and SDK key, setting a user context, checking if a feature flag is enabled, and tracking a custom event.

### Kotlin
```kotlin
import com.wingify.Wingify
import com.wingify.Wingify.init
import com.wingify.interfaces.IWingifyInitCallback
import com.wingify.interfaces.IWingifyListener
import com.wingify.models.user.GetFlag
import com.wingify.models.user.WingifyUserContext
import com.wingify.models.user.WingifyInitOptions

// Initialize Wingify SDK
val wingifyInitOptions = WingifyInitOptions()
// Set SDK Key and Account ID
wingifyInitOptions.sdkKey = SDK_KEY
wingifyInitOptions.accountId = ACCOUNT_ID

// Create Wingify instance with the wingifyInitOptions
init(wingifyInitOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        //Initialization failed
    }
})

// Create WingifyUserContext object
var context = WingifyUserContext()
// Set User ID
context.id = "unique_user_id"
context.customVariables = mutableMapOf("key1" to 21, "key2" to 0)

// Get the GetFlag object for the feature key and context
wingifyClient.getFlag("feature_key", context, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        featureFlag = data as? GetFlag
        // Get the flag value
        val isFeatureFlagEnabled = featureFlag?.isEnabled()

        // Get the variable value for the given variable key and default value
        val variable: String = featureFlag.getVariable("feature_flag_variable", "default-value") as String
    }

    override fun onFailure(message: String) {
        //Feature flag is disabled
    }
})

// Track the event for the given event name and context
val properties = mutableMapOf<String, Any>("cartvalue" to 10)
wingifyClient?.trackEvent("feature_event", context, properties)

// send attributes data
val attributes = mapOf(
    "attributeName" to "attributeValue"
)
wingifyClient?.setAttribute(attributes, context)
```

Java usage
```java
import com.wingify.Wingify;
import com.wingify.Wingify.init;
import com.wingify.interfaces.IWingifyInitCallback;
import com.wingify.interfaces.IWingifyListener;
import com.wingify.models.user.GetFlag;
import com.wingify.models.user.WingifyUserContext;
import com.wingify.models.user.WingifyInitOptions;

// Initialize Wingify SDK
WingifyInitOptions wingifyInitOptions = new WingifyInitOptions();
// Set SDK Key and Account ID
wingifyInitOptions.setSdkKey(SDK_KEY);
wingifyInitOptions.setAccountId(ACCOUNT_ID);

// Create Wingify instance with the wingifyInitOptions
init(wingifyInitOptions, new IWingifyInitCallback() {
    @Override
    public void wingifyInitSuccess(@NonNull Wingify wingifyClient, @NonNull String message) {
        MyActivity.this.wingifyClient = wingifyClient;
    }

    @Override
    public void wingifyInitFailed(@NonNull String message) {
        //Initialization failed
    }
});

// Create WingifyUserContext object
WingifyUserContext context = new WingifyUserContext();
context.setId("unique_user_id");

Map<String, Object> customVariables = new HashMap<>();
customVariables.put("variable", "variable-value");
context.setCustomVariables(customVariables);

// Get the GetFlag object for the feature key and context
wingifyClient.getFlag("feature-key", context, new IWingifyListener() {
    public void onSuccess(Object data) {

        GetFlag featureFlag = (GetFlag) data;
        // Get the flag value
        boolean isFeatureFlagEnabled = false;
        if (featureFlag != null) {
            isFeatureFlagEnabled = featureFlag.isEnabled();
        }
        if (isFeatureFlagEnabled) {
            // Get the variable value for the given variable key and default value
            String variable = (String) featureFlag.getVariable("variable_key", "default-value");
            List<Map<String, Object>> getAllVariables = featureFlag.getVariables();
        } else {
            //Feature flag is disabled
        }
    }

    public void onFailure(@NonNull String message) {
        //Error in getFlag
    }
});

// Track the event for the given event name, user context and properties
Map<String, Object> properties = new HashMap<>();
properties.put("cartvalue", 120);
wingifyClient.trackEvent("eventName", context, properties);

// Send attributes data
HashMap<String, Object> attributes = new HashMap<>();
attributes.put("attribute_key", "attribute_value");
wingifyClient.setAttribute(attributes, context);
```

## Advanced Configuration Options

To customize the SDK further, additional parameters can be passed to the `init()` API. Here’s a table describing each option:

| **Parameter**              | **Description**                                                                                                                                             | **Required** | **Type** | **Example**                     |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------| ------------ |----------|---------------------------------|
| `accountId`                | Wingify account ID for authentication.                                                                                                                          | Yes          | Integer  | `123456`                        |
| `sdkKey`                   | SDK key corresponding to the specific environment to initialize the Wingify SDK client. You can get this key from the Wingify application.                              | Yes          | String   | `'32-alpha-numeric-sdk-key'`    |
| `pollInterval`             | Time interval for fetching updates from Wingify servers (in milliseconds).                                                                                      | No           | Integer   | `60000`                         |
| `storage`                  | Custom storage connector for persisting user decisions and campaign data.                                                                                   | No           | Object   | See [Storage](#storage) section |
| `logger`                   | Toggle log levels for more insights or for debugging purposes. You can also customize your own transport in order to have better control over log messages. | No           | Object   | See [Logger](#logger) section   |
| `cachedSettingsExpiryTime` | Controls the duration (in milliseconds) the SDK uses cached settings before fetching new ones.                                                              | No           | Integer  | `60000`                         |
| `batchMinSize`             | Uploads are triggered when the batch reaches this minimum size.                                                                                             | No           | Integer  | `10`                            |
| `batchUploadTimeInterval`  | Specifies the time interval (in milliseconds) for periodic batch uploads.                                                                                   | No           | Integer  | `60000`                         |

Refer to the [official documentation](https://developers.vwo.com/v2/docs/fme-android-install) for additional parameter details.

### User Context

The `context` object uniquely identifies users and is crucial for consistent feature rollouts. A typical `context` includes an `id` for identifying the user. It can also include other attributes that can be used for targeting and segmentation, such as `customVariables`.

#### Parameters Table

The following table explains all the parameters in the `context` object:

| **Parameter**     | **Description**                                                            | **Required** | **Type** | **Example**                      |
| ----------------- | -------------------------------------------------------------------------- | ------------ | -------- | -------------------------------- |
| `id`              | Unique identifier for the user.                                            | Yes          | String   | `'unique_user_id'`               |
| `customVariables` | Custom attributes for targeting.                                           | No           | Object   | `mutableMapOf("age" to 25))`     |
| `shouldUseDeviceIdAsUserId`  | Use device ID as user ID when user ID is not provided.                  | No           | Boolean  | `true`                           |

#### Example

```kotlin
val context = WingifyUserContext()
context.id = USER_ID
context.customVariables = mutableMapOf(
    "age" to 25,
    "location" to "US"
)
```

Based on the changes made to the Android SDK, here's a concise description for the README.md about device ID usage:

#### Device ID Configuration

The SDK supports automatic device ID generation when a user ID is not provided. This feature helps maintain consistent user identification across app sessions.

##### Enable Device ID

To enable device ID generation, set the `shouldUseDeviceIdAsUserId` property in your `WingifyUserContext`:

```kotlin
// Kotlin
val userContext = WingifyUserContext()
userContext.id = "" // Empty ID to trigger device ID fallback
userContext.shouldUseDeviceIdAsUserId = true // Use device ID as user ID
```

```java
// Java
WingifyUserContext context = new WingifyUserContext();
context.setId(""); // Empty ID to trigger device ID fallback
context.setShouldUseDeviceIdAsUserId(true); // Use device ID as user ID
```

##### How It Works

- User ID Priority: If a user ID is provided, it takes precedence over device ID
- Device ID Fallback: When no user ID is available and `shouldUseDeviceIdAsUserId` is enabled, the SDK generates a persistent device ID
- Privacy-Friendly: Device IDs are hashed using SHA-256 for enhanced privacy protection
- Persistent: Device IDs remain consistent across app uninstalls/reinstalls but may change on factory resets

##### Usage Example

```kotlin
// Create user context with device ID enabled
val userContext = WingifyUserContext()
userContext.shouldUseDeviceIdAsUserId = true
// Leave userContext.id empty or null to use device ID

// Use the context for feature flags
wingifyClient.getFlag("feature_key", userContext, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        val featureFlag = data as? GetFlag
        // Device ID will be automatically generated and used
    }
    
    override fun onFailure(message: String) {
        // Handle error
    }
})
```

##### Error Handling

If neither a user ID is provided nor device ID is enabled, the SDK will log an error.

This feature is particularly useful for anonymous users or scenarios where explicit user identification is not available.

### Custom Bucketing Seed

The `bucketingSeed` property in `WingifyUserContext` lets you enforce consistent feature flag decisions across different users. When a seed is provided, the SDK uses it as the bucketing identifier instead of the user ID, so any two users sharing the same seed will always land on the same variation.

Common use-cases:
- **Household / account-level consistency** – everyone in the same household or account gets the same experience.
- **Cross-device consistency** – the same logical identity resolves to the same variation regardless of the device user ID.

#### Parameters

| **Parameter**   | **Description**                                                                                    | **Required** | **Type** | **Example**         |
| --------------- | -------------------------------------------------------------------------------------------------- | ------------ | -------- | ------------------- |
| `bucketingSeed` | Seed used for bucketing instead of user ID. Falls back to `context.id` if `null` or empty string. | No           | String   | `"household-123"`   |

#### Behaviour

- If `bucketingSeed` is set (non-null, non-empty) it takes priority over `context.id` for bucketing.
- If `bucketingSeed` is `null` or `""`, the SDK falls back to `context.id`.
- Forced variations (whitelisted users) always take precedence over the bucketing seed.

#### Example

Kotlin usage:
```kotlin
// Two different users sharing the same seed will receive the same variation
val context1 = WingifyUserContext().apply {
    id = "user_alice"
    bucketingSeed = "household-123"
}
val context2 = WingifyUserContext().apply {
    id = "user_bob"
    bucketingSeed = "household-123"
}

wingifyClient.getFlag("feature_key", context1, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        val flag = data as? GetFlag
        // alice and bob will receive the same variation
        val isEnabled = flag?.isEnabled()
    }
    override fun onFailure(message: String) {}
})
```

Java usage:
```java
// Two different users sharing the same seed will receive the same variation
WingifyUserContext context1 = new WingifyUserContext();
context1.setId("user_alice");
context1.setBucketingSeed("household-123");

WingifyUserContext context2 = new WingifyUserContext();
context2.setId("user_bob");
context2.setBucketingSeed("household-123");

wingifyClient.getFlag("feature_key", context1, new IWingifyListener() {
    public void onSuccess(Object data) {
        GetFlag flag = (GetFlag) data;
        // alice and bob will receive the same variation
        boolean isEnabled = flag != null && flag.isEnabled();
    }
    public void onFailure(String message) {}
});
```

### Basic Feature Flagging

Feature Flags serve as the foundation for all testing, personalization, and rollout rules within FME.
To implement a feature flag, first use the `getFlag` API to retrieve the flag configuration.
The `getFlag` API provides a simple way to check if a feature is enabled for a specific user and access its variables. It returns a feature flag object that contains methods for checking the feature's status and retrieving any associated variables.

| Parameter    | Description                                                      | Required | Type   | Example                                                                               |
| ------------ |------------------------------------------------------------------| -------- | ------ |---------------------------------------------------------------------------------------|
| `featureKey` | Unique identifier of the feature flag                            | Yes      | String | `'new_checkout'`                                                                      |
| `context`    | Object containing user identification and contextual information | Yes      | Object | `WingifyUserContext()`                                                                    |
| `listener`   | Callback object to receive status update about the operation.    | Yes      | Object | see [Feature Flags & Variables](https://developers.vwo.com/v2/docs/fme-android-flags) |

Example usage:

```kotlin
wingifyClient.getFlag("featureKey", context, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        featureFlag = data as? GetFlag
        // Get the flag value
        val isFeatureFlagEnabled = featureFlag?.isEnabled()

        // Get the variable value for the given variable key and default value
        val variable: String = featureFlag.getVariable("feature_flag_variable", "default-value") as String
    }

    override fun onFailure(message: String) {
        //Feature flag is disabled
    }
})
```

### Custom Event Tracking

Feature flags can be enhanced with connected metrics to track key performance indicators (KPIs) for your features. These metrics help measure the effectiveness of your testing rules by comparing control versus variation performance, and evaluate the impact of personalization and rollout campaigns. Use the `trackEvent` API to track custom events like conversions, user interactions, and other important metrics:

| Parameter         | Description                                                            | Required | Type   | Example                                     |
| ----------------- | ---------------------------------------------------------------------- | -------- | ------ |---------------------------------------------|
| `eventName`       | Name of the event you want to track                                    | Yes      | String | `'purchase_completed'`                      |
| `context`         | Object containing user identification and other contextual information | Yes      | Object | `WingifyUserContext()`                          |
| `eventProperties` | Additional properties/metadata associated with the event               | No       | Object | `mutableMapOf<String, Any>("amount" to 10)` |

Example usage:

```kotlin
val context = WingifyUserContext()
context.id = USER_ID
val properties = mutableMapOf<String, Any>("cartvalue" to 10)
wingifyClient?.trackEvent("feature_event", context, properties)
```

See [Tracking Conversions](https://developers.vwo.com/v2/docs/fme-android-metrics#usage) documentation for more information.

### Pushing Attributes

User attributes provide rich contextual information about users, enabling powerful personalization. The `setAttribute` method provides a simple way to associate these attributes with users in Wingify for advanced segmentation. Here's what you need to know about the method parameters:

| Parameter        | Description                                                            | Required | Type   | Example                 |
|------------------|------------------------------------------------------------------------| -------- |--------|-------------------------|
| `attributes`     | Map of attribute key and value to be set                               | Yes      | Object | `mapOf("price" to 99)`  |
| `context`        | Object containing user identification and other contextual information | Yes      | Object | `WingifyUserContext()`      |

Example usage:

```kotlin
val context = WingifyUserContext()
context.id = USER_ID
val attributes = mapOf("price" to 99)
wingifyClient?.setAttribute(attributes, context)
```

See [Pushing Attributes](https://developers.vwo.com/v2/docs/fme-android-attributes#usage) documentation for additional information.

### Polling Interval Adjustment

The `pollInterval` is an optional parameter that allows the SDK to automatically fetch and update settings from the Wingify server at specified intervals. Setting this parameter ensures your application always uses the latest configuration.

```kotlin
val wingifyInitOptions = WingifyInitOptions()
wingifyInitOptions.sdkKey = SDK_KEY
wingifyInitOptions.accountId = ACCOUNT_ID
wingifyInitOptions.pollInterval = 60000

// Create Wingify instance with the wingifyInitOptions
init(wingifyInitOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        //Initialization failed
    }
})
```

### Storage

The SDK operates in a stateless mode by default, meaning each `getFlag` call triggers a fresh evaluation of the flag against the current user context.

To optimize performance and maintain consistency SDK will use internal storage if application context is provided. You can implement a custom storage mechanism by passing a `storage` parameter during initialization. This allows you to persist feature flag decisions in your preferred data store.

Key benefits of implementing storage:

- Improved performance by caching decisions
- Consistent user experience across sessions
- Reduced load on your application

The storage mechanism ensures that once a decision is made for a user, it remains consistent even if campaign settings are modified in the Wingify application. This is particularly useful for maintaining a stable user experience during A/B tests and feature rollouts.

```kotlin
class StorageConnector: Connector() {

    /**
     * Stores the data in the storage.
     *
     * @param data A map containing the data to be stored.
     */
    override fun set(data: Map<String, Any>) {
        // Set data corresponding to a featureKey & user id
    }

    /**
     * Retrieves the data from the storage.
     *
     * @param featureKey The feature key for the data.
     * @param userId The user ID for the data.
     * @return The data if found, or null otherwise.
     */
    override fun get(featureKey: String?, userId: String?): Any? {
        // return await data (based on featureKey and userId)
    }
}

val wingifyInitOptions = WingifyInitOptions()
wingifyInitOptions.sdkKey = SDK_KEY
wingifyInitOptions.accountId = ACCOUNT_ID
wingifyInitOptions.storage = StorageConnector()

init(wingifyInitOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        // Success
        this@MainActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Log error here
    }
})
```

### Logger

The Wingify SDK by default logs all `ERROR` level messages to logcat. To gain more control over Wingify's logging behaviour, you can use the `logger` parameter in the `init` configuration.

| **Parameter** | **Description**                        | **Required** | **Type** | **Example**           |
| ------------- | -------------------------------------- | ------------ | -------- | --------------------- |
| `level`       | Log level to control verbosity of logs | Yes          | String   | `DEBUG`               |
| `transport`   | Custom logger implementation           | No           | Object   | See example below     |

#### Example 1: Set log level to control verbosity of logs
```kotlin
val wingifyInitOptions = WingifyInitOptions()
wingifyInitOptions.sdkKey = SDK_KEY
wingifyInitOptions.accountId = ACCOUNT_ID
wingifyInitOptions.logger = mutableMapOf<String, Any>().apply { put("level", "TRACE") }

init(wingifyInitOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        // Success
        this@MainActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Log error here
    }
})
```
#### Example 2: Implement custom transport to handle logs your way

The `transport` parameter allows you to implement custom logging behavior by providing your own logging functions. You can define handlers for different log levels (TRACE, DEBUG, INFO, WARN, ERROR) to process log messages according to your needs.
For example, you could:

- Send logs to a third-party logging service
- Write logs to a file
- Format log messages differently
- Filter or transform log messages

The transport object should implement `defaultTransport` handler to customize.

```kotlin
val wingifyInitOptions = WingifyInitOptions()
wingifyInitOptions.sdkKey = SDK_KEY
wingifyInitOptions.accountId = ACCOUNT_ID
val logger: MutableList<Map<String, Any>> = mutableListOf()
val transport: MutableMap<String, Any> = mutableMapOf()
transport["defaultTransport"] = object : LogTransport {
    override fun log(level: LogLevelEnum, message: String?) {
        if (message == null) return
        Log.d("FME", message)
    }
}
logger.add(transport)
wingifyInitOptions.logger = mutableMapOf<String, Any>().apply {
    put("level", "TRACE")
    put("transports", logger)
}
```

### Running Unit Tests

The SDK includes a comprehensive test suite to ensure reliability and functionality. To run the unit tests:

Using Android Studio:
   - Right-click on the `test` directory in the project view
   - Select "Run Tests in 'test'"

The test suite includes:
- Unit tests for core functionality
- Integration tests for API interactions
- Mock tests for external dependencies
- Coverage reports for code quality assurance

## 📊 Analytics Integration with Mixpanel

Wingify FME SDK provides integration capabilities with analytics platforms like Mixpanel. This allows you to track feature flag evaluations and events in your analytics dashboard.

### Kotlin Implementation

```kotlin
// 1. Create a MixpanelIntegration class
import android.content
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

class MixpanelIntegration private constructor(context: Context, projectToken: String) {
    private val mixpanel: MixpanelAPI = MixpanelAPI.getInstance(context, projectToken, true)

    companion object {
        @Volatile
        private var instance: MixpanelIntegration? = null

        fun getInstance(context: Context, projectToken: String): MixpanelIntegration {
            return instance ?: synchronized(this) {
                instance ?: MixpanelIntegration(context, projectToken).also { instance = it }
            }
        }
    }

    fun trackEvent(eventName: String, properties: Map<String, Any>) {
        val props = JSONObject()
        properties.forEach { (key, value) ->
            props.put(key, value)
        }
        mixpanel.track("vwo_fme_track_event", props)
    }

    fun trackFlagEvaluation(properties: Map<String, Any>) {
        mixpanel.trackMap("vwo_fme_flag_evaluation", properties)
    }
}

// 2. Initialize Mixpanel and set up integration callback
val mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN
mixpanelIntegration = MixpanelIntegration.getInstance(context, mixpanelToken)

initOptions.integrations = object : IntegrationCallback {
    override fun execute(properties: Map<String, Any>) {
        // Check if this is a flag evaluation or event tracking
        if (properties["api"] == "track") {
            // This is event tracking
            val eventName = properties["eventName"] as String
            mixpanelIntegration?.trackEvent(eventName, properties)
        } else if (properties.containsKey("featureName")) {
            // This is a flag evaluation
            mixpanelIntegration?.trackFlagEvaluation(properties)
        }
    }
}
```

### Java Implementation

```java
// 1. Create a MixpanelIntegration class
import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;

public class MixpanelIntegration {
    private static volatile MixpanelIntegration instance;
    private final MixpanelAPI mixpanel;

    private MixpanelIntegration(Context context, String projectToken) {
        mixpanel = MixpanelAPI.getInstance(context, projectToken, true);
    }

    public static MixpanelIntegration getInstance(Context context, String projectToken) {
        if (instance == null) {
            synchronized (MixpanelIntegration.class) {
                if (instance == null) {
                    instance = new MixpanelIntegration(context, projectToken);
                }
            }
        }
        return instance;
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        JSONObject props = new JSONObject();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            try {
                props.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mixpanel.track("vwo_fme_track_event", props);
    }

    public void trackFlagEvaluation(Map<String, Object> properties) {
        mixpanel.trackMap("vwo_fme_flag_evaluation", properties);
    }
}

// 2. Initialize Mixpanel and set up integration callback
String mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN;
final MixpanelIntegration mixpanelIntegration = MixpanelIntegration.getInstance(context, mixpanelToken);

WingifyInitOptions initOptions = new WingifyInitOptions();
initOptions.setIntegrations(new IntegrationCallback() {
    @Override
    public void execute(Map<String, Object> properties) {
        // Check if this is a flag evaluation or event tracking
        if ("track".equals(properties.get("api"))) {
            // This is event tracking
            String eventName = (String) properties.get("eventName");
            mixpanelIntegration.trackEvent(eventName, properties);
        } else if (properties.containsKey("featureName")) {
            // This is a flag evaluation
            mixpanelIntegration.trackFlagEvaluation(properties);
        }
    }
});
```

### Integration Data

When using the integration callback, you'll receive the following data:

- **For flag evaluations**:
  ```
  {
    featureName: "yourFlagName",
    featureId: 5,
    featureKey: "yourFlagKey",
    userId: "0duMh1j7krRB",
    ...
  }
  ```

- **For event tracking**:
  ```
  {
    eventName: "yourEventName",
    api: "track"
  }
  ```

Don't forget to add your Mixpanel project token to your `local.properties` file:
```
MIXPANEL_PROJECT_TOKEN=YOUR_PROJECT_TOKEN
```

### Version History

The version history tracks changes, improvements and bug fixes in each version. For a full history, see the [CHANGELOG.md](https://github.com/wingify/vwo-fme-android-sdk/blob/master/CHANGELOG.md).

## Contributing

We welcome contributions to improve this SDK! Please read our [contributing guidelines](https://github.com/wingify/vwo-fme-android-sdk/blob/master/CONTRIBUTING.md) before submitting a PR.

## Code of Conduct

[Code of Conduct](https://github.com/wingify/vwo-fme-android-sdk/blob/master/CODE_OF_CONDUCT.md)

## License

[Apache License, Version 2.0](https://github.com/wingify/vwo-fme-android-sdk/blob/master/LICENSE)

Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
