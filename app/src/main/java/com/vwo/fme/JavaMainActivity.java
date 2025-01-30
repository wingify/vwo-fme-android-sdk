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
import com.vwo.fme.databinding.ActivityMainBinding;
import com.vwo.interfaces.IVwoInitCallback;
import com.vwo.interfaces.IVwoListener;
import com.vwo.models.user.GetFlag;
import com.vwo.models.user.VWOContext;
import com.vwo.models.user.VWOInitOptions;

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

    private VWO vwo;
    private GetFlag featureFlag;
    private VWOContext userContext;

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

            Map<String, Object> loggerOptions = new HashMap<>();
            loggerOptions.put("level", "TRACE");
            vwoInitOptions.setLogger(loggerOptions);

            VWO.init(vwoInitOptions, new IVwoInitCallback() {
                @Override
                public void vwoInitSuccess(@NonNull VWO vwo, @NonNull String message) {
                    Log.d("Flag", "vwoInitSuccess " + message);
                    JavaMainActivity.this.vwo = vwo;
                }

                @Override
                public void vwoInitFailed(@NonNull String message) {
                    Log.d("Flag", "vwoInitFailed: " + message);
                }
            });
        });
        binding.btnGetFlag.setOnClickListener(v -> {
            if (vwo != null) {
                getFlag(vwo);
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

    private void getFlag(@NonNull VWO vwo) {
        userContext = new VWOContext();
        userContext.setId("unique_user_id");

        Map<String, Object> customVariables = new HashMap<>();
        customVariables.put("Username", "Swapnil");
        customVariables.put("userType", "trial");
        userContext.setCustomVariables(customVariables);

        vwo.getFlag("feature-key", userContext, new IVwoListener() {
            public void onSuccess(Object data) {
                featureFlag = (GetFlag) data;
                if (featureFlag != null) {
                    boolean isFeatureFlagEnabled = featureFlag.isEnabled();
                    Log.d("FME-App", "Received getFlag isFeatureFlagEnabled=" + isFeatureFlagEnabled);
                }
            }

            public void onFailure(@NonNull String message) {
                Log.d("FME-App", "getFlag " + message);
            }
        });
        if (featureFlag == null)
            return;
        boolean isFeatureFlagEnabled = featureFlag.isEnabled();

        Log.d("Flag", "isFeatureFlagEnabled=" + isFeatureFlagEnabled);
    }

    private void getVariable(@NonNull GetFlag featureFlag) {
        boolean isFeatureFlagEnabled = featureFlag.isEnabled();
        Log.d("Flag", "isFeatureFlagEnabled=" + isFeatureFlagEnabled);

        if (isFeatureFlagEnabled) {
            String variable1 = (String) featureFlag.getVariable("variable_key", "default-value1");

            List<Map<String, Object>> getAllVariables = featureFlag.getVariables();
            Log.d("Flag", "variable1=" + variable1 + " getAllVariables=" + getAllVariables);
        } else {
            Log.d("Flag", "Feature flag is disabled: " + featureFlag.isEnabled() + " " + featureFlag.getVariables());
        }
    }

    private void track() {
        if (userContext == null) return;

        Map<String, Object> properties = new HashMap<>();
        properties.put("cartvalue", 120);
        properties.put("productCountInCart", 2);

        // Track the event for the given event name, user context and properties
        Map<String, Boolean> trackResponse = vwo.trackEvent("productViewed", userContext, properties);
        Log.d("Flag", "track=" + trackResponse);
        // Track the event for the given event name and user context
        //Map<String, Boolean> trackResponse = vwo.trackEvent("vwoevent", userContext);
    }

    private void sendAttribute() {
        if (vwo != null) {
            vwo.setAttribute("userType", "paid", userContext);
            vwo.setAttribute("attribute-name-float", 1.01, userContext);
            vwo.setAttribute("attribute-name-boolean", true, userContext);
        }
    }
}