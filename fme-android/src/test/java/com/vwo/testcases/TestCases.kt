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
package com.vwo.testcases

import com.fasterxml.jackson.annotation.JsonProperty

class TestCases {
    @JsonProperty("GETFLAG_WITHOUT_STORAGE")
    var getFlagWithoutStorage: List<TestData>? = null

    @JsonProperty("GETFLAG_MEG_RANDOM")
    var getFlagMegRandom: List<TestData>? = null

    @JsonProperty("GETFLAG_MEG_ADVANCE")
    var getFlagMegAdvance: List<TestData>? = null

    @JsonProperty("GETFLAG_WITH_STORAGE")
    var getFlagWithStorage: List<TestData>? = null

    @JsonProperty("GETFLAG_WITH_SALT")
    var GETFLAG_WITH_SALT: List<TestData>? = null

    @JsonProperty("SETTINGS_WITH_DIFFERENT_SALT")
    var settingsWithDifferentSalt: List<TestData>? = null
}
