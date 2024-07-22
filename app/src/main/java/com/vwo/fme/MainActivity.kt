package com.vwo.fme

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vwo.VWO
import com.vwo.fme.databinding.ActivityMainBinding
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.VWOContext
import com.vwo.models.user.VWOInitOptions


private const val SDK_KEY = ""
private const val ACCOUNT_ID = 0

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnAdd.setOnClickListener {
            // Initialize VWO SDK
            val vwoInitOptions = VWOInitOptions()
            // Set SDK Key and Account ID
            vwoInitOptions.sdkKey = SDK_KEY
            vwoInitOptions.accountId = ACCOUNT_ID

            vwoInitOptions.logger = mutableMapOf<String, Any>().apply { put("level","TRACE") }
            // create VWO instance with the vwoInitOptions
            VWO.init(vwoInitOptions, object : IVwoInitCallback {
                override fun vwoInitSuccess(vwo: VWO, message: String) {
                    Log.d("SwapnilFlag", "vwoInitSuccess $message")
                    //getFlag(vwo)
                }

                override fun vwoInitFailed(message: String) {
                    Log.d("SwapnilFlag", "vwoInitFailed: $message")
                }
            })


            /*val calculator = Calculator()
            val result = calculator.add(2, 5)
            Toast.makeText(this, "Result: $result", Toast.LENGTH_SHORT).show()
        }
    }
}