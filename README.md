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

Add below Maven dependency in your project's `build.gradle` file.

```groovy
implementation 'com.vwo.sdk:vwo-fme-android-sdk:<latestVersion>'
```

Latest version of SDK can be found in [Maven repository](https://mvnrepository.com/artifact/com.vwo.sdk/vwo-fme-android-sdk)

## Basic Usage
The following example demonstrates initializing the SDK with a VWO account ID and SDK key, setting a user context, checking if a feature flag is enabled, and tracking a custom event.

Kotlin usage
```kotlin
import com.vwo.VWO
import com.vwo.VWO.init
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOUserContext
import com.vwo.models.user.VWOInitOptions

// Initialize VWO SDK
val vwoInitOptions = VWOInitOptions()
// Set SDK Key and Account ID
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID

// Create VWO instance with the vwoInitOptions
init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        this@MyActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        //Initialization failed
    }
})

// Create VWOUserContext object
var context = VWOUserContext()
// Set User ID
context.id = "unique_user_id"
context.customVariables = mutableMapOf("key1" to 21, "key2" to 0)

// Get the GetFlag object for the feature key and context
vwoClient.getFlag("feature_key", context, object : IVwoListener {
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
vwoClient?.trackEvent("vwoevent", context, properties)

// send attributes data
val attributes = mapOf(
    "attributeName" to "attributeValue"
)
vwoClient?.setAttribute(attributes, context)
```

Java usage
```java
import com.vwo.VWO;
import com.vwo.VWO.init;
import com.vwo.interfaces.IVwoInitCallback;
import com.vwo.interfaces.IVwoListener;
import com.vwo.models.user.GetFlag;
import com.vwo.models.user.VWOUserContext;
import com.vwo.models.user.VWOInitOptions;

// Initialize VWO SDK
VWOInitOptions vwoInitOptions = new VWOInitOptions();
// Set SDK Key and Account ID
vwoInitOptions.setSdkKey(SDK_KEY);
vwoInitOptions.setAccountId(ACCOUNT_ID);

// Create VWO instance with the vwoInitOptions
init(vwoInitOptions, new IVwoInitCallback() {
    @Override
    public void vwoInitSuccess(@NonNull VWO vwoClient, @NonNull String message) {
        MyActivity.this.vwoClient = vwoClient;
    }

    @Override
    public void vwoInitFailed(@NonNull String message) {
        //Initialization failed
    }
});

// Create VWOUserContext object
VWOUserContext context = new VWOUserContext();
context.setId("unique_user_id");

Map<String, Object> customVariables = new HashMap<>();
customVariables.put("variable", "variable-value");
context.setCustomVariables(customVariables);

// Get the GetFlag object for the feature key and context
vwoClient.getFlag("feature-key", context, new IVwoListener() {
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
vwoClient.trackEvent("eventName", context, properties);

// Send attributes data
HashMap<String, Object> attributes = new HashMap<>();
attributes.put("attribute_key", "attribute_value");
vwoClient.setAttribute(attributes, context);
```

## Advanced Configuration Options

To customize the SDK further, additional parameters can be passed to the `init()` API. Here’s a table describing each option:

| **Parameter**              | **Description**                                                                                                                                             | **Required** | **Type** | **Example**                     |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------| ------------ |----------|---------------------------------|
| `accountId`                | VWO Account ID for authentication.                                                                                                                          | Yes          | Integer  | `123456`                        |
| `sdkKey`                   | SDK key corresponding to the specific environment to initialize the VWO SDK Client. You can get this key from VWO Application.                              | Yes          | String   | `'32-alpha-numeric-sdk-key'`    |
| `pollInterval`             | Time interval for fetching updates from VWO servers (in milliseconds).                                                                                      | No           | Integer   | `60000`                         |
| `storage`                  | Custom storage connector for persisting user decisions and campaign data.                                                                                   | No           | Object   | See [Storage](#storage) section |
| `logger`                   | Toggle log levels for more insights or for debugging purposes. You can also customize your own transport in order to have better control over log messages. | No           | Object   | See [Logger](#logger) section   |
| `cachedSettingsExpiryTime` | Controls the duration (in milliseconds) the SDK uses cached settings before fetching new ones.                                                              | No           | Integer  | `60000`                         |
| `batchMinSize`             | Uploads are triggered when the batch reaches this minimum size.                                                                                             | No           | Integer  | `10`                            |
| `batchUploadTimeInterval`  | Specifies the time interval (in milliseconds) for periodic batch uploads.                                                                                   | No           | Integer  | `60000`                         |

Refer to the [official VWO documentation](https://developers.vwo.com/v2/docs/fme-android-install) for additional parameter details.

### User Context

The `context` object uniquely identifies users and is crucial for consistent feature rollouts. A typical `context` includes an `id` for identifying the user. It can also include other attributes that can be used for targeting and segmentation, such as `customVariables`.

#### Parameters Table

The following table explains all the parameters in the `context` object:

| **Parameter**     | **Description**                                                            | **Required** | **Type** | **Example**                      |
| ----------------- | -------------------------------------------------------------------------- | ------------ | -------- | -------------------------------- |
| `id`              | Unique identifier for the user.                                            | Yes          | String   | `'unique_user_id'`               |
| `customVariables` | Custom attributes for targeting.                                           | No           | Object   | `mutableMapOf("age" to 25))`     |
| `enableDeviceId`  | Enable device ID generation when user ID is not provided.                  | No           | Boolean  | `true`                           |

#### Example

```kotlin
val context = VWOUserContext()
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

To enable device ID generation, set the `enableDeviceId` property in your `VWOUserContext`:

```kotlin
// Kotlin
val userContext = VWOUserContext()
userContext.id = "" // Empty ID to trigger device ID fallback
userContext.enableDeviceId = true // Enable device ID generation
```

```java
// Java
VWOUserContext context = new VWOUserContext();
context.setId(""); // Empty ID to trigger device ID fallback
context.setEnableDeviceId(true); // Enable device ID generation
```

##### How It Works

- User ID Priority: If a user ID is provided, it takes precedence over device ID
- Device ID Fallback: When no user ID is available and `enableDeviceId` is enabled, the SDK generates a persistent device ID
- Privacy-Friendly: Device IDs are hashed using SHA-256 for enhanced privacy protection
- Persistent: Device IDs remain consistent across app uninstalls/reinstalls but may change on factory resets

##### Usage Example

```kotlin
// Create user context with device ID enabled
val userContext = VWOUserContext()
userContext.enableDeviceId = true
// Leave userContext.id empty or null to use device ID

// Use the context for feature flags
vwoClient.getFlag("feature_key", userContext, object : IVwoListener {
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

```kotlin
vwoClient.getFlag("featureKey", context, object : IVwoListener {
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
| `context`         | Object containing user identification and other contextual information | Yes      | Object | `VWOUserContext()`                          |
| `eventProperties` | Additional properties/metadata associated with the event               | No       | Object | `mutableMapOf<String, Any>("amount" to 10)` |

Example usage:

```kotlin
val context = VWOUserContext()
context.id = USER_ID
val properties = mutableMapOf<String, Any>("cartvalue" to 10)
vwoClient?.trackEvent("vwoevent", context, properties)
```

See [Tracking Conversions](https://developers.vwo.com/v2/docs/fme-android-metrics#usage) documentation for more information.

### Pushing Attributes

User attributes provide rich contextual information about users, enabling powerful personalization. The `setAttribute` method provides a simple way to associate these attributes with users in VWO for advanced segmentation. Here's what you need to know about the method parameters:

| Parameter        | Description                                                            | Required | Type   | Example                 |
|------------------|------------------------------------------------------------------------| -------- |--------|-------------------------|
| `attributes`     | Map of attribute key and value to be set                               | Yes      | Object | `mapOf("price" to 99)`  |
| `context`        | Object containing user identification and other contextual information | Yes      | Object | `VWOUserContext()`      |

Example usage:

```kotlin
val context = VWOUserContext()
context.id = USER_ID
val attributes = mapOf("price" to 99)
vwoClient?.setAttribute(attributes, context)
```

See [Pushing Attributes](https://developers.vwo.com/v2/docs/fme-android-attributes#usage) documentation for additional information.

### Polling Interval Adjustment

The `pollInterval` is an optional parameter that allows the SDK to automatically fetch and update settings from the VWO server at specified intervals. Setting this parameter ensures your application always uses the latest configuration.

```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.pollInterval = 60000

// Create VWO instance with the vwoInitOptions
init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        this@MyActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
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

The storage mechanism ensures that once a decision is made for a user, it remains consistent even if campaign settings are modified in the VWO Application. This is particularly useful for maintaining a stable user experience during A/B tests and feature rollouts.

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

val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.storage = StorageConnector()

init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        // Success
        this@MainActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
        // Log error here
    }
})
```

### Logger

VWO by default logs all `ERROR` level messages to logcat. To gain more control over VWO's logging behaviour, you can use the `logger` parameter in the `init` configuration.

| **Parameter** | **Description**                        | **Required** | **Type** | **Example**           |
| ------------- | -------------------------------------- | ------------ | -------- | --------------------- |
| `level`       | Log level to control verbosity of logs | Yes          | String   | `DEBUG`               |
| `transport`   | Custom logger implementation           | No           | Object   | See example below     |

#### Example 1: Set log level to control verbosity of logs
```kotlin
val vwoInitOptions = VWOInitOptions()
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID
vwoInitOptions.logger = mutableMapOf<String, Any>().apply { put("level", "TRACE") }

init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwoClient: VWO, message: String) {
        // Success
        this@MainActivity.vwoClient = vwoClient
    }

    override fun vwoInitFailed(message: String) {
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

VWOInitOptions initOptions = new VWOInitOptions();
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

Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
