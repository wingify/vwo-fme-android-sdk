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
package com.vwo.models

import com.vwo.models.request.visitor.Visitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VisitorTest {

    private lateinit var visitor: Visitor

    @Before
    fun setup() {
        visitor = Visitor()
    }

    @Test
    fun `test default values`() {
        assertNull(visitor.props) // Assert on the actual property name
    }

    @Test
    fun `test setting and getting props`() { // Corrected test name
        val testProps= mutableMapOf<String, Any>( // Use mutableMapOf
            "key1" to "value1",
            "key2" to 123
        )
        visitor.setProps(testProps) // Use the setProps method
        assertEquals(testProps, visitor.props) // Assert on the props property
    }

    @Test
    fun `test setting props to null`() { // Added test for setting props to null
        val testProps = mutableMapOf<String, Any>(
            "key1" to "value1"
        )
        visitor.setProps(testProps)
        assertEquals(testProps, visitor.props)

        visitor.setProps(null)
        assertNull(visitor.props)
    }

    @Test
    fun `test setProps method chaining`() { // Added test for method chaining
        val testProps= mutableMapOf<String, Any>(
            "key1" to "value1"
        )
        val returnedVisitor = visitor.setProps(testProps)
        assertEquals(visitor, returnedVisitor) // Assert that the method returns the visitor instance
        assertEquals(testProps, visitor.props) // Also verify props were set
    }

    @Test
    fun `test setting and getting all properties`() {
        val testProps = mutableMapOf<String, Any>( // Use mutableMapOf
            "key1" to "value1",
            "key2" to 123
        )

        visitor.apply {
            setProps(testProps) // Use the setProps method
        }

        assertEquals(testProps, visitor.props) // Assert on the props property
    }
}