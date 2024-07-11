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
package com.vwo.utils

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object SDKMetaUtil {
    private const val POM_FILE_PATH = "pom.xml"

    /**
     * Returns the sdkVersion
     */
    var sdkVersion: String? = null
        private set

    /**
     * Initializes the SDKMetaUtil with the sdkVersion from pom.xml
     */
    @JvmStatic
    fun init() {
        try {
            val pomFile = File(POM_FILE_PATH)
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(pomFile)
            doc.documentElement.normalize()
            val versionElement = doc.getElementsByTagName("version").item(0) as Element
            sdkVersion = versionElement.textContent
        } catch (e: Exception) {
            sdkVersion = "1.0.0-error"
            throw RuntimeException("Failed to read version from pom.xml", e)
        }
    }
}
