/**
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vwo.packages.segmentor

import com.vwo.VWO
import com.vwo.VWO.init
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.VWOInitOptions
import com.vwo.utils.NetworkUtil.Companion.removeNullValues
import org.junit.Before
import org.junit.Test

class GreaterThanOperatorTests {

    private val sdkKey = "abcd"
    private val accountId = 1234
    private lateinit var vwo: VWO

    @Before
    fun initialize() {
        val vwoInitOptions = VWOInitOptions()
        vwoInitOptions.sdkKey = sdkKey
        vwoInitOptions.accountId = accountId
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                this@GreaterThanOperatorTests.vwo = vwo
            }

            override fun vwoInitFailed(message: String) {
            }
        })
    }

    @Test
    fun greaterThanOperatorPass() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"gt(150)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 160)
                put("expectation", true)
            }
        }
        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun greaterThanOperatorFail() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"gt(150)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 100)
                put("expectation", false)
            }
        }
        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun greaterThanOperatorEqualValueFail() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"gt(150)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 150)
                put("expectation", false)
            }
        }
        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun greaterThanOperatorStringValueFail() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"gt(150)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "abc")
                put("expectation", false)
            }
        }
        verifyExpectation(dsl, removeNullValues(customVariables))
    }
}
