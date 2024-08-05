# VWO FME Android SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Requirements

The Android SDK supports: `Android API level 21 onwards`

## SDK Installation

Add below Maven dependency in your project.

```java
implementation 'com.vwo.sdk:vwo-fme-android-sdk:<latestVersion>'
```

Latest version of SDK can be found in [Maven repository](https://mvnrepository.com/artifact/com.vwo.sdk/vwo-fme-android-sdk)

## Basic Usage

```kotlin
import com.vwo.VWO
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOContext
import com.vwo.models.user.VWOInitOptions

// Initialize VWO SDK
val vwoInitOptions = VWOInitOptions()
// Set SDK Key and Account ID
vwoInitOptions.sdkKey = SDK_KEY
vwoInitOptions.accountId = ACCOUNT_ID

// Create VWO instance with the vwoInitOptions
VWO.init(vwoInitOptions, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwo: VWO, message: String) {
        Log.d("Vwo", "vwoInitSuccess $message")

        // Create VWOContext object
        var userContext = VWOContext()
        // Set User ID
        userContext.id = "unique_user_id"
        userContext.customVariables = mutableMapOf("key1" to 21, "key2" to 0)

        // Get the GetFlag object for the feature key and context
        featureFlag = vwo.getFlag("feature_flag_name", userContext)
        // Get the flag value
        val isFeatureFlagEnabled = featureFlag?.isEnabled

        // Get the variable value for the given variable key and default value
        val variable1 = featureFlag.getVariable("feature_flag_variable1", "default-value1")

        // Track the event for the given event name and context
        val properties = mutableMapOf<String, Any>("cartvalue" to 10)
        vwo.trackEvent("vwoevent", userContext, properties)

        // send attributes data
        vwo.setAttribute("attribute-name", "attribute-value1", userContext)
    }

    override fun vwoInitFailed(message: String) {
        Log.d("Vwo", "vwoInitFailed: $message")
    }
})
```

## Authors

* [Swapnil Chaudhari](https://github.com/swapnilWingify)

## Changelog

Refer [CHANGELOG.md](https://github.com/wingify/vwo-fme-android-sdk/blob/master/CHANGELOG.md)

## Contributing

Please go through our [contributing guidelines](https://github.com/wingify/vwo-fme-android-sdk/blob/master/CONTRIBUTING.md)

## Code of Conduct

[Code of Conduct](https://github.com/wingify/vwo-fme-android-sdk/blob/master/CODE_OF_CONDUCT.md)

## License

[Apache License, Version 2.0](https://github.com/wingify/vwo-fme-android-sdk/blob/master/LICENSE)

Copyright 2024 Wingify Software Pvt. Ltd.
