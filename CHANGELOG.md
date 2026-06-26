# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.55.0] - 2026-06-09

### Added

- Added user tracking support: sends a `vwo_feTrackUsage` event when user tracking is enabled for the account and no variation-shown impression was dispatched for the evaluation.

## [1.50.1] - 2026-06-09

### Fixed

- Updated proguard rules for Hybrid SDKs

## [1.50.0] - 2026-05-29

This release introduces Wingify as the primary SDK branding and package namespace, while keeping existing VWO integrations fully supported.

Both Maven coordinates ship the **same library binary** at the same version — pick one; do not add both:

| Coordinate | Public API |
| --- | --- |
| `com.wingify.sdk:wingify-fme-android-sdk` | `com.wingify.*` (recommended for new apps) |
| `com.vwo.sdk:vwo-fme-android-sdk` | `com.vwo.*` (legacy, deprecated) |

For a full migration guide, see [MIGRATE.md](MIGRATE.md).

### Added

- **Wingify public API** — use `Wingify`, `WingifyInitOptions`, and `WingifyUserContext` from the `com.wingify` package as the recommended entry point for new integrations.
- **Wingify Maven coordinate** — `com.wingify.sdk:wingify-fme-android-sdk` publishes the same AAR as the legacy VWO artifact.

  ```kotlin
  // Kotlin
  import com.wingify.Wingify
  import com.wingify.Wingify.init
  import com.wingify.interfaces.IWingifyInitCallback
  import com.wingify.models.user.WingifyInitOptions
  import com.wingify.models.user.WingifyUserContext
  import com.wingify.models.user.GetFlag

  val options = WingifyInitOptions()
  options.accountId = ""
  options.sdkKey = ""

  init(options, object : IWingifyInitCallback {
      override fun wingifyInitSuccess(wingifyClient: Wingify, message: String) {
          val context = WingifyUserContext()
          context.id = "user-123"

          val flag: GetFlag = wingifyClient.getFlag("feature-key", context)
      }

      override fun wingifyInitFailed(message: String) {
          // Initialization failed
      }
  })
  ```

  ```java
  // Java
  import com.wingify.Wingify;
  import com.wingify.Wingify.init;
  import com.wingify.interfaces.IWingifyInitCallback;
  import com.wingify.models.user.GetFlag;
  import com.wingify.models.user.WingifyInitOptions;
  import com.wingify.models.user.WingifyUserContext;

  WingifyInitOptions options = new WingifyInitOptions();
  options.setAccountId(123456);
  options.setSdkKey("");

  init(options, new IWingifyInitCallback() {
      @Override
      public void wingifyInitSuccess(@NonNull Wingify wingifyClient, @NonNull String message) {
          WingifyUserContext context = new WingifyUserContext();
          context.setId("user-123");

          GetFlag flag = wingifyClient.getFlag("feature-key", context);
      }

      @Override
      public void wingifyInitFailed(@NonNull String message) {
          // Initialization failed
      }
  });
  ```

### Changed

- The SDK implementation now lives under the `com.wingify` package.
- Log messages and documentation have been updated to reflect Wingify branding.
- When initialized through `Wingify`, settings and events use Wingify hosts (`edge.wingify.net`, `collect.wingify.net`) and the Logcat tag `Wingify-FME-Android`.
- No breaking changes for existing integrations — server event names, payload keys, and runtime behavior remain compatible with the VWO platform.

### Deprecated

The following VWO classes in `com.vwo` are deprecated but continue to work without modification:

| Deprecated (still supported) | Use instead |
| --- | --- |
| `com.vwo.VWO` | `com.wingify.Wingify` |
| `com.vwo.models.user.VWOInitOptions` | `com.wingify.models.user.WingifyInitOptions` |
| `com.vwo.models.user.VWOUserContext` | `com.wingify.models.user.WingifyUserContext` |
| `com.vwo.interfaces.IVwoInitCallback` | `com.wingify.interfaces.IWingifyInitCallback` |
| `com.vwo.interfaces.IVwoListener` | `com.wingify.interfaces.IWingifyListener` |
| `com.vwo.interfaces.logger.LogTransport` | `com.wingify.interfaces.logger.LogTransport` |
| `com.vwo.interfaces.integration.IntegrationCallback` | `com.wingify.interfaces.integration.IntegrationCallback` |
| `com.vwo.packages.storage.Connector` | `com.wingify.packages.storage.Connector` |

Existing code does not need to change immediately. We recommend adopting the Wingify API for new projects and migrating when convenient:

```kotlin
// Still supported — no action required today
import com.vwo.VWO
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext

val options = VWOInitOptions()
options.accountId = 123456
options.sdkKey = "32-alpha-numeric-sdk-key"

VWO.init(options, object : IVwoInitCallback {
    override fun vwoInitSuccess(vwo: VWO, message: String) {
        val context = VWOUserContext()
        context.id = "user-123"

        vwo.getFlag("feature-key", context)
    }

    override fun vwoInitFailed(message: String) {
        // Initialization failed
    }
})
```

**Migration tip:** Replace `VWO` → `Wingify`, `VWOInitOptions` → `WingifyInitOptions`, `VWOUserContext` → `WingifyUserContext`, init callbacks to `wingifyInitSuccess` / `wingifyInitFailed`, and update imports from `com.vwo.*` to `com.wingify.*`. Method signatures and SDK behavior are unchanged.

## [1.14.0] - 2026-04-18

### Fixed

- Improve settings parsing ability, don't store invalid setting received from server.
- Bugfix: `batch-events-v2` network calls not respecting the passed `gateway` url.
- Platform type not being sent in network calls.

## [1.13.1] - 2026-04-15

### Fixed

- Decision expiry time not being set when configured in init options.

## [1.13.0] - 2026-03-11

### Added

- Added support for custom bucketing logic to ensure consistent decisioning across sessions.

  Set `bucketingSeed` on `VWOUserContext` to ensure different users sharing the same seed always receive the same variation.

  ```kotlin
  // Kotlin — household-level consistency
  val context1 = VWOUserContext().apply {
      id = "user_alice"
      bucketingSeed = "household-123"
  }
  val context2 = VWOUserContext().apply {
      id = "user_bob"
      bucketingSeed = "household-123"
  }
  // alice and bob will receive the same variation
  ```

  ```java
  // Java — household-level consistency
  VWOUserContext context1 = new VWOUserContext();
  context1.setId("user_alice");
  context1.setBucketingSeed("household-123");

  VWOUserContext context2 = new VWOUserContext();
  context2.setId("user_bob");
  context2.setBucketingSeed("household-123");
  // alice and bob will receive the same variation
  ```

## [1.12.0] - 2026-03-04

### Added

- Introduced holdout group support for feature flags and events, including holdout settings in the configuration, SDK-side holdout evaluation, and exclusion of users in holdout groups from feature rollouts and experiments.

## [1.11.4] - 2026-02-27

### Fixed

- Improved decision caching logic to ensure rollout expirations are respected in single-rollout configurations.

## [1.11.3] - 2026-02-25

### Added

- Decision Expiry Support: Introduced `cachedDecisionExpiryTime` in `VWOInitOptions` to allow time-based invalidation of stored decisions.

## [1.11.2] - 2026-02-23

### Added

- Fetch settings parallelly even when cache is not expired
- Fixed a bug that prevented Hybrid SDKs to send initialization event

## [1.11.1] - 2026-02-09

### Fixed

- Fixed an issue where the VWO.init() method was stripped or missing from production builds.

## [1.11.0] - 2026-01-29

### Added

- Multi-instance / multi-account for different credentials within a single application

## [1.10.4] - 2026-01-15

### Changed

- When invalid credentials entered, do not retry API calls

## [1.10.3] - 2025-11-27

### Fixed

- Fixed issue where `getVariable()` and `getVariables()` returned fallback/empty values when
  fetching user data from storage for rollout rule.

## [1.10.2] - 2025-11-27

### Added

- Enhanced Logging capabilities at VWO by sending `vwo_sdkDebug` event with additional debug
  properties.

## [1.10.1] - 2025-10-28

### Changed

- Commit reverted for v1.10.0(logging capabilities)

## [1.9.2] - 2025-10-01

### Added

- Bugfix for user count not increasing on dashboard

## [1.9.1] - 2025-09-20

### Added

- Bugfix and handling crashes when `VWO.init` is called multiple times

## [1.9.0] - 2025-09-10

### Added

- Support to use Device ID if `context.id` is not available

```java
// Java
VWOUserContext context = new VWOUserContext();
context.

setId(""); // Empty ID to trigger device ID fallback
context.

setShouldUseDeviceIdAsUserId(true); // Use device ID as user ID
```

- Add support for user aliasing (will work after gateway has been setup)

```java
// Java
VWOInitOptions options = new VWOInitOptions();
options.isAliasingEnabled =true;

VWOUserContext context = new VWOUserContext();
context.id ="temp_id";
        VWO.

setAlias(context, "user_alias");
```

## [1.8.0] - 2025-08-29

### Added

- Attribute Support: Use custom variables in pre/post-segmentation.

### Changed

- Updated SDK's usage data upload logic to aggregate in single account

[1.7.0] - 2025-08-05

- Added support for sending a one-time SDK initialization event to VWO server as part of
  health-check milestones.

## [1.6.4] - 2025-07-25

### Added

- Send the SDK name and version in the events and batching call to VWO as query parameters.

## [1.6.3] - 2025-06-24

### Added

- Send the SDK name and version in the settings call to VWO as query parameters.

## [1.6.2] - 2025-06-20

### Changed

- Optimized application size by 61% via dependency minimization
- Gradle updated for Maven Central publishing

## [1.6.1] - 2025-05-28

### Changed

- Refactored code to enhance testability

## [1.6.0] - 2025-05-07

### Added

- Added the ability to collect usage data to help guide future enhancements and debugging.

### Changed

- Improved the README for better clarity.
- Code refactored for syntax consistency with other SDKs.

### Fixed

- Resolved an issue where error logs were generated when no flags were present in settings.
- Corrected an error that occurred when retrieving flag values during polling.

## [1.4.1] - 2025-04-02

### Changed

- Increased retry attempts and enhanced retry logging

### Fixed

- ProGuard configuration updated for storage connector

## [1.4.0] - 2025-03-11

### Added

- Added support to use DACDN as Gateway substitute
- Added delay while retrying failed API calls

## [1.3.1] - 2025-03-06

### Added

- Added support for error message uploading

## [1.3.0] - 2025-02-14

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

- The SDK now supports storing impression events while the device is offline, ensuring no data loss.
  These events are batched and seamlessly synchronized with VWO servers once the device reconnects
  to the internet.

### Changed

- Optimized the MEG rules engine for faster processing.

## [1.0.1] - 2024-12-19

### Added

- Added support for changing SDK name and SDK version in hybrid(React-native and Flutter) SDKs.

## [1.0.0] - 2024-11-11

### Added

- Added support for Personalise rules within `Mutually Exclusive Groups`.
- Settings cache: Cached settings will be used till it expires. Client can set the expiry time of
  cache.
- Storage support: Built-in local storage will be used by default if client doesn't provide their
  own. Client’s storage will be used if it is provided.
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
