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
package com.vwo.packages.segmentation_evaluator.evaluators

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SegmentOperandEvaluator.evaluateDeviceModelDSL method.
 * 
 * This test class verifies the evaluation of device model DSL operands including:
 * 1. Wildcard patterns: "wildcard(*0)"
 * 2. Greater than equal to: "gte(1.0)"
 * 3. Various other operand types for comprehensive coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SegmentOperandEvaluatorTest {

    private val evaluator = SegmentOperandEvaluator()

    // ==================== WILDCARD TESTS ====================

    @Test
    fun testWildcardEndsWithZero_MatchingValue() {
        // Test case: app_version ends with "0"
        val result = evaluator.evaluateStringOperandDSL("wildcard(*0)", "1.2.0")
        assertTrue("Should match version ending with 0", result)
    }

    @Test
    fun testWildcardEndsWithZero_NonMatchingValue() {
        // Test case: app_version does not end with "0"
        val result = evaluator.evaluateStringOperandDSL("wildcard(*0)", "1.2.1")
        assertFalse("Should not match version not ending with 0", result)
    }

    @Test
    fun testWildcardEndsWithZero_ExactZero() {
        // Test case: exact "0"
        val result = evaluator.evaluateStringOperandDSL("wildcard(*0)", "0")
        assertTrue("Should match exact 0", result)
    }

    @Test
    fun testWildcardEndsWithZero_MultipleZeros() {
        // Test case: version ending with multiple zeros
        val result = evaluator.evaluateStringOperandDSL("wildcard(*0)", "1.0.0")
        assertTrue("Should match version ending with 0", result)
    }

    @Test
    fun testWildcardEndsWithZero_EmptyString() {
        // Test case: empty string
        val result = evaluator.evaluateStringOperandDSL("wildcard(*0)", "")
        assertFalse("Should not match empty string", result)
    }

    @Test
    fun testWildcardEndsWithZero_ContainsZeroButNotAtEnd() {
        // Test case: contains 0 but not at the end
        val result = evaluator.evaluateStringOperandDSL("wildcard(*0)", "1.0.1")
        assertFalse("Should not match when 0 is not at the end", result)
    }

    // ==================== GREATER THAN EQUAL TO TESTS ====================

    @Test
    fun testGreaterThanEqualTo_ExactMatch() {
        // Test case: exact match with 1.0
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "1.0")
        assertTrue("Should match exact version 1.0", result)
    }

    @Test
    fun testGreaterThanEqualTo_GreaterVersion() {
        // Test case: greater version
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "1.1")
        assertTrue("Should match greater version 1.1", result)
    }

    @Test
    fun testGreaterThanEqualTo_LesserVersion() {
        // Test case: lesser version
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "0.9")
        assertFalse("Should not match lesser version 0.9", result)
    }

    @Test
    fun testGreaterThanEqualTo_MajorVersionGreater() {
        // Test case: major version greater
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "2.0")
        assertTrue("Should match greater major version 2.0", result)
    }

    @Test
    fun testGreaterThanEqualTo_ComplexVersionGreater() {
        // Test case: complex version comparison
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "1.2.3")
        assertTrue("Should match greater complex version 1.2.3", result)
    }

    @Test
    fun testGreaterThanEqualTo_ComplexVersionLesser() {
        // Test case: complex version lesser
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "0.9.9")
        assertFalse("Should not match lesser complex version 0.9.9", result)
    }

    @Test
    fun testGreaterThanEqualTo_IntegerComparison() {
        // Test case: integer comparison
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "2")
        assertTrue("Should match greater integer version 2", result)
    }

    @Test
    fun testGreaterThanEqualTo_ZeroComparison() {
        // Test case: comparison with zero
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "0")
        assertFalse("Should not match version 0", result)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun testGreaterThanEqualTo_InvalidVersionString() {
        // Test case: invalid version string
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "abc")
        assertFalse("Should not match invalid version string", result)
    }

    @Test
    fun testWildcard_SpecialCharacters() {
        // Test case: wildcard with special characters
        val result = evaluator.evaluateStringOperandDSL("wildcard(*-beta)", "1.0.0-beta")
        assertTrue("Should match version with beta suffix", result)
    }

    @Test
    fun testWildcard_CaseSensitive() {
        // Test case: wildcard case sensitivity
        val result = evaluator.evaluateStringOperandDSL("wildcard(*Beta)", "1.0.0-beta")
        assertFalse("Should be case sensitive and not match", result)
    }

    // ==================== ADDITIONAL OPERAND TESTS ====================

    @Test
    fun testEquals_ExactMatch() {
        // Test case: exact equality
        val result = evaluator.evaluateStringOperandDSL("1.0.0", "1.0.0")
        assertTrue("Should match exact version", result)
    }

    @Test
    fun testEquals_NoMatch() {
        // Test case: no equality match
        val result = evaluator.evaluateStringOperandDSL("1.0.0", "1.0.1")
        assertFalse("Should not match different versions", result)
    }

    @Test
    fun testGreaterThan_Success() {
        // Test case: greater than
        val result = evaluator.evaluateStringOperandDSL("gt(1.0)", "1.1")
        assertTrue("Should match greater version", result)
    }

    @Test
    fun testGreaterThan_EqualValue() {
        // Test case: greater than with equal value
        val result = evaluator.evaluateStringOperandDSL("gt(1.0)", "1.0")
        assertFalse("Should not match equal version for gt", result)
    }

    @Test
    fun testLessThan_Success() {
        // Test case: less than
        val result = evaluator.evaluateStringOperandDSL("lt(1.0)", "0.9")
        assertTrue("Should match lesser version", result)
    }

    @Test
    fun testLessThan_EqualValue() {
        // Test case: less than with equal value
        val result = evaluator.evaluateStringOperandDSL("lt(1.0)", "1.0")
        assertFalse("Should not match equal version for lt", result)
    }

    @Test
    fun testLessThanEqualTo_Success() {
        // Test case: less than equal to
        val result = evaluator.evaluateStringOperandDSL("lte(1.0)", "0.9")
        assertTrue("Should match lesser version", result)
    }

    @Test
    fun testLessThanEqualTo_EqualValue() {
        // Test case: less than equal to with equal value
        val result = evaluator.evaluateStringOperandDSL("lte(1.0)", "1.0")
        assertTrue("Should match equal version for lte", result)
    }

    @Test
    fun testRegex_Match() {
        // Test case: regex pattern matching
        val result = evaluator.evaluateStringOperandDSL("regex(1\\.\\d+)", "1.5")
        assertTrue("Should match regex pattern", result)
    }

    @Test
    fun testRegex_NoMatch() {
        // Test case: regex pattern not matching
        val result = evaluator.evaluateStringOperandDSL("regex(2\\.\\d+)", "1.5")
        assertFalse("Should not match regex pattern", result)
    }

    // ==================== WILDCARD VARIATIONS ====================

    @Test
    fun testWildcardStartsWith_Match() {
        // Test case: starts with pattern
        val result = evaluator.evaluateStringOperandDSL("wildcard(1.*)", "1.2.3")
        assertTrue("Should match version starting with 1", result)
    }

    @Test
    fun testWildcardStartsWith_NoMatch() {
        // Test case: starts with pattern no match
        val result = evaluator.evaluateStringOperandDSL("wildcard(2.*)", "1.2.3")
        assertFalse("Should not match version not starting with 2", result)
    }

    @Test
    fun testWildcardContains_Match() {
        // Test case: contains pattern
        val result = evaluator.evaluateStringOperandDSL("wildcard(*beta*)", "1.0.0-beta-1")
        assertTrue("Should match version containing beta", result)
    }

    @Test
    fun testWildcardContains_NoMatch() {
        // Test case: contains pattern no match
        val result = evaluator.evaluateStringOperandDSL("wildcard(*alpha*)", "1.0.0-beta-1")
        assertFalse("Should not match version not containing alpha", result)
    }

    // ==================== COMPREHENSIVE VERSION COMPARISON TESTS ====================

    @Test
    fun testComplexVersionComparison_MultipleDecimalPoints() {
        // Test case: complex version with multiple decimal points
        val result = evaluator.evaluateStringOperandDSL("gte(1.2.3)", "1.2.3.4")
        assertTrue("Should handle complex version comparison", result)
    }

    @Test
    fun testVersionComparison_DifferentFormats() {
        // Test case: different version formats
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "1")
        assertTrue("Should handle different version formats", result)
    }

    @Test
    fun testVersionComparison_LeadingZeros() {
        // Test case: versions with leading zeros
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "01.0")
        assertTrue("Should handle versions with leading zeros", result)
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    fun testEmptyDSL() {
        // Test case: empty DSL
        val result = evaluator.evaluateStringOperandDSL("", "1.0.0")
        assertFalse("Should handle empty DSL gracefully", result)
    }

    @Test
    fun testEmptyValue() {
        // Test case: empty value
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "")
        assertFalse("Should handle empty value gracefully", result)
    }

    @Test
    fun testNullLikeValue() {
        // Test case: null-like value
        val result = evaluator.evaluateStringOperandDSL("gte(1.0)", "null")
        assertFalse("Should handle null-like string gracefully", result)
    }

    @Test
    fun testMalformedDSL() {
        // Test case: malformed DSL
        val result = evaluator.evaluateStringOperandDSL("gte(", "1.0.0")
        assertFalse("Should handle malformed DSL gracefully", result)
    }

    // ==================== LESS THAN EQUAL TO (LTE) TESTS ====================

    @Test
    fun testLessThanEqualTo_ExactMatch() {
        // Test case: exact match with 1.1
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.1")
        assertTrue("Should match exact version 1.1", result)
    }

    @Test
    fun testLessThanEqualTo_LesserVersion() {
        // Test case: lesser version
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.0")
        assertTrue("Should match lesser version 1.0", result)
    }

    @Test
    fun testLessThanEqualTo_GreaterVersion() {
        // Test case: greater version
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.2")
        assertFalse("Should not match greater version 1.2", result)
    }

    @Test
    fun testLessThanEqualTo_MajorVersionLesser() {
        // Test case: major version lesser
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "0.9")
        assertTrue("Should match lesser major version 0.9", result)
    }

    @Test
    fun testLessThanEqualTo_ComplexVersionLesser() {
        // Test case: complex version lesser
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.0.9")
        assertTrue("Should match lesser complex version 1.0.9", result)
    }

    @Test
    fun testLessThanEqualTo_ComplexVersionGreater() {
        // Test case: complex version greater
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.1.1")
        assertFalse("Should not match greater complex version 1.1.1", result)
    }

    @Test
    fun testLessThanEqualTo_IntegerComparison() {
        // Test case: integer comparison
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1")
        assertTrue("Should match lesser integer version 1", result)
    }

    @Test
    fun testLessThanEqualTo_ZeroComparison() {
        // Test case: comparison with zero
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "0")
        assertTrue("Should match version 0", result)
    }

    @Test
    fun testLessThanEqualTo_EdgeCase() {
        // Test case: edge case with 1.09 vs 1.1
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.09")
        assertFalse("Should not match version 1.09 which is greater than 1.1", result)
    }

    @Test
    fun testLessThanEqualTo_InvalidVersionString() {
        // Test case: invalid version string
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "abc")
        assertTrue("Should match invalid version string (treated as less than)", result)
    }

    // ==================== LESS THAN (LT) TESTS ====================

    @Test
    fun testLessThan_ExactMatch() {
        // Test case: exact match with 1
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "1")
        assertFalse("Should not match exact version 1", result)
    }

    @Test
    fun testLessThan_LesserVersion() {
        // Test case: lesser version
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "0.9")
        assertTrue("Should match lesser version 0.9", result)
    }

    @Test
    fun testLessThan_GreaterVersion() {
        // Test case: greater version
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "1.1")
        assertFalse("Should not match greater version 1.1", result)
    }

    @Test
    fun testLessThan_ZeroComparison() {
        // Test case: comparison with zero
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "0")
        assertTrue("Should match version 0", result)
    }

    @Test
    fun testLessThan_ComplexVersionLesser() {
        // Test case: complex version lesser
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "0.9.9")
        assertTrue("Should match lesser complex version 0.9.9", result)
    }

    @Test
    fun testLessThan_ComplexVersionGreater() {
        // Test case: complex version greater
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "1.0.1")
        assertFalse("Should not match greater complex version 1.0.1", result)
    }

    @Test
    fun testLessThan_FloatComparison() {
        // Test case: float comparison
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "0.5")
        assertTrue("Should match lesser float version 0.5", result)
    }

    @Test
    fun testLessThan_EdgeCaseWithDecimals() {
        // Test case: edge case with decimals
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "0.99")
        assertTrue("Should match version 0.99 which is less than 1", result)
    }

    @Test
    fun testLessThan_InvalidVersionString() {
        // Test case: invalid version string
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "abc")
        assertTrue("Should match invalid version string (treated as less than)", result)
    }

    @Test
    fun testLessThan_NegativeVersion() {
        // Test case: negative version (edge case)
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "-1")
        assertTrue("Should match negative version -1", result)
    }

    // ==================== GREATER THAN (GT) TESTS ====================

    @Test
    fun testGreaterThan_ExactMatch() {
        // Test case: exact match with 1.1
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.1")
        assertFalse("Should not match exact version 1.1", result)
    }

    @Test
    fun testGreaterThan_GreaterVersion() {
        // Test case: greater version
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.2")
        assertTrue("Should match greater version 1.2", result)
    }

    @Test
    fun testGreaterThan_LesserVersion() {
        // Test case: lesser version
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.0")
        assertFalse("Should not match lesser version 1.0", result)
    }

    @Test
    fun testGreaterThan_MajorVersionGreater() {
        // Test case: major version greater
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "2.0")
        assertTrue("Should match greater major version 2.0", result)
    }

    @Test
    fun testGreaterThan_ComplexVersionGreater() {
        // Test case: complex version greater
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.1.1")
        assertTrue("Should match greater complex version 1.1.1", result)
    }

    @Test
    fun testGreaterThan_ComplexVersionLesser() {
        // Test case: complex version lesser
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.0.9")
        assertFalse("Should not match lesser complex version 1.0.9", result)
    }

    @Test
    fun testGreaterThan_IntegerComparison() {
        // Test case: integer comparison
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "2")
        assertTrue("Should match greater integer version 2", result)
    }

    @Test
    fun testGreaterThan_ZeroComparison() {
        // Test case: comparison with zero
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "0")
        assertFalse("Should not match version 0", result)
    }

    @Test
    fun testGreaterThan_EdgeCaseMinorIncrement() {
        // Test case: edge case with minor increment
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.11")
        assertTrue("Should match version 1.11 which is greater than 1.1", result)
    }

    @Test
    fun testGreaterThan_InvalidVersionString() {
        // Test case: invalid version string
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "abc")
        assertFalse("Should not match invalid version string", result)
    }

    // ==================== COMBINED SCENARIO TESTS ====================

    @Test
    fun testCombinedScenario_LteWithComplexVersion() {
        // Test case: lte with complex version comparison
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "1.1.0")
        assertTrue("Should match 1.1.0 for lte(1.1) as 1.1.0 == 1.1", result)
    }

    @Test
    fun testCombinedScenario_LtWithFloatPrecision() {
        // Test case: lt with float precision
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "0.999")
        assertTrue("Should match 0.999 for lt(1)", result)
    }

    @Test
    fun testCombinedScenario_GtWithVersionBoundary() {
        // Test case: gt with version boundary
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "1.10")
        assertTrue("Should match 1.10 for gt(1.1) as 1.10 > 1.1", result)
    }

    @Test
    fun testCombinedScenario_AllOperatorsWithSameValue() {
        // Test case: all operators with same value 1.5
        val testValue = "1.5"
        
        assertTrue("lte(1.5) should match 1.5", evaluator.evaluateStringOperandDSL("lte(1.5)", testValue))
        assertFalse("lt(1.5) should not match 1.5", evaluator.evaluateStringOperandDSL("lt(1.5)", testValue))
        assertTrue("gte(1.5) should match 1.5", evaluator.evaluateStringOperandDSL("gte(1.5)", testValue))
        assertFalse("gt(1.5) should not match 1.5", evaluator.evaluateStringOperandDSL("gt(1.5)", testValue))
    }

    // ==================== EDGE CASES FOR NEW OPERATORS ====================

    @Test
    fun testEdgeCase_EmptyStringWithLte() {
        // Test case: empty string with lte
        val result = evaluator.evaluateStringOperandDSL("lte(1.1)", "")
        assertTrue("Should handle empty string gracefully for lte", result)
    }

    @Test
    fun testEdgeCase_EmptyStringWithLt() {
        // Test case: empty string with lt
        val result = evaluator.evaluateStringOperandDSL("lt(1)", "")
        assertTrue("Should handle empty string gracefully for lt", result)
    }

    @Test
    fun testEdgeCase_EmptyStringWithGt() {
        // Test case: empty string with gt
        val result = evaluator.evaluateStringOperandDSL("gt(1.1)", "")
        assertFalse("Should handle empty string gracefully for gt", result)
    }

    @Test
    fun testEdgeCase_MalformedDSLWithNewOperators() {
        // Test case: malformed DSL with new operators
        assertFalse("Should handle malformed lte DSL", evaluator.evaluateStringOperandDSL("lte(", "1.0"))
        assertFalse("Should handle malformed lt DSL", evaluator.evaluateStringOperandDSL("lt(", "1.0"))
        assertFalse("Should handle malformed gt DSL", evaluator.evaluateStringOperandDSL("gt(", "1.0"))
    }

    @Test
    fun testEdgeCase_VeryLongVersionNumbers() {
        // Test case: very long version numbers
        val longVersion = "1.2.3.4.5.6.7.8.9.10"
        assertTrue("Should handle long version for lte", evaluator.evaluateStringOperandDSL("lte(2.0)", longVersion))
        assertTrue("Should handle long version for lt", evaluator.evaluateStringOperandDSL("lt(2)", longVersion))
        assertFalse("Should handle long version for gt", evaluator.evaluateStringOperandDSL("gt(2.0)", longVersion))
    }
} 