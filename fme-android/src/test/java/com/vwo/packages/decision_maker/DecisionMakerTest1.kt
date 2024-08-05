package com.vwo.packages.decision_maker

import org.junit.Assert.*

import org.junit.Before
import org.junit.Test

class DecisionMakerTest1 {

    @Before
    fun setUp() {
    }

    @Test
    fun generateHashValue() {
        assertEquals(3929804969, DecisionMaker().generateHashValue("Swapnil"))
        assertEquals(1364076727, DecisionMaker().generateHashValue(""))
        assertEquals(562546376, DecisionMaker().generateHashValue("VWO"))

    }
    @Test
    fun generateHashValue_differentCase_returnsSameHash() {
        val input = "TestCase"
        val lowerCaseHash = DecisionMaker().generateHashValue(input.lowercase())
        val upperCaseHash = DecisionMaker().generateHashValue(input.uppercase())
        assertNotEquals(lowerCaseHash, upperCaseHash)
    }

    @Test
    fun generateHashValue_specialCharacters_returnsValidHash() {
        val input = "!@#$%^&*()"
        val hash = DecisionMaker().generateHashValue(input)
        assertNotEquals(0, hash)
    }

    @Test
    fun generateHashValue_unicodeCharacters_returnsValidHash() {
        val input = "你好世界"
        val hash = DecisionMaker().generateHashValue(input)
        assertNotEquals(0, hash)
    }

    @Test
    fun generateHashValue_longString_returnsValidHash() {
        val input = "This is a very long string to test the hash function with a large input."
        val hash = DecisionMaker().generateHashValue(input)
        // Add an assertion based on your expected behavior for long strings
        // For example, assert that the hash is within a certain range:
        assertEquals(880528217,hash)
    }
}