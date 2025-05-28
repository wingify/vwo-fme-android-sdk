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
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager
import com.vwo.utils.NetworkUtil.Companion.removeNullValues
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ContainsOperandTests {

    private val sdkKey = "abcd"
    private val accountId = 1234
    private lateinit var vwo: VWO

    @Before
    fun initialize() {
        val vwoInitOptions = VWOInitOptions()
        vwoInitOptions.sdkKey = sdkKey
        vwoInitOptions.accountId = accountId
        vwoInitOptions.isUsageStatsDisabled = true
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                this@ContainsOperandTests.vwo = vwo
            }

            override fun vwoInitFailed(message: String) {
            }
        })
    }

    @Test
    fun exactMatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun suffixMatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "asdn3kn42knsdsomething")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun prefixMatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "somethingdfgdwerewew")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun containsMatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "asdn3kn42knsdsomethingjsbdj")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun specialCharactersTest() {
        val dsl =
            "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*f25u!v@b#k$6%9^f&o*v(m)w_-=+s,./`(*&^%$#@!*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put(
                    "eq",
                    "A-N-Y-T-H-I-N-G---f25u!v@b#k$6%9^f&o*v(m)w_-=+s,./`(*&^%$#@!----A-N-Y-T-H-I-N-G"
                )
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun containsOperandFalsyTestWithSpecialCharacter() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"a\":\"wildcard(*some*thing*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("a", "hellosomethingworld")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun spacesTest() {
        val dsl =
            "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*nice to see you. will    you be   my        friend?*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put(
                    "eq",
                    "Hello there!! nice to see you. will    you be   my        friend? Yes, Great!!"
                )
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun upperCaseTest() {
        val dsl =
            "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*HgUvshFRjsbTnvsdiUFFTGHFHGvDRT.YGHGH*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put(
                    "eq",
                    "A-N-Y-T-H-I-N-G---HgUvshFRjsbTnvsdiUFFTGHFHGvDRT.YGHGH----A-N-Y-T-H-I-N-G"
                )
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*123*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 365412363)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*123.456*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 765123.4567364)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*123*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 765123.7364)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*123.456*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "87654123.4567902")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*E*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'E')
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*true*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", true)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*false*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", false)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun mismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "qwertyu")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun partOfTextTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*zzsomethingzz*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "something")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun singleCharTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*zzsomethingzz*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "i")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun CaseMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "Something")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun CaseMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "SOMETHING")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun noValueProvidedTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun missingkeyValueTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun nullValueProvidedTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", null)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun incorrectKeyTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("neq", "something")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun incorrectKeyCaseTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*something*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("EQ", "something")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*123*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 12)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*false*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", true)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*true*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", false)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeCaseMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*E*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'e')
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeCaseMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"wildcard(*e*)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'E')
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