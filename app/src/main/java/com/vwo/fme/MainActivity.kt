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
            // create VWO instance with the vwoInitOptions
            VWO.init(vwoInitOptions, object : IVwoInitCallback {
                override fun vwoInitSuccess(vwo: VWO, message: String) {
                    Log.d("SwapnilFlag", "vwoInitSuccess $message")
                    this@MainActivity.vwo = vwo
                }

                override fun vwoInitFailed(message: String) {
                    Log.d("SwapnilFlag", "vwoInitFailed: $message")
                }
            })
            binding.btnGetFlag.setOnClickListener {
                vwo?.let { getFlag(it) }
            }
            binding.btnGetVariable.setOnClickListener {
                featureFlag?.let { getVariable(it) }
            }

            /*val calculator = Calculator()
            val result = calculator.add(2, 5)
            Toast.makeText(this, "Result: $result", Toast.LENGTH_SHORT).show()*/
        }
    }

    private fun getFlag(vwo: VWO) {
        // Create VWOContext object
        val userContext = VWOContext()
        // Set User ID
        userContext.id = "unique_user_id1"
        userContext.customVariables = mutableMapOf<String, Any>("name" to "Swapnil")

        // Get feature flag object
        featureFlag = vwo.getFlag("swapnilFlag", userContext)

        // Get the flag value
        val isFeatureFlagEnabled = featureFlag?.isEnabled
        Log.d("SwapnilFlag", "isFeatureFlagEnabled=$isFeatureFlagEnabled")
        // Determine the application flow based on feature flag status
        if (isFeatureFlagEnabled == true) {
            // Your code when feature flag is enabled
            // To get value of a single variable
            val variable1 = featureFlag?.getVariable("Variable1", false)

            // To get value of all variables in object format
            val getAllVariables = featureFlag?.getVariables()//)
            Log.d("SwapnilFlag", "getAllVariables=$getAllVariables")
            Log.d("SwapnilFlag", "variable1=$variable1")
        } else {
            // Your code when feature flag is disabled
        }
    }

    private fun getVariable(featureFlag: GetFlag) {
        val isFeatureFlagEnabled = featureFlag.isEnabled
        Log.d("SwapnilFlag", "isFeatureFlagEnabled=$isFeatureFlagEnabled")
        // Determine the application flow based on feature flag status
        if (isFeatureFlagEnabled) {
            // Your code when feature flag is enabled
            // To get value of a single variable
            val variable1 = featureFlag.getVariable("Variable1", false)

            // To get value of all variables in object format
            val getAllVariables = featureFlag.getVariables()//)
            Log.d("SwapnilFlag", "getAllVariables=$getAllVariables")
            Log.d("SwapnilFlag", "variable1=$variable1")
        } else {
            // Your code when feature flag is disabled
            Log.d(
                "SwapnilFlag",
                "Feature flag is disabled: ${featureFlag.isEnabled} ${featureFlag.getVariables()}"
            )
        }
    }
}