/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vwo.fme;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vwo.VWO;
import static com.vwo.VWO.init;
import com.vwo.fme.databinding.ActivityMainBinding;
import com.vwo.interfaces.IVwoInitCallback;
import com.vwo.interfaces.IVwoListener;
import com.vwo.interfaces.logger.LogTransport;
import com.vwo.models.user.GetFlag;
import com.vwo.models.user.VWOUserContext;
import com.vwo.models.user.VWOInitOptions;
import com.vwo.utils.DeviceIdUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaMainActivity extends AppCompatActivity {

    TestApp prod = new TestApp(0,
            "",
            "flag-name",
            "variable-name",
            "event-name",
            "attribute-name");

    TestApp server = prod;

    String SDK_KEY = server.getSdkKey();

    int ACCOUNT_ID = server.getAccountId();

    private String TAG = "Flag";

    private VWO vwoClient;

    private GetFlag featureFlag;

    private VWOUserContext context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvName.setText("FME Java");

        binding.btnInitSdk.setOnClickListener(view -> {
            VWOInitOptions vwoInitOptions = new VWOInitOptions();
            vwoInitOptions.setSdkKey(SDK_KEY);
            vwoInitOptions.setAccountId(ACCOUNT_ID);

            List<Map<String, Object>> logger = new ArrayList<>();
            Map<String, Object> transport = new HashMap<>();

            transport.put("defaultTransport", (LogTransport) (level, message) -> {
                if (message == null) return;
                Log.d("FME", message);
            });

            logger.add(transport);

            Map<String, Object> loggerConfig = new HashMap<>();
            loggerConfig.put("level", "TRACE");
            loggerConfig.put("transports", logger);

            vwoInitOptions.setLogger(loggerConfig);
            /*Map<String, Object> loggerOptions = new HashMap<>();
            loggerOptions.put("level", "TRACE");
            vwoInitOptions.setLogger(loggerOptions);*/

            init(vwoInitOptions, new IVwoInitCallback() {

                @Override
                public void vwoInitSuccess(@NonNull VWO vwoClient, @NonNull String message) {
                    Log.d(TAG, "vwoInitSuccess " + message);
                    JavaMainActivity.this.vwoClient = vwoClient;
                }

                @Override
                public void vwoInitFailed(@NonNull String message) {
                    Log.d(TAG, "vwoInitFailed: " + message);
                }
            });
        });
        binding.btnGetFlag.setOnClickListener(v -> {
            if (vwoClient != null) {
                getFlag(vwoClient);
            }
        });

        binding.btnGetVariable.setOnClickListener(v -> {
            if (featureFlag != null) {
                getVariable(featureFlag);
            }
        });

        binding.btnTrack.setOnClickListener(v -> track());

        binding.btnAttribute.setOnClickListener(v -> sendAttribute());
        binding.btnJavaScreen.setVisibility(View.GONE);
    }

    private void getFlag(@NonNull VWO vwoClient) {
        context = new VWOUserContext();
        // Set user ID to empty to trigger device ID fallback
        context.setId("");
        // Enable device ID fallback
        context.setEnableDeviceId(true);

        Map<String, Object> customVariables = new HashMap<>();
        customVariables.put("Username", "Swapnil");
        customVariables.put("userType", "trial");
        context.setCustomVariables(customVariables);

        // Log the device ID for demonstration
        DeviceIdUtil deviceIdUtil = new DeviceIdUtil();
        String deviceId = deviceIdUtil.getDeviceId(getApplicationContext());
        Log.d(TAG, "Generated Device ID: " + deviceId);

        vwoClient.getFlag("feature-key", context, new IVwoListener() {
            public void onSuccess(Object data) {
                featureFlag = (GetFlag) data;
                if (featureFlag != null) {
                    boolean isFeatureFlagEnabled = featureFlag.isEnabled();
                    Log.d(TAG, "Received getFlag isFeatureFlagEnabled=" + isFeatureFlagEnabled);
                }
            }

            public void onFailure(@NonNull String message) {
                Log.d(TAG, "getFlag " + message);
            }
        });
        if (featureFlag == null)
            return;
        boolean isFeatureFlagEnabled = featureFlag.isEnabled();

        Log.d(TAG, "isFeatureFlagEnabled=" + isFeatureFlagEnabled);
    }

    private void getVariable(@NonNull GetFlag featureFlag) {
        boolean isFeatureFlagEnabled = featureFlag.isEnabled();
        Log.d(TAG, "isFeatureFlagEnabled=" + isFeatureFlagEnabled);

        if (isFeatureFlagEnabled) {
            String variable = (String) featureFlag.getVariable("variable_key", "default-value");

            List<Map<String, Object>> getAllVariables = featureFlag.getVariables();
            Log.d(TAG, "variable=" + variable + " getAllVariables=" + getAllVariables);
        } else {
            Log.d(TAG, "Feature flag is disabled");
        }
    }

    private void track() {
        if (context == null) return;

        Map<String, Object> properties = new HashMap<>();
        properties.put("cartvalue", 120);
        properties.put("productCountInCart", 2);

        // Track the event for the given event name, user context and properties
        Map<String, Boolean> trackResponse = vwoClient.trackEvent("productViewed", context, properties);
        Log.d(TAG, "track=" + trackResponse);
        // Track the event for the given event name and user context
        //Map<String, Boolean> trackResponse = vwo.trackEvent("vwoevent", userContext);
    }

    private void sendAttribute() {
        if (vwoClient != null) {
            HashMap<String, Object> attributes = new HashMap<>();
            attributes.put("userType", "paid");
            attributes.put("price", 99);
            attributes.put("isEnterpriseCustomer", true);

            vwoClient.setAttribute(attributes, context);
        }
    }
}