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
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager
import com.vwo.utils.NetworkUtil.Companion.removeNullValues
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class AndOperatorTests {

    private lateinit var vwo: VWO
    private val SDK_KEY = "abcd"
    private val ACCOUNT_ID = 1234

    @Before
    fun initialize() {
        val vwoInitOptions = VWOInitOptions()
        vwoInitOptions.sdkKey = SDK_KEY
        vwoInitOptions.accountId = ACCOUNT_ID
        VWO.init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                this@AndOperatorTests.vwo = vwo
            }

            override fun vwoInitFailed(message: String) {
            }
        })
    }

    @Test
    fun singleAndOperatorMatchingTest() {
        val dsl = "{\"and\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun singleAndOperatorMismatchTest() {
        val dsl = "{\"and\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("a", "n_eq_value")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun singleAndOperatorCaseMismatchTest() {
        val dsl = "{\"and\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "Eq_Value")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleAndOperatorTest2() {
        val dsl =
            "{\"and\":[{\"and\":[{\"and\":[{\"and\":[{\"and\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}]}]}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleAndOperatorWithSingleCorrectValueTest() {
        val dsl =
            "{\"and\":[{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]},{\"or\":[{\"custom_variable\":{\"reg\":\"regex(myregex+)\"}}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("reg", "wrong")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleAndOperatorWithSingleCorrectValueTest2() {
        val dsl =
            "{\"and\":[{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]},{\"or\":[{\"custom_variable\":{\"reg\":\"regex(myregex+)\"}}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "wrong")
                put("reg", "myregexxxxxx")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleAndOperatorWithAllCorrectCustomVariablesTest() {
        val dsl =
            "{\"and\":[{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]},{\"or\":[{\"custom_variable\":{\"reg\":\"regex(myregex+)\"}}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("reg", "myregexxxxxx")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleAndOperatorWithAllIncorrectCorrectCustomVariablesTest() {
        val dsl =
            "{\"and\":[{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]},{\"or\":[{\"custom_variable\":{\"reg\":\"regex(myregex+)\"}}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "wrong")
                put("reg", "wrong")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun sdhgsjdh() {
        val dsl = "{\"or\":[{\"user\":\"Varun\"}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "wrong")
                put("reg", "wrong")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    private fun verifyExpectation(dsl: String, customVariables: Map<String, Any>) {
        SegmentationManager.attachEvaluator()
        assertEquals(
            SegmentationManager.validateSegmentation(dsl, customVariables),
            customVariables["expectation"]
        )
    }
}
