# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

[1.4.0] - 2025-03-11

### Added

- Added support to use DACDN as Gateway substitute
- Added delay while retrying failed API calls

[1.3.1] - 2025-03-06

### Added

- Added support for error message uploading

[1.3.0] - 2025-02-14

### Added

- Added support for FME-MI sdk linking
- added support to use salt for bucketing if provided in the rule.

### Changed

- Changed setAttribute to send map instead of key-value pair

## [1.2.0] - 2025-01-30

### Added

- Batch upload support added for optimized data transfer.

## [1.1.1] - 2025-01-10

### Changed

- Addressed security vulnerabilities by updating dependencies.

## [1.1.0] - 2025-01-03

### Added

- The SDK now supports storing impression events while the device is offline, ensuring no data loss. These events are batched and seamlessly synchronized with VWO servers once the device reconnects to the internet.

### Changed

- Optimized the MEG rules engine for faster processing.

## [1.0.1] - 2024-12-19

### Added

- Added support for changing SDK name and SDK version in hybrid(React-native and Flutter) SDKs.

## [1.0.0] - 2024-11-11

### Added

- Added support for Personalise rules within `Mutually Exclusive Groups`.
- Settings cache: Cached settings will be used till it expires. Client can set the expiry time of cache.
- Storage support: Built-in local storage will be used by default if client doesn't provide their own. Client’s storage will be used if it is provided.
- Call backs added to avoid busy waiting for server call to complete.
- Changed variable access to method access for Flag - setIsEnabled & isEnabled

## [0.1.0] - 2024-07-31

### Added

- First release of VWO Feature Management and Experimentation capabilities.

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

- **Error handling**

  - Gracefully handle any kind of error - TypeError, NetworkError, etc.
