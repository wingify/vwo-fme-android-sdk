# VWO FME Android SDK

[![License](https://img.shields.io/github/license/wingify/vwo-fme-android-sdk?style=for-the-badge&color=blue)](http://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://img.shields.io/github/actions/workflow/status/wingify/vwo-fme-android-sdk/android-unit-tests.yml?style=for-the-badge&logo=github)](https://github.com/wingify/vwo-fme-node-sdk/actions?query=workflow%3ACI)

## Overview

The **VWO Feature Management and Experimentation SDK** (VWO FME Android SDK) enables Android developers to integrate feature flagging and experimentation into their applications across mobile, tablet and Android tv. This SDK provides full control over feature rollout, A/B testing, and event tracking, allowing teams to manage features dynamically and gain insights into user behavior.

## Requirements

The Android SDK supports: `Android API level 21 onwards`

## Device Support

This SDK supports the following devices:

- Mobile
- Tablet
- Android TV

## SDK Installation

Add the Maven dependency in your project's `build.gradle` file.

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```groovy
implementation 'com.vwo.sdk:vwo-fme-android-sdk:<latestVersion>'
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```groovy
implementation 'com.wingify.sdk:wingify-fme-android-sdk:<latestVersion>'
```

</details>

Both coordinates publish the **same** library binary at the same version. Pick one dependency line; do not add both.

Latest versions: [vwo-fme-android-sdk](https://mvnrepository.com/artifact/com.vwo.sdk/vwo-fme-android-sdk) · [wingify-fme-android-sdk](https://mvnrepository.com/artifact/com.wingify.sdk/wingify-fme-android-sdk)

## Basic Usage Example

The following example demonstrates initializing the SDK with an account ID and SDK key, setting a user context, checking if a feature flag is enabled, and tracking a custom event.

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

Kotlin usage
```kotlin
import com.vwo.VWO
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOUserContext
import com.vwo.models.user.VWOInitOptions

val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID

VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        this@MyActivity.vwoClient = vwoClient

        val context = VWOUserContext()
        context.id = "unique_user_id"
        context.customVariables = mutableMapOf("key1" to 21, "key2" to 0)

        vwoClient.getFlag("feature_key", context, object : IVwoListener {
            override fun onSuccess(data: Any) {
                val featureFlag = data as? GetFlag
                val isFeatureFlagEnabled = featureFlag?.isEnabled()

                val variable: String = featureFlag?.getVariable("feature_flag_variable", "default-value") as String
            }

            override fun onFailure(message: String) {
                // Feature flag is disabled or request failed
            }
        })

        val properties = mutableMapOf<String, Any>("cartvalue" to 10)
        vwoClient.trackEvent("vwoevent", context, properties)

        val attributes = mapOf("attributeName" to "attributeValue")
        vwoClient.setAttribute(attributes, context)
    }

    override fun vwoInitFailed(message: String) {
        // Initialization failed
    }
})
```

Java usage
```java
import com.vwo.VWO;
import com.vwo.interfaces.IVwoInitCallback;
import com.vwo.interfaces.IVwoListener;
import com.vwo.models.user.GetFlag;
import com.vwo.models.user.VWOUserContext;
import com.vwo.models.user.VWOInitOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

VWOInitOptions vwoInitOptions = new VWOInitOptions();
vwoInitOptions.setSdkKey(SDK_KEY);
vwoInitOptions.setAccountId(ACCOUNT_ID);

VWO.init(vwoInitOptions, new IVwoInitCallback() {
    @Override
    public void vwoInitSuccess(VWO vwoClient, String message) {
        MyActivity.this.vwoClient = vwoClient;

        VWOUserContext context = new VWOUserContext();
        context.setId("unique_user_id");

        Map<String, Object> customVariables = new HashMap<>();
        customVariables.put("variable", "variable-value");
        context.setCustomVariables(customVariables);

        vwoClient.getFlag("feature-key", context, new IVwoListener() {
            @Override
            public void onSuccess(Object data) {
                GetFlag featureFlag = (GetFlag) data;
                boolean isFeatureFlagEnabled = featureFlag != null && featureFlag.isEnabled();
                if (isFeatureFlagEnabled) {
                    String variable = (String) featureFlag.getVariable("variable_key", "default-value");
                    List<Map<String, Object>> getAllVariables = featureFlag.getVariables();
                }
            }

            @Override
            public void onFailure(String message) {
                // Error in getFlag
            }
        });

        Map<String, Object> properties = new HashMap<>();
        properties.put("cartvalue", 120);
        vwoClient.trackEvent("eventName", context, properties);

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("attribute_key", "attribute_value");
        vwoClient.setAttribute(attributes, context);
    }

    @Override
    public void vwoInitFailed(String message) {
        // Initialization failed
    }
});
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

Kotlin usage
```kotlin
import com.wingify.Wingify
import com.wingify.interfaces.IWingifyInitCallback
import com.wingify.interfaces.IWingifyListener
import com.wingify.models.user.GetFlag
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext

val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID

Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient

        val context = WingifyUserContext()
        context.id = "unique_user_id"
        context.customVariables = mutableMapOf("key1" to 21, "key2" to 0)

        wingifyClient.getFlag("feature_key", context, object : IWingifyListener {
            override fun onSuccess(data: Any) {
                val featureFlag = data as? GetFlag
                val isFeatureFlagEnabled = featureFlag?.isEnabled()

                val variable: String = featureFlag?.getVariable("feature_flag_variable", "default-value") as String
            }

            override fun onFailure(message: String) {
                // Feature flag is disabled or request failed
            }
        })

        val properties = mutableMapOf<String, Any>("cartvalue" to 10)
        wingifyClient.trackEvent("vwoevent", context, properties)

        val attributes = mapOf("attributeName" to "attributeValue")
        wingifyClient.setAttribute(attributes, context)
    }

    override fun wingifyInitFailed(message: String) {
        // Initialization failed
    }
})
```

Java usage
```java
import com.wingify.Wingify;
import com.wingify.interfaces.IWingifyInitCallback;
import com.wingify.interfaces.IWingifyListener;
import com.wingify.models.user.GetFlag;
import com.wingify.models.user.WingifyInitOptions;
import com.wingify.models.user.WingifyUserContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

WingifyInitOptions initOptions = new WingifyInitOptions();
initOptions.setSdkKey(SDK_KEY);
initOptions.setAccountId(ACCOUNT_ID);

Wingify.init(initOptions, new IWingifyInitCallback() {
    @Override
    public void wingifyInitSuccess(Wingify wingifyClient, String message) {
        MyActivity.this.wingifyClient = wingifyClient;

        WingifyUserContext context = new WingifyUserContext();
        context.setId("unique_user_id");

        Map<String, Object> customVariables = new HashMap<>();
        customVariables.put("variable", "variable-value");
        context.setCustomVariables(customVariables);

        wingifyClient.getFlag("feature-key", context, new IWingifyListener() {
            @Override
            public void onSuccess(Object data) {
                GetFlag featureFlag = (GetFlag) data;
                boolean isFeatureFlagEnabled = featureFlag != null && featureFlag.isEnabled();
                if (isFeatureFlagEnabled) {
                    String variable = (String) featureFlag.getVariable("variable_key", "default-value");
                    List<Map<String, Object>> getAllVariables = featureFlag.getVariables();
                }
            }

            @Override
            public void onFailure(String message) {
                // Error in getFlag
            }
        });

        Map<String, Object> properties = new HashMap<>();
        properties.put("cartvalue", 120);
        wingifyClient.trackEvent("eventName", context, properties);

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("attribute_key", "attribute_value");
        wingifyClient.setAttribute(attributes, context);
    }

    @Override
    public void wingifyInitFailed(String message) {
        // Initialization failed
    }
});
```

</details>

| Legacy (`com.vwo`) | New (`com.wingify`) |
| --- | --- |
| `VWO` | `Wingify` |
| `VWOInitOptions` | `WingifyInitOptions` |
| `VWOUserContext` | `WingifyUserContext` |
| `IVwoInitCallback` | `IWingifyInitCallback` |
| `IVwoListener` | `IWingifyListener` |
| `GetFlag` / `FlagCollection` (`com.vwo.models.user`) | `GetFlag` / `FlagCollection` (`com.wingify.models.user`) |

Existing apps can keep using `com.vwo` without changes. Migrate imports when convenient; IDE `ReplaceWith` hints are provided on deprecated types. See [MIGRATE.md](MIGRATE.md) for the Wingify migration guide and API reference.

## Advanced Configuration Options

To customize the SDK further, additional parameters can be passed to the `init()` API. Here’s a table describing each option:

| **Parameter**              | **Description**                                                                                                                                             | **Required** | **Type** | **Example**                     |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------| ------------ |----------|---------------------------------|
| `accountId`                | VWO Account ID for authentication.                                                                                                                          | Yes          | Integer  | `123456`                        |
| `sdkKey`                   | SDK key corresponding to the specific environment to initialize the VWO SDK Client. You can get this key from VWO Application.                              | Yes          | String   | `'32-alpha-numeric-sdk-key'`    |
| `pollInterval`             | Time interval for fetching updates from VWO servers (in milliseconds).                                                                                      | No           | Integer   | `60000`                         |
| `storage`                  | Custom storage connector for persisting user decisions and campaign data.                                                                                   | No           | Object   | See [Storage](#storage) section |
| `context`                  | Android application context. Required for device ID generation, default storage, and cached settings.                                                       | No           | Context  | `applicationContext`            |
| `gatewayService`           | Configuration for integrating VWO Gateway Service.                                                                                                          | No           | Object   | See [Gateway](#gateway) section |
| `isAliasingEnabled`        | Enables user aliasing. Requires a configured gateway.                                                                                                       | No           | Boolean  | `true`                          |
| `logger`                   | Toggle log levels for more insights or for debugging purposes. You can also customize your own transport in order to have better control over log messages. | No           | Object   | See [Logger](#logger) section   |
| `cachedSettingsExpiryTime` | Controls the duration (in milliseconds) the SDK uses cached settings before fetching new ones.                                                              | No           | Integer  | `60000`                         |
| `batchMinSize`             | Uploads are triggered when the batch reaches this minimum size.                                                                                             | No           | Integer  | `10`                            |
| `batchUploadTimeInterval`  | Specifies the time interval (in milliseconds) for periodic batch uploads.                                                                                   | No           | Integer  | `60000`                         |
| `shouldTriggerIntegrationCallbackAlways` | When `true`, the integration callback is fired on every `getFlag()` call (including holdout, stored-decision, and feature-not-found paths). Defaults to `false`. | No | Boolean | `false` |

Refer to the [official VWO documentation](https://developers.vwo.com/v2/docs/fme-android-install) for additional parameter details.

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

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val context = VWOUserContext()
context.id = USER_ID
context.customVariables = mutableMapOf(
    "age" to 25,
    "location" to "US"
)
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val context = WingifyUserContext()
context.id = USER_ID
context.customVariables = mutableMapOf(
    "age" to 25,
    "location" to "US"
)
```

</details>

#### Device ID Configuration

The SDK can generate a persistent device ID when a user ID is not provided. Device ID generation requires an Android `context` on the init options.

##### Enable Device ID

Set `shouldUseDeviceIdAsUserId` on the user context and leave `id` unset. Also set `context` on the init options:

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.context = applicationContext

val userContext = VWOUserContext()
userContext.shouldUseDeviceIdAsUserId = true
```

```java
VWOInitOptions vwoInitOptions = new VWOInitOptions();
vwoInitOptions.setSdkKey(SDK_KEY);
vwoInitOptions.setAccountId(ACCOUNT_ID);
vwoInitOptions.setContext(getApplicationContext());

VWOUserContext context = new VWOUserContext();
context.setShouldUseDeviceIdAsUserId(true);
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.context = applicationContext

val userContext = WingifyUserContext()
userContext.shouldUseDeviceIdAsUserId = true
```

```java
WingifyInitOptions initOptions = new WingifyInitOptions();
initOptions.setSdkKey(SDK_KEY);
initOptions.setAccountId(ACCOUNT_ID);
initOptions.setContext(getApplicationContext());

WingifyUserContext context = new WingifyUserContext();
context.setShouldUseDeviceIdAsUserId(true);
```

</details>

##### How It Works

- User ID priority: if `id` is non-empty, it is used and device ID is skipped
- Device ID fallback: when `id` is null or empty, `shouldUseDeviceIdAsUserId` is true, and init `context` is set, the SDK uses Android ID
- Persistent: Android ID stays the same across app reinstalls, but can change on factory reset

##### Usage Example

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val userContext = VWOUserContext()
userContext.shouldUseDeviceIdAsUserId = true

vwoClient.getFlag("feature_key", userContext, object : IVwoListener {
    override fun onSuccess(data: Any) {
        val featureFlag = data as? GetFlag
        // Device ID is generated and used as the user ID
    }

    override fun onFailure(message: String) {
        // Handle error
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val userContext = WingifyUserContext()
userContext.shouldUseDeviceIdAsUserId = true

wingifyClient.getFlag("feature_key", userContext, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        val featureFlag = data as? GetFlag
        // Device ID is generated and used as the user ID
    }

    override fun onFailure(message: String) {
        // Handle error
    }
})
```

</details>

##### Error Handling

If neither a user ID is provided nor device ID can be generated (flag off, or init `context` missing), `getFlag` fails with an invalid-context error.

This feature is useful for anonymous users when explicit user identification is not available.

### Custom Bucketing Seed

The `bucketingSeed` property in the user context lets you enforce consistent feature flag decisions across different users. When a seed is provided, the SDK uses it as the bucketing identifier instead of the user ID, so any two users sharing the same seed will always land on the same variation.

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

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

Kotlin usage:
```kotlin
// Two different users sharing the same seed will receive the same variation
val context1 = VWOUserContext().apply {
    id = "user_alice"
    bucketingSeed = "household-123"
}
val context2 = VWOUserContext().apply {
    id = "user_bob"
    bucketingSeed = "household-123"
}

vwoClient.getFlag("feature_key", context1, object : IVwoListener {
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
VWOUserContext context1 = new VWOUserContext();
context1.setId("user_alice");
context1.setBucketingSeed("household-123");

VWOUserContext context2 = new VWOUserContext();
context2.setId("user_bob");
context2.setBucketingSeed("household-123");

vwoClient.getFlag("feature_key", context1, new IVwoListener() {
    public void onSuccess(Object data) {
        GetFlag flag = (GetFlag) data;
        // alice and bob will receive the same variation
        boolean isEnabled = flag != null && flag.isEnabled();
    }
    public void onFailure(String message) {}
});
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

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

</details>

### Basic Feature Flagging

Feature Flags serve as the foundation for all testing, personalization, and rollout rules within FME.
To implement a feature flag, first use the `getFlag` API to retrieve the flag configuration.
The `getFlag` API provides a simple way to check if a feature is enabled for a specific user and access its variables. It returns a feature flag object that contains methods for checking the feature's status and retrieving any associated variables.

| Parameter    | Description                                                      | Required | Type   | Example                                                                               |
| ------------ |------------------------------------------------------------------| -------- | ------ |---------------------------------------------------------------------------------------|
| `featureKey` | Unique identifier of the feature flag                            | Yes      | String | `'new_checkout'`                                                                      |
| `context`    | Object containing user identification and contextual information | Yes      | Object | `VWOUserContext()`                                                                    |
| `listener`   | Callback object to receive status update about the operation.    | Yes      | Object | see [Feature Flags & Variables](https://developers.vwo.com/v2/docs/fme-android-flags) |

Example usage:

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
vwoClient.getFlag("featureKey", context, object : IVwoListener {
    override fun onSuccess(data: Any) {
        val featureFlag = data as? GetFlag
        val isFeatureFlagEnabled = featureFlag?.isEnabled()
        val variable: String = featureFlag?.getVariable("feature_flag_variable", "default-value") as String
    }

    override fun onFailure(message: String) {
        // Feature flag is disabled
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
wingifyClient.getFlag("featureKey", context, object : IWingifyListener {
    override fun onSuccess(data: Any) {
        val featureFlag = data as? GetFlag
        val isFeatureFlagEnabled = featureFlag?.isEnabled()
        val variable: String = featureFlag?.getVariable("feature_flag_variable", "default-value") as String
    }

    override fun onFailure(message: String) {
        // Feature flag is disabled
    }
})
```

</details>

### Evaluating Multiple Feature Flags

To retrieve several feature flags at once, use the `getFlags` API.
The `getFlags` API evaluates multiple flags for a specific user in a single call. It returns a `FlagCollection` that you can use to check each flag's status and access its variables. Unlike `getFlag()`, this API is synchronous and does not use a listener.

Pass feature keys to evaluate a subset, or `null` / empty to evaluate every flag from settings.

| Parameter   | Description                                                                 | Required | Type            | Example                                     |
| ----------- | --------------------------------------------------------------------------- | -------- | --------------- | ------------------------------------------- |
| `flagNames` | Feature keys to evaluate. `null` or empty evaluates all flags from settings | No       | `Array<String>` | `arrayOf("feature_key_1", "feature_key_2")` |
| `context`   | Object containing user identification and contextual information            | Yes      | Object          | `VWOUserContext()`                          |

The returned `FlagCollection` provides:

| Method | Description |
| --- | --- |
| `get(featureKey)` | Returns the evaluated `GetFlag` for that key. A missing key returns a **disabled** flag (not null). |
| `keys()` | Feature keys present in the collection. |
| `size()` | Number of evaluated flags. |
| `iterator()` | Walks each feature-key → `GetFlag` entry. |

Each `GetFlag` is the same type as `getFlag()`: `isEnabled()`, `getVariable(key, default)`, `getVariables()`.

Example usage:

<details>
<summary>VWO (legacy)</summary>

```kotlin
val context = VWOUserContext()
context.id = "unique_user_id"

val flags = vwoClient.getFlags(arrayOf("feature_key_1", "feature_key_2"), context)
val featureFlag = flags.get("feature_key_1")
val isFeatureFlagEnabled = featureFlag.isEnabled()
val variable: String = featureFlag.getVariable("feature_flag_variable", "default-value") as String

// Evaluate every flag from settings
val allFlags = vwoClient.getFlags(null, context)
```

</details>

<details>
<summary>Wingify — recommended</summary>

```kotlin
val context = WingifyUserContext()
context.id = "unique_user_id"

val flags = wingifyClient.getFlags(arrayOf("feature_key_1", "feature_key_2"), context)
val featureFlag = flags.get("feature_key_1")
val isFeatureFlagEnabled = featureFlag.isEnabled()
val variable: String = featureFlag.getVariable("feature_flag_variable", "default-value") as String

// Evaluate every flag from settings
val allFlags = wingifyClient.getFlags(null, context)
```

</details>

### Custom Event Tracking

Feature flags can be enhanced with connected metrics to track key performance indicators (KPIs) for your features. These metrics help measure the effectiveness of your testing rules by comparing control versus variation performance, and evaluate the impact of personalization and rollout campaigns. Use the `trackEvent` API to track custom events like conversions, user interactions, and other important metrics:

| Parameter         | Description                                                            | Required | Type   | Example                                     |
| ----------------- | ---------------------------------------------------------------------- | -------- | ------ |---------------------------------------------|
| `eventName`       | Name of the event you want to track                                    | Yes      | String | `'purchase_completed'`                      |
| `context`         | Object containing user identification and other contextual information | Yes      | Object | `VWOUserContext()`                          |
| `eventProperties` | Additional properties/metadata associated with the event               | No       | Object | `mutableMapOf<String, Any>("amount" to 10)` |

Example usage:

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val context = VWOUserContext()
context.id = USER_ID
val properties = mutableMapOf<String, Any>("cartvalue" to 10)
vwoClient.trackEvent("vwoevent", context, properties)
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val context = WingifyUserContext()
context.id = USER_ID
val properties = mutableMapOf<String, Any>("cartvalue" to 10)
wingifyClient.trackEvent("vwoevent", context, properties)
```

</details>

See [Tracking Conversions](https://developers.vwo.com/v2/docs/fme-android-metrics#usage) documentation for more information.

### Pushing Attributes

User attributes provide rich contextual information about users, enabling powerful personalization. The `setAttribute` method provides a simple way to associate these attributes with users in VWO for advanced segmentation. Here's what you need to know about the method parameters:

| Parameter        | Description                                                            | Required | Type   | Example                 |
|------------------|------------------------------------------------------------------------| -------- |--------|-------------------------|
| `attributes`     | Map of attribute key and value to be set                               | Yes      | Object | `mapOf("price" to 99)`  |
| `context`        | Object containing user identification and other contextual information | Yes      | Object | `VWOUserContext()`      |

Example usage:

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val context = VWOUserContext()
context.id = USER_ID
val attributes = mapOf("price" to 99)
vwoClient.setAttribute(attributes, context)
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val context = WingifyUserContext()
context.id = USER_ID
val attributes = mapOf("price" to 99)
wingifyClient.setAttribute(attributes, context)
```

</details>

See [Pushing Attributes](https://developers.vwo.com/v2/docs/fme-android-attributes#usage) documentation for additional information.

### Polling Interval Adjustment

The `pollInterval` is an optional parameter that allows the SDK to automatically fetch and update settings from the VWO server at specified intervals. Setting this parameter ensures your application always uses the latest configuration.

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.pollInterval = 60000

// Create VWO instance with the vwoInitOptions
VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        this@MyActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        //Initialization failed
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.pollInterval = 60000

// Create Wingify instance with the initOptions
Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Initialization failed
    }
})
```

</details>

### Gateway

The VWO FME Gateway Service is an optional but powerful component that enhances VWO's Feature Management and Experimentation (FME) SDKs. It acts as a critical intermediary for pre-segmentation capabilities based on user location and user agent (UA). By deploying this service within your infrastructure, you benefit from minimal latency and strengthened security for all FME operations.

#### Why Use a Gateway?

The Gateway Service is required in the following scenarios:

- When using pre-segmentation features based on user location or user agent.
- For applications requiring advanced targeting capabilities.
- When using [user aliasing](#user-aliasing).

#### How to Use the Gateway

The gateway can be customized by passing the `gatewayService` parameter in the `init` configuration. At minimum, set `url`. You can also pass `protocol` and `port` if they are not already part of the URL.

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.gatewayService = mapOf("url" to "https://custom.gateway.com")

VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        this@MyActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        // Initialization failed
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.gatewayService = mapOf("url" to "https://custom.gateway.com")

Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Initialization failed
    }
})
```

</details>

Refer to the [Gateway Documentation](https://developers.vwo.com/v2/docs/gateway-service) for further details.

### User Aliasing

User aliasing lets you associate an existing user ID with an alternate ID (alias) so future evaluations and tracking use a unified identity across systems.

Requirements:

- Gateway must be configured.
- Aliasing must be enabled during initialization: `isAliasingEnabled = true`

Initialization example:

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.isAliasingEnabled = true
vwoInitOptions.gatewayService = mapOf("url" to "https://custom.gateway.com")

VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        this@MyActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        // Initialization failed
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.isAliasingEnabled = true
initOptions.gatewayService = mapOf("url" to "https://custom.gateway.com")

Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        this@MyActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Initialization failed
    }
})
```

</details>

Usage example:

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val context = VWOUserContext()
context.id = "user-123"
vwoClient.setAlias(context, "alias-abc")
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val context = WingifyUserContext()
context.id = "user-123"
wingifyClient.setAlias(context, "alias-abc")
```

</details>

Behavior and validations:

- Requires aliasing to be enabled and gateway configured; otherwise the call is a no-op and an error is logged
- `context.id` must be a non-empty user ID (or a device ID when `shouldUseDeviceIdAsUserId` is enabled)
- `aliasId` must be a non-empty string
- The request is sent asynchronously; `setAlias` does not return a success boolean

### Storage

The SDK operates in a stateless mode by default, meaning each `getFlag` call triggers a fresh evaluation of the flag against the current user context.

To optimize performance and maintain consistency SDK will use internal storage if application context is provided. You can implement a custom storage mechanism by passing a `storage` parameter during initialization. This allows you to persist feature flag decisions in your preferred data store.

Key benefits of implementing storage:

- Improved performance by caching decisions
- Consistent user experience across sessions
- Reduced load on your application

The storage mechanism ensures that once a decision is made for a user, it remains consistent even if campaign settings are modified in the VWO Application. This is particularly useful for maintaining a stable user experience during A/B tests and feature rollouts.

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
import com.vwo.packages.storage.Connector

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

val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.storage = StorageConnector()

VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        // Success
        this@MainActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        // Log error here
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
import com.wingify.packages.storage.Connector

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

val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.storage = StorageConnector()

Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        // Success
        this@MainActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Log error here
    }
})
```

</details>

### Logger

VWO by default logs all `ERROR` level messages to logcat. To gain more control over VWO's logging behaviour, you can use the `logger` parameter in the `init` configuration.

By default, messages are written to logcat with the tag `Vwo-fme-android` (or `Wingify-FME-Android` when initialized via Wingify) in the format `[LEVEL]: message`. If you set a custom `prefix`, messages appear as `[LEVEL]: prefix message`. Timestamps are not included in the message body because logcat adds them automatically.

| **Parameter** | **Description**                        | **Required** | **Type** | **Example**           |
|---------------| -------------------------------------- | ------------ | -------- | --------------------- |
| `level`       | Log level to control verbosity of logs | Yes          | String   | `DEBUG`               |
| `prefix`      | Custom prefix included in log messages | No           | String   | `MyCustomPrefix`      |
| `transports`  | Custom logger implementation           | No           | Object   | See example below     |

#### Example 1: Set log level to control verbosity of logs

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.logger = mutableMapOf<String, Any>().apply { put("level", "TRACE") }

VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        // Success
        this@MainActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        // Log error here
    }
})
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.logger = mutableMapOf<String, Any>().apply { put("level", "TRACE") }

Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        // Success
        this@MainActivity.wingifyClient = wingifyClient
    }

    override fun wingifyInitFailed(message: String) {
        // Log error here
    }
})
```

</details>

#### Example 2: Add a custom prefix to log messages

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.logger = mutableMapOf<String, Any>().apply {
    put("level", "INFO")
    put("prefix", "MyCustomPrefix")
}

VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        // Success
    }

    override fun vwoInitFailed(message: String) {
        // Log error here
    }
})
```

Logcat output:

```
I/Vwo-fme-android: [INFO]: MyCustomPrefix Settings fetched successfully
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.logger = mutableMapOf<String, Any>().apply {
    put("level", "INFO")
    put("prefix", "MyCustomPrefix")
}

Wingify.init(initOptions, object : IWingifyInitCallback {
    override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
        // Success
    }

    override fun wingifyInitFailed(message: String) {
        // Log error here
    }
})
```

Logcat output:

```
I/Wingify-FME-Android: [INFO]: MyCustomPrefix Settings fetched successfully
```

</details>

#### Example 3: Implement custom transport to handle logs your way

The `transports` parameter allows you to implement custom logging behavior by providing your own logging functions. You can define handlers for different log levels (TRACE, DEBUG, INFO, WARN, ERROR) to process log messages according to your needs.
For example, you could:

- Send logs to a third-party logging service
- Write logs to a file
- Format log messages differently
- Filter or transform log messages

The transport object should implement `defaultTransport` handler to customize. Custom transports receive the formatted message string (including level and optional prefix).

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
import android.util.Log
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum

val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
val logger: MutableList<Map<String, Any>> = mutableListOf()
val transport: MutableMap<String, Any> = mutableMapOf()
transport["defaultTransport"] = object : LogTransport {
    override fun log(level: LogLevelEnum, message: String?) {
        if (message == null) return
        Log.d("FME", message)
    }
}
logger.add(transport)
vwoInitOptions.logger = mutableMapOf<String, Any>().apply {
    put("level", "TRACE")
    put("transports", logger)
}
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
import android.util.Log
import com.wingify.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum

val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
val logger: MutableList<Map<String, Any>> = mutableListOf()
val transport: MutableMap<String, Any> = mutableMapOf()
transport["defaultTransport"] = object : LogTransport {
    override fun log(level: LogLevelEnum, message: String?) {
        if (message == null) return
        Log.d("FME", message)
    }
}
logger.add(transport)
initOptions.logger = mutableMapOf<String, Any>().apply {
    put("level", "TRACE")
    put("transports", logger)
}
```

</details>

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

VWO FME SDK provides integration capabilities with analytics platforms like Mixpanel. This allows you to track feature flag evaluations and events in your analytics dashboard.

### Kotlin Implementation

#### 1. Create a MixpanelIntegration class

```kotlin
import android.content.Context
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
```

#### 2. Initialize Mixpanel and set up integration callback

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
import com.vwo.interfaces.integration.IntegrationCallback
import com.vwo.models.user.VWOInitOptions

val mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN
mixpanelIntegration = MixpanelIntegration.getInstance(context, mixpanelToken)

val vwoInitOptions = VWOInitOptions()
vwoInitOptions.integrations = object : IntegrationCallback {
    override fun execute(properties: Map<String, Any>) {
        when (properties["api"]) {
            "trackEvent" -> {
                val eventName = properties["eventName"] as String
                mixpanelIntegration?.trackEvent(eventName, properties)
            }
            "getFlag" -> mixpanelIntegration?.trackFlagEvaluation(properties)
        }
    }
}
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
import com.wingify.interfaces.integration.IntegrationCallback
import com.wingify.models.user.WingifyInitOptions

val mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN
mixpanelIntegration = MixpanelIntegration.getInstance(context, mixpanelToken)

val initOptions = WingifyInitOptions()
initOptions.integrations = object : IntegrationCallback {
    override fun execute(properties: Map<String, Any>) {
        when (properties["api"]) {
            "trackEvent" -> {
                val eventName = properties["eventName"] as String
                mixpanelIntegration?.trackEvent(eventName, properties)
            }
            "getFlag" -> mixpanelIntegration?.trackFlagEvaluation(properties)
        }
    }
}
```

</details>

### Java Implementation

#### 1. Create a MixpanelIntegration class

```java
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
```

#### 2. Initialize Mixpanel and set up integration callback

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```java
import com.vwo.interfaces.integration.IntegrationCallback;
import com.vwo.models.user.VWOInitOptions;

String mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN;
final MixpanelIntegration mixpanelIntegration = MixpanelIntegration.getInstance(context, mixpanelToken);

VWOInitOptions initOptions = new VWOInitOptions();
initOptions.setIntegrations(new IntegrationCallback() {
    @Override
    public void execute(Map<String, Object> properties) {
        Object api = properties.get("api");
        if ("trackEvent".equals(api)) {
            String eventName = (String) properties.get("eventName");
            mixpanelIntegration.trackEvent(eventName, properties);
        } else if ("getFlag".equals(api)) {
            mixpanelIntegration.trackFlagEvaluation(properties);
        }
    }
});
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```java
import com.wingify.interfaces.integration.IntegrationCallback;
import com.wingify.models.user.WingifyInitOptions;

String mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN;
final MixpanelIntegration mixpanelIntegration = MixpanelIntegration.getInstance(context, mixpanelToken);

WingifyInitOptions initOptions = new WingifyInitOptions();
initOptions.setIntegrations(new IntegrationCallback() {
    @Override
    public void execute(Map<String, Object> properties) {
        Object api = properties.get("api");
        if ("trackEvent".equals(api)) {
            String eventName = (String) properties.get("eventName");
            mixpanelIntegration.trackEvent(eventName, properties);
        } else if ("getFlag".equals(api)) {
            mixpanelIntegration.trackFlagEvaluation(properties);
        }
    }
});
```

</details>

### Always-On Integration Callbacks

By default, the integration callback is only invoked when a full live evaluation takes place inside `getFlag()`. Enable `shouldTriggerIntegrationCallbackAlways` in `WingifyInitOptions` (or legacy `VWOInitOptions`) to receive the callback on **every** `getFlag()` call, including:

- Users served from **local storage** (previously stored decisions).
- Users placed into a **holdout group**.
- Calls where the **feature is not found** in the settings.

This ensures your analytics pipeline captures a complete picture of all flag evaluations, not just live ones.

If SDK version less than 1.50.0, use the VWO snippet. If SDK version 1.50.0 or later, we recommend switching to the Wingify snippet.

<details>
<summary>VWO (SDK version &lt; 1.50.0)</summary>

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.shouldTriggerIntegrationCallbackAlways = true

vwoInitOptions.integrations = object : IntegrationCallback {
    override fun execute(properties: Map<String, Any>) {
        // Called for every getFlag() evaluation
        mixpanelIntegration?.trackFlagEvaluation(properties)
    }
}
```

```java
VWOInitOptions vwoInitOptions = new VWOInitOptions();
vwoInitOptions.setSdkKey(SDK_KEY);
vwoInitOptions.setAccountId(ACCOUNT_ID);
vwoInitOptions.setShouldTriggerIntegrationCallbackAlways(true);

vwoInitOptions.setIntegrations(new IntegrationCallback() {
    @Override
    public void execute(Map<String, Object> properties) {
        // Called for every getFlag() evaluation
        mixpanelIntegration.trackFlagEvaluation(properties);
    }
});
```

</details>

<details>
<summary>Wingify (SDK version &gt;= 1.50.0) — recommended</summary>

```kotlin
val initOptions = WingifyInitOptions()
initOptions.sdkKey = SDK_KEY
initOptions.accountId = ACCOUNT_ID
initOptions.shouldTriggerIntegrationCallbackAlways = true

initOptions.integrations = object : IntegrationCallback {
    override fun execute(properties: Map<String, Any>) {
        // Called for every getFlag() evaluation
        mixpanelIntegration?.trackFlagEvaluation(properties)
    }
}
```

```java
WingifyInitOptions initOptions = new WingifyInitOptions();
initOptions.setSdkKey(SDK_KEY);
initOptions.setAccountId(ACCOUNT_ID);
initOptions.setShouldTriggerIntegrationCallbackAlways(true);

initOptions.setIntegrations(new IntegrationCallback() {
    @Override
    public void execute(Map<String, Object> properties) {
        // Called for every getFlag() evaluation
        mixpanelIntegration.trackFlagEvaluation(properties);
    }
});
```

</details>

### Integration Data

When using the integration callback, you'll receive the following data:

- **For flag evaluations**:
  ```
  {
    featureName: "yourFlagName",
    featureId: 5,
    featureKey: "yourFlagKey",
    userId: "0duMh1j7krRB",
    api: "getFlag",
    isPartOfHoldout: true/false,
    rolloutId: "<id or empty string>",
    rolloutKey: "<key or empty string>",
    rolloutVariationId: "<id or empty string>",
    experimentId: "<id or empty string>",
    experimentKey: "<key or empty string>",
    experimentVariationId: "<id or empty string>",
    customVariables: { ... },
    variationTargetingVariables: { ... },
    ...
  }
  ```

  > **Note:** When `shouldTriggerIntegrationCallbackAlways` is enabled, campaign keys (`rolloutId`, `rolloutKey`, `rolloutVariationId`, `experimentId`, `experimentKey`, `experimentVariationId`) are always present. Keys that are not applicable for a given path are set to an empty string.

- **For event tracking**:
  ```
  {
    eventName: "yourEventName",
    api: "trackEvent"
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
