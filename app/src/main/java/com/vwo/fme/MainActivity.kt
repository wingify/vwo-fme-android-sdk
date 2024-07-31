package com.vwo.fme

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vwo.VWO
import com.vwo.fme.databinding.ActivityMainBinding
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOContext
import com.vwo.models.user.VWOInitOptions


private const val SDK_KEY = ""
private const val ACCOUNT_ID = 0

class MainActivity : AppCompatActivity() {

    private var vwo: VWO? = null
    private var featureFlag: GetFlag? = null
    private lateinit var userContext: VWOContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnInitSdk.setOnClickListener {
            // Initialize VWO SDK
            val vwoInitOptions = VWOInitOptions()
            // Set SDK Key and Account ID
            vwoInitOptions.sdkKey = SDK_KEY
            vwoInitOptions.accountId = ACCOUNT_ID

            vwoInitOptions.logger = mutableMapOf<String, Any>().apply { put("level", "TRACE") }
            // Create VWO instance with the vwoInitOptions
            VWO.init(vwoInitOptions, object : IVwoInitCallback {
                override fun vwoInitSuccess(vwo: VWO, message: String) {
                    Log.d("Vwo", "vwoInitSuccess $message")
                    this@MainActivity.vwo = vwo
                }

                override fun vwoInitFailed(message: String) {
                    Log.d("Vwo", "vwoInitFailed: $message")
                }
            })
            binding.btnGetFlag.setOnClickListener {
                vwo?.let { getFlag(it) }
            }
            binding.btnGetVariable.setOnClickListener {
                featureFlag?.let { getVariable(it) }
            }
            binding.btnTrack.setOnClickListener {
                track()
            }
            binding.btnAttribute.setOnClickListener {
                sendAttribute()
            }
        }
    }

    private fun getFlag(vwo: VWO) {
        userContext = VWOContext()
        userContext.id = "unique_user_id"
        userContext.customVariables = mutableMapOf("name1" to 21,"name2" to 0,"name3" to 5,"name4" to 11)

        // Get feature flag object
        featureFlag = vwo.getFlag("feature_flag_name", userContext)

        val isFeatureFlagEnabled = featureFlag?.isEnabled
        Log.d("Vwo", "isFeatureFlagEnabled=$isFeatureFlagEnabled")
    }

    private fun getVariable(featureFlag: GetFlag) {
        val isFeatureFlagEnabled = featureFlag.isEnabled
        Log.d("Vwo", "isFeatureFlagEnabled=$isFeatureFlagEnabled")

        // Determine the application flow based on feature flag status
        if (isFeatureFlagEnabled) {
            // To get value of a single variable
            val variable1 = featureFlag.getVariable("feature_flag_variable1", "default-value1")
            val variable2 = featureFlag.getVariable("feature_flag_variable2", "default-value2")

            // To get value of all variables in object format
            val getAllVariables = featureFlag.getVariables()
            Log.d("Vwo", "variable1=$variable1 variable2=$variable2 getAllVariables=$getAllVariables")
        } else {
            // Your code when feature flag is disabled
            Log.d("Vwo", "Feature flag is disabled: ${featureFlag.isEnabled} " +
                        "${featureFlag.getVariables()}")
        }
    }

    private fun track() {

        if (!::userContext.isInitialized) return

        val properties = mutableMapOf<String, Any>("cartvalue" to 10)
        // Track the event for the given event name and user context
        val trackResponse = vwo?.trackEvent("vwoevent", userContext, properties)
        //val trackResponse = vwo?.trackEvent("vwoevent", userContext)
        Log.d("Vwo", "track=$trackResponse")
    }

    private fun sendAttribute() {
        if (!::userContext.isInitialized) return

        vwo?.setAttribute("attribute-name", "attribute-value1", userContext)
        vwo?.setAttribute("attribute-name-float", 1.01, userContext)
        vwo?.setAttribute("attribute-name-boolean", true, userContext)
    }
}