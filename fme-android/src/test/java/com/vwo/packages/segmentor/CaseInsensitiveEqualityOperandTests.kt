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
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager.attachEvaluator
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager.validateSegmentation
import com.vwo.utils.NetworkUtil.Companion.removeNullValues
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CaseInsensitiveEqualityOperandTests {

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
                this@CaseInsensitiveEqualityOperandTests.vwo = vwo
            }

            override fun vwoInitFailed(message: String) {
            }
        })
    }

    @Test
    fun exactMatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun exactMatchWithSpecialCharactersTest() {
        val dsl =
            "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(f25u!v@b#k$6%9^f&o*v(m)w_-=+s,./`(*&^%$#@!)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "f25u!v@b#k$6%9^f&o*v(m)w_-=+s,./`(*&^%$#@!")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun exactMatchWithSpacesTest() {
        val dsl =
            "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(nice to see you. will    YOU be   my        Friend?)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "nice to see you. will    YOU be   my        Friend?")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun exactMatchWithUpperCaseTest() {
        val dsl =
            "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(HgUvshFRjsbTnvsdiUFFTGHFHGvDRT.YGHGH)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "HgUvshFRjsbTnvsdiUFFTGHFHGvDRT.YGHGH")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun caseMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "Something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun caseMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "SOMETHINg")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun trimValueTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(HI)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "          hi          ")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.456)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.456)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeExtraDecimalZerosTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.456)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.456000000)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.0)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.456)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "123.456000000")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.0)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest3() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.4560000)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.456)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(E)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'E')
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }


    @Test
    fun charDataTypeCaseMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(E)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'e')
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeCaseMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(e)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(true)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(false)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", false)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeTest3() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(True)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", true)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun mismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "notsomething")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun partOfTextTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(zzsomethingzz)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(zzsomethingzz)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "i")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun noValueProvidedTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun nullValueProvidedTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(something)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 12)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.456)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeMismatchTest2() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(123.456)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.4567)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeMismatchTest() {
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(false)\"}}]}"
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
        val dsl = "{\"or\":[{\"custom_variable\":{\"eq\":\"lower(true)\"}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", false)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    private fun verifyExpectation(dsl: String, customVariables: Map<String, Any>) {
        attachEvaluator()
        Assert.assertEquals(
            validateSegmentation(dsl, customVariables),
            customVariables["expectation"]
        )
    }
}
