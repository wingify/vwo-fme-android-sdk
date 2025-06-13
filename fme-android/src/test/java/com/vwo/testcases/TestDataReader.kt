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

import com.google.gson.Gson
import com.vwo.VWOClient
import com.vwo.utils.NetworkUtil.Companion.removeNullValues
import java.nio.file.Files
import java.nio.file.Paths

class TestDataReader {
    var testCases = readTestCases("index.json")

    private var gson: Gson? = null

    /**
     * Reads the test cases from a JSON file located in the specified folder.
     * The JSON file must be named "index.json".
     *
     * @param folderPath The path to the folder containing the "index.json" file.
     * @return An instance of TestCases containing the data from the JSON file, or null if the file does not exist.
     */
    private fun readTestCases(folderPath: String): TestCases? {
        gson = Gson()
        val content = readFile(folderPath)
        return content
    }

    /**
     * Reads the log files and returns the messages in a map.
     */
    private fun readFile(fileName: String): TestCases? {
        try {
            val inputStream = this.javaClass.classLoader?.getResourceAsStream(fileName)
            val contents = inputStream?.bufferedReader().use { it?.readText() }
            val values = gson?.fromJson(contents, TestCases::class.java)
            return values
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return null
    }
}
