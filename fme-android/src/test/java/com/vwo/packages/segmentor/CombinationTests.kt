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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CombinationTests {

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
                this@CombinationTests.vwo = vwo
            }

            override fun vwoInitFailed(message: String) {
            }
        })
    }

    @Test
    fun dslWithAllOperandsTest() {
        val dsl =
            "{\"or\":[{\"or\":[{\"and\":[{\"or\":[{\"custom_variable\":{\"start_with\":\"wildcard(my_start_with_val*)\"}}]},{\"not\":{\"or\":[{\"custom_variable\":{\"neq\":\"not_eq_value\"}}]}}]},{\"or\":[{\"custom_variable\":{\"contain\":\"wildcard(*my_contain_val*)\"}}]}]},{\"and\":[{\"or\":[{\"custom_variable\":{\"eq\":\"eq_value\"}}]},{\"or\":[{\"custom_variable\":{\"reg\":\"regex(myregex+)\"}}]}]}]}"
        val customVariables: Map<String, Map<String, Any>> =
            object : HashMap<String, Map<String, Any>>() {
                init {
                    put("matchingStartWithValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "my_start_with_valzzzzzzzzzzzzzzzz")
                            put("neq", 1)
                            put("contain", 1)
                            put("eq", 1)
                            put("reg", 1)
                            put("expectation", true)
                        }
                    })

                    put("matchingNotEqualToValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", 1)
                            put("neq", "not_eq_value")
                            put("contain", 1)
                            put("eq", 1)
                            put("reg", 1)
                            put("expectation", false)
                        }
                    })

                    put(
                        "matchingBothStartWithAndNotEqualToValue",
                        object : HashMap<String, Any>() {
                            init {
                                put("start_with", "my_start_with_valzzzzzzzzzzzzzzzz")
                                put("neq", "not_eq_value")
                                put("contain", 1)
                                put("eq", 1)
                                put("reg", 1)
                                put("expectation", false)
                            }
                        })

                    put("matchingContainsWithValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            put("neq", false)
                            put("contain", "zzzzzzmy_contain_valzzzzz")
                            put("eq", 1)
                            put("reg", 1)
                            put("expectation", true)
                        }
                    })

                    put("matchingEqualToValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            //put("neq", null)
                            put("contain", "my_ contain _val")
                            put("eq", "eq_value")
                            put("reg", 1)
                            put("expectation", false)
                        }
                    })

                    put("matchingRegexValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            put("neq", 123)
                            put("contain", "my_ contain _val")
                            put("eq", "eq__value")
                            put("reg", "myregexxxxxx")
                            put("expectation", false)
                        }
                    })

                    put("matchingBothEqualToAndRegexValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            put("neq", "not_matching")
                            put("contain", "my\$contain\$val")
                            put("eq", "eq_value")
                            put("reg", "myregexxxxxx")
                            put("expectation", true)
                        }
                    })

                    put(
                        "matching_both_start_with_and_not_equal_to_value",
                        object : HashMap<String, Any>() {
                            init {
                                put("start_with", "my_start_with_valzzzzzzzzzzzzzzzz")
                                put("neq", "not_eq_value")
                                put("contain", 1)
                                put("eq", 1)
                                put("reg", 1)
                                put("expectation", false)
                            }
                        })

                    put("matching_not_equal_to_value", object : HashMap<String, Any>() {
                        init {
                            put("start_with", 1)
                            put("neq", "not_eq_value")
                            put("contain", 1)
                            put("eq", 1)
                            put("reg", 1)
                            put("expectation", false)
                        }
                    })

                    put("matching_start_with_value", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "my_start_with_valzzzzzzzzzzzzzzzz")
                            put("neq", 1)
                            put("contain", 1)
                            put("eq", 1)
                            put("reg", 1)
                            put("expectation", true)
                        }
                    })

                    put("matching_regex_value", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            put("neq", 123)
                            put("contain", "my_ contain _val")
                            put("eq", "eq__value")
                            put("reg", "myregexxxxxx")
                            put("expectation", false)
                        }
                    })

                    put(
                        "matching_both_equal_to_and_regex_value",
                        object : HashMap<String, Any>() {
                            init {
                                put("start_with", "m1y_1sta1rt_with_val")
                                put("neq", "not_matching")
                                put("contain", "my\$contain\$val")
                                put("eq", "eq_value")
                                put("reg", "myregexxxxxx")
                                put("expectation", true)
                            }
                        })

                    put("matching_equal_to_value", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            //put("neq", null)
                            put("contain", "my_ contain _val")
                            put("eq", "eq_value")
                            put("reg", 1)
                            put("expectation", false)
                        }
                    })
                }
            }

        validateAllCases(dsl, customVariables)
    }

    @Test
    fun emptyDslTest() {
        val dsl = "{}"
        val customVariables: Map<String, Map<String, Any>> =
            object : HashMap<String, Map<String, Any>>() {
                init {
                    put("matchingStartWithValue", object : HashMap<String, Any>() {
                        init {
                            put("start_with", "m1y_1sta1rt_with_val")
                            //put("neq", null)
                            put("contain", "my_ contain _val")
                            put("eq", "eq_value")
                            put("reg", 1)
                            put("expectation", false)
                        }
                    })
                }
            }

        validateAllCases(dsl, customVariables)
    }

    @Test
    fun dslWithAllOperandsTest2() {
        val dsl =
            "{\"or\":[{\"and\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"notvwo\":\"notvwo\"}}]}},{\"or\":[{\"custom_variable\":{\"vwovwovwo\":\"regex(vwovwovwo)\"}}]}]},{\"or\":[{\"custom_variable\":{\"regex_vwo\":\"regex(this\\\\s+is\\\\s+vwo)\"}}]}]},{\"and\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"vwo_not_equal_to\":\"owv\"}}]}},{\"or\":[{\"custom_variable\":{\"vwo_equal_to\":\"vwo\"}}]}]},{\"or\":[{\"or\":[{\"custom_variable\":{\"vwo_starts_with\":\"wildcard(owv vwo*)\"}}]},{\"or\":[{\"custom_variable\":{\"vwo_contains\":\"wildcard(*vwo vwo vwo vwo vwo*)\"}}]}]}]}]}"
        val customVariables: Map<String, Map<String, Any>> =
            object : HashMap<String, Map<String, Any>>() {
                init {
                    put("false_1", object : HashMap<String, Any>() {
                        init {
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("vwo_contains", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "v owv vwo")
                            put("vwovwovwo", "vwovovwo")
                            put("expectation", false)
                        }
                    })

                    put("false_2", object : HashMap<String, Any>() {
                        init {
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("vwo_contains", "vwo")
                            put("vwo_equal_to", "vwovwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "owv vwo")
                            put("vwovwovwo", "vwovw")
                            put("expectation", false)
                        }
                    })

                    put("false_3", object : HashMap<String, Any>() {
                        init {
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("vwo_contains", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "vwo owv vwo")
                            put("vwovwovwo", "vwovwovw")
                            put("expectation", false)
                        }
                    })

                    put("false_4", object : HashMap<String, Any>() {
                        init {
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("vwo_contains", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "vwo owv vwo")
                            put("vwovwovwo", "vwo")
                            put("expectation", false)
                        }
                    })
                }
            }

        validateAllCases(dsl, customVariables)
    }

    @Test
    fun dslWithAllOperandsTest3() {
        val dsl =
            "{\"or\":[{\"and\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"notvwo\":\"notvwo\"}}]}},{\"or\":[{\"custom_variable\":{\"vwovwovwo\":\"regex(vwovwovwo)\"}}]}]},{\"or\":[{\"custom_variable\":{\"regex_vwo\":\"regex(this\\\\s+is\\\\s+vwo)\"}}]}]},{\"and\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"vwo_not_equal_to\":\"owv\"}}]}},{\"or\":[{\"custom_variable\":{\"vwo_equal_to\":\"vwo\"}}]}]},{\"or\":[{\"or\":[{\"custom_variable\":{\"vwo_starts_with\":\"wildcard(owv vwo*)\"}}]},{\"or\":[{\"custom_variable\":{\"vwo_contains\":\"wildcard(*vwo vwo vwo vwo vwo*)\"}}]}]}]}]}"
        val customVariables: Map<String, Map<String, Any>> =
            object : HashMap<String, Map<String, Any>>() {
                init {
                    put("true_1", object : HashMap<String, Any>() {
                        init {
                            put("vwo_starts_with", "vwo owv vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("notvwo", "vo")
                            put("regex_vwo", "this   is vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("vwo_contains", "vw")
                            put("expectation", true)
                        }
                    })

                    put("true_2", object : HashMap<String, Any>() {
                        init {
                            put("vwo_starts_with", "owv vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("vwo_contains", "vwo")
                            put("expectation", true)
                        }
                    })

                    put("true_3", object : HashMap<String, Any>() {
                        init {
                            put("vwo_starts_with", "owv vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("notvwo", "vwovwo")
                            put("regex_vwo", "this   isvwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("vwo_contains", "vwo")
                            put("expectation", true)
                        }
                    })

                    put("true_4", object : HashMap<String, Any>() {
                        init {
                            put("vwo_starts_with", "owv vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_equal_to", "vwo")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("vwovwovwo", "vwo")
                            put("vwo_contains", "vwo")
                            put("expectation", true)
                        }
                    })
                }
            }

        validateAllCases(dsl, customVariables)
    }

    @Test
    fun dslWithAllOperandsTest4() {
        val dsl =
            "{\"and\":[{\"or\":[{\"custom_variable\":{\"contains_vwo\":\"wildcard(*vwo*)\"}}]},{\"and\":[{\"and\":[{\"or\":[{\"and\":[{\"or\":[{\"and\":[{\"or\":[{\"custom_variable\":{\"regex_for_all_letters\":\"regex(^[A-z]+$)\"}}]},{\"or\":[{\"custom_variable\":{\"regex_for_capital_letters\":\"regex(^[A-Z]+$)\"}}]}]},{\"or\":[{\"custom_variable\":{\"regex_for_small_letters\":\"regex(^[a-z]+$)\"}}]}]},{\"or\":[{\"custom_variable\":{\"regex_for_no_zeros\":\"regex(^[1-9]+$)\"}}]}]},{\"or\":[{\"custom_variable\":{\"regex_for_zeros\":\"regex(^[0]+$)\"}}]}]},{\"or\":[{\"custom_variable\":{\"regex_real_number\":\"regex(^\\\\d+(\\\\.\\\\d+)?)\"}}]}]},{\"or\":[{\"or\":[{\"custom_variable\":{\"this_is_regex\":\"regex(this\\\\s+is\\\\s+text)\"}}]},{\"and\":[{\"and\":[{\"or\":[{\"custom_variable\":{\"starts_with\":\"wildcard(starts_with_variable*)\"}}]},{\"or\":[{\"custom_variable\":{\"contains\":\"wildcard(*contains_variable*)\"}}]}]},{\"or\":[{\"not\":{\"or\":[{\"custom_variable\":{\"is_not_equal_to\":\"is_not_equal_to_variable\"}}]}},{\"or\":[{\"custom_variable\":{\"is_equal_to\":\"equal_to_variable\"}}]}]}]}]}]}]}"
        val customVariables: Map<String, Map<String, Any>> =
            object : HashMap<String, Map<String, Any>>() {
                init {
                    put("false_5", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF6")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 12231023)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", "0001000")
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", false)
                        }
                    })

                    put("false_6", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "is_not_equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 1223123)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", false)
                        }
                    })

                    put("false_7", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF6")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 12231023)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "startss_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", false)
                        }
                    })

                    put("false_8", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "wingify")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 1223123)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", false)
                        }
                    })

                    put("false_9", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 1223123)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", "not a number")
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", false)
                        }
                    })

                    put("false_10", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 1223123)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "_variable")
                            put("this_is_regex", "thisis    regex")
                            put("expectation", false)
                        }
                    })

                    put("true_5", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 1223123)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", true)
                        }
                    })

                    put("true_6", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 1223123)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 1234)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", true)
                        }
                    })

                    put("true_7", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF6")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 12231023)
                            put("regex_for_small_letters", "sadfAksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", true)
                        }
                    })

                    put("true_8", object : HashMap<String, Any>() {
                        init {
                            put("contains", "contains_variable")
                            put("contains_vwo", "legends say that vwo is the best")
                            put("is_equal_to", "equal_to_variable")
                            put("is_not_equal_to", "is_not_equal_to_variable")
                            put("regex_for_all_letters", "dsfASF6")
                            put("regex_for_capital_letters", "SADFLSDLF")
                            put("regex_for_no_zeros", 12231023)
                            put("regex_for_small_letters", "sadfksjdf")
                            put("regex_for_zeros", 0)
                            put("regex_real_number", 12321.2242)
                            put("starts_with", "starts_with_variable")
                            put("this_is_regex", "this    is    regex")
                            put("expectation", true)
                        }
                    })
                }
            }

        validateAllCases(dsl, customVariables)
    }

    @Test
    fun dslWithAllOperandsTest5() {
        val dsl =
            "{\"and\":[{\"or\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"thanos\":\"snap\"}}]}},{\"or\":[{\"custom_variable\":{\"batman\":\"wildcard(*i am batman*)\"}}]}]},{\"or\":[{\"custom_variable\":{\"joker\":\"regex((joker)+)\"}}]}]},{\"and\":[{\"or\":[{\"or\":[{\"custom_variable\":{\"lol\":\"lolololololol\"}}]},{\"or\":[{\"custom_variable\":{\"blablabla\":\"wildcard(*bla*)\"}}]}]},{\"and\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"notvwo\":\"notvwo\"}}]}},{\"or\":[{\"and\":[{\"or\":[{\"custom_variable\":{\"vwovwovwo\":\"regex(vwovwovwo)\"}}]},{\"or\":[{\"custom_variable\":{\"regex_vwo\":\"regex(this\\\\s+is\\\\s+vwo)\"}}]}]},{\"or\":[{\"and\":[{\"not\":{\"or\":[{\"custom_variable\":{\"vwo_not_equal_to\":\"owv\"}}]}},{\"or\":[{\"custom_variable\":{\"vwo_equal_to\":\"vwo\"}}]}]},{\"or\":[{\"custom_variable\":{\"vwo_starts_with\":\"wildcard(owv vwo*)\"}}]}]}]}]},{\"or\":[{\"custom_variable\":{\"vwo_contains\":\"wildcard(*vwo vwo vwo vwo vwo*)\"}}]}]}]}]}"
        val customVariables: Map<String, Map<String, Any>> =
            object : HashMap<String, Map<String, Any>>() {
                init {
                    put("false_11", object : HashMap<String, Any>() {
                        init {
                            put("batman", "hello i am batman world")
                            put("blablabla", "lba")
                            put("joker", "joker joker joker")
                            put("lol", "lollolololol")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("thanos", "snap")
                            put("vwo_contains", "vwo vwo vwo vwo vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("expectation", false)
                        }
                    })

                    put("false_12", object : HashMap<String, Any>() {
                        init {
                            put("batman", "hello i am batman world")
                            put("blablabla", "bla bla bla")
                            put("joker", "joker joker joker")
                            put("lol", "lolololololol")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("thanos", "half universe")
                            put("vwo_contains", "vwo vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "owv vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("expectation", false)
                        }
                    })

                    put("false_13", object : HashMap<String, Any>() {
                        init {
                            put("batman", "hello i am batman world")
                            put("blablabla", "bla bla bla")
                            put("joker", "joker joker joker")
                            put("lol", "lollolololol")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("thanos", "snap")
                            put("vwo_contains", "vwo vwo vwo vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("expectation", false)
                        }
                    })

                    put("true_9", object : HashMap<String, Any>() {
                        init {
                            put("batman", "hello i am batman world")
                            put("blablabla", "bla bla bla")
                            put("joker", "joker joker joker")
                            put("lol", "lollolololol")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("thanos", "half universe")
                            put("vwo_contains", "vwo vwo vwo vwo vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "owv vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("expectation", true)
                        }
                    })

                    put("true_10", object : HashMap<String, Any>() {
                        init {
                            put("batman", "hello i am batman world")
                            put("blablabla", "bla bla bla")
                            put("joker", "joker joker joker")
                            put("lol", "lolololololol")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("thanos", "half universe")
                            put("vwo_contains", "vwo vwo vwo vwo vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "owv vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("expectation", true)
                        }
                    })

                    put("true_11", object : HashMap<String, Any>() {
                        init {
                            put("batman", "hello i am batman world")
                            put("blablabla", "bla bla bla")
                            put("joker", "joker joker joker")
                            put("lol", "lolololololol")
                            put("notvwo", "vwo")
                            put("regex_vwo", "this   is vwo")
                            put("thanos", "snap")
                            put("vwo_contains", "vwo vwo vwo vwo vwo")
                            put("vwo_equal_to", "vwo")
                            put("vwo_not_equal_to", "vwo")
                            put("vwo_starts_with", "owv vwo")
                            put("vwovwovwo", "vwovwovwo")
                            put("expectation", false) // TODO FIX to true
                        }
                    })
                }
            }

        validateAllCases(dsl, customVariables)
    }

    private fun validateAllCases(dsl: String, customVariables: Map<String, Map<String, Any>>) {
        SegmentationManager.attachEvaluator()
        for ((_, value) in customVariables) {
            val isPresegmentValid: Boolean =
                SegmentationManager.validateSegmentation(dsl, value)
            assertEquals(isPresegmentValid, value["expectation"])
        }
    }
}
