package com.vwo.fme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vwo.VWO
import com.vwo.fme.databinding.ActivityMainBinding
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.Recommendation
import com.vwo.models.user.VWOContext
import com.vwo.models.user.VWOInitOptions
import com.vwo.services.LoggerService

val prod = TestApp(
    accountId = 0,
    sdkKey = "",
    flagName = "",
    variableName = "",
    eventName = "",
    attributeName = ""
)

val server = prod
private val SDK_KEY = server.sdkKey
private val ACCOUNT_ID = server.accountId

class MainActivity : AppCompatActivity() {

    private val USER_ID = ""
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
            vwoInitOptions.context = this@MainActivity.applicationContext

            vwoInitOptions.logger = mutableMapOf<String, Any>().apply { put("level", "TRACE") }
            /*vwoInitOptions.gatewayService = mutableMapOf<String, Any>().apply {
                    put("url", "http://10.0.2.2:8000")
                }*/
            //vwoInitOptions.pollInterval = 60000
            vwoInitOptions.cachedSettingsExpiryTime = 2 * 60 * 1000 // 2 min

            //vwoInitOptions.storage = StorageTest()
            // Create VWO instance with the vwoInitOptions
            VWO.init(vwoInitOptions, object : IVwoInitCallback {
                override fun vwoInitSuccess(vwo: VWO, message: String) {
                    // Success
                    this@MainActivity.vwo = vwo
                }

                override fun vwoInitFailed(message: String) {
                    // Log error here
                }
            })
        }
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
        binding.btnJavaScreen.setOnClickListener {
            startActivity(Intent(this, JavaMainActivity::class.java))
        }
    }

    private fun getFlag(vwo: VWO) {
        userContext = VWOContext()
        userContext.id = USER_ID
        userContext.ipAddress = "182.69.183.212"
        //userContext.userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36"
        userContext.userAgent = "AppName/1.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36"
        userContext.customVariables = mutableMapOf(
            "name1" to 21,
            "name2" to 0,
            "name3" to 5,
            "name4" to 11
        )

        // Get feature flag object
        vwo.getFlag(server.flagName, userContext, object : IVwoListener {
            override fun onSuccess(data: Any) {
                featureFlag = data as? GetFlag
                val isFeatureFlagEnabled = featureFlag?.isEnabled()
                Log.d("FME-App", "Received getFlag isFeatureFlagEnabled=$isFeatureFlagEnabled")
            }

            override fun onFailure(message: String) {
                Log.d("FME-App", "getFlag $message")
            }
        })
    }

    private fun getVariable(featureFlag: GetFlag) {
        val isFeatureFlagEnabled = featureFlag.isEnabled()

        // Determine the application flow based on feature flag status
        if (isFeatureFlagEnabled) {
            // To get value of a single variable
            recommendation(featureFlag)
            val variable2 = featureFlag.getVariable(server.variableName, "default-value2")

            // To get value of all variables in object format
            val getAllVariables = featureFlag.getVariables()
            println("Variable values: getAllVariables=$getAllVariables")
        } else {
            // Your code when feature flag is disabled
        }
    }

    private fun recommendation(featureFlag: GetFlag) {

        val recommendationWrapper = featureFlag.getVariable(server.variableName, "default")

        if (recommendationWrapper is Recommendation) {
            val options = mapOf<String, Any>(
                "userId" to USER_ID,
                "productIds" to "1,2,3,4",
                "pageType" to "shopping-cart-page-view"
            )
            recommendationWrapper.getRecommendations(
                options,
                category = "Clothing",
                productIds = listOf(1501),
                object : IVwoListener {

                    override fun onSuccess(data: Any) {
                        println("response is -- $data")
                    }

                    override fun onFailure(message: String) {
                        println("error is -- $message")
                    }
                })
            recommendationWrapper.getRecommendationWidget(
                featureFlag,
                emptyMap(),
                object : IVwoListener {
                    override fun onSuccess(data: Any) {
                        println("getRecommendationWidget response is -- $data")
                    }

                    override fun onFailure(message: String) {
                        println("getRecommendationWidget error is -- $message")
                    }
                })
            println("RecommendationBlock=${recommendationWrapper.recommendationBlock}")
        }
    }

    private fun track() {

        if (!::userContext.isInitialized) return

        val properties = mutableMapOf<String, Any>("cartvalue" to 10)
        // Track the event for the given event name and user context
        val map: MutableMap<String, Any> = mutableMapOf()
        map["category"] = "electronics"
        map["isWishlisted"] = false
        map["price"] = 21
        map["productId"] = 1
        val trackResponse = vwo?.trackEvent(server.eventName, userContext, map)
        //val trackResponse = vwo?.trackEvent(server.eventName, userContext)
    }

    private fun sendAttribute() {
        if (!::userContext.isInitialized) return

        vwo?.setAttribute(server.attributeName, "attribute-value1", userContext)
        vwo?.setAttribute("attribute-name-float", 1.01, userContext)
        vwo?.setAttribute("attribute-name-boolean", true, userContext)
    }
}

data class TestApp(
    val accountId: Int,
    val sdkKey: String,
    val flagName: String,
    val variableName: String,
    val eventName: String,
    val attributeName: String
)