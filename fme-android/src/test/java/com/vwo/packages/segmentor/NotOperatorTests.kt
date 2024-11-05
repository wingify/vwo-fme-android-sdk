/**
 * Copyright 2024 Wingify Software Pvt. Ltd.
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

class NotOperatorTests {

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
                this@NotOperatorTests.vwo = vwo
            }

            override fun vwoInitFailed(message: String) {
            }
        })
    }

    @Test
    fun exactMatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "something")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun exactMatchWithSpecialCharactersTest() {
        val dsl =
            "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"f25u!v@b#k$6%9^f&o*v(m)w_-=+s,./`(*&^%$#@!\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "f25u!v@b#k$6%9^f&o*v(m)w_-=+s,./`(*&^%$#@!")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun exactMatchWithSpacesTest() {
        val dsl =
            "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"nice to see you. will    you be   my        friend?\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "nice to see you. will    you be   my        friend?")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun exactMatchWithUpperCaseTest() {
        val dsl =
            "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"HgUvshFRjsbTnvsdiUFFTGHFHGvDRT.YGHGH\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "HgUvshFRjsbTnvsdiUFFTGHFHGvDRT.YGHGH")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.456\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.456)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeExtraDecimalZerosTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.456\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.456000000)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeMismatchTest2() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.0)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.456\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "123.456000000")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest2() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.0\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun stringifiedFloatTest3() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.4560000\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.456)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"E\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'E') // Char in JAVA
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"true\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", true)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeTest2() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"false\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", false)
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun mismatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "notsomething")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun partOfTextTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"zzsomethingzz\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun singleCharTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"zzsomethingzz\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "i")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun CaseMismatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "Something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun CaseMismatchTest2() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "SOMETHING")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun noValueProvidedTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun missingkeyValueTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun nullValueProvidedTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", null)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun incorrectKeyTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("neq", "something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun incorrectKeyCaseTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"something\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("EQ", "something")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun numericDataTypeMismatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 12)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeMismatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.456\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun floatDataTypeMismatchTest2() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"123.456\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 123.4567)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeMismatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"false\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", true)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun booleanDataTypeMismatchTest2() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"true\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", false)
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun charDataTypeCaseMismatchTest() {
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"E\"}}]}}"
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
        val dsl = "{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"e\"}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", 'E')
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun nestedNotOperatorTest() {
        val dsl =
            "{\"or\":[{\"or\":[{\"not\":{\"or\":[{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}]}}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest() {
        val dsl =
            "{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}}]}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest2() {
        val dsl =
            "{\"and\":[{\"and\":[{\"not\":{\"and\":[{\"and\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}]}}]}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest3() {
        val dsl =
            "{\"and\":[{\"not\":{\"and\":[{\"not\":{\"and\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]}}]}}]}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("eq", "eq_value")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest4() {
        val dsl =
            "{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"custom_variable\":{\"neq\":\"eq_value\"}}]}}]}}]}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("neq", "eq_value")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest5() {
        val dsl =
            "{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"custom_variable\":{\"neq\":\"not_eq_value\"}}]}}]}}]}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("neq", "eq_value")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest6() {
        val dsl =
            "{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"custom_variable\":{\"neq\":\"eq_value\"}}]}}]}}]}}]}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("neq", "eq_value")
                put("expectation", false)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }

    @Test
    fun multipleNotOperatorTest7() {
        val dsl =
            "{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"not\":{\"or\":[{\"custom_variable\":{\"neq\":\"neq_value\"}}]}}]}}]}}]}}]}}"
        val customVariables: Map<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("neq", "eq_value")
                put("expectation", true)
            }
        }

        verifyExpectation(dsl, removeNullValues(customVariables))
    }
}
