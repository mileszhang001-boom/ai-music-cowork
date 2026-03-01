package com.example.layer1.utils

import org.junit.Assert.*
import org.junit.Test

class ConfidenceValidatorTest {

    private val validator = ConfidenceValidator(0.6)

    @Test
    fun testNormalHighConfidence() {
        val result = validator.validate(0.85)
        assertTrue(result.isValid)
        assertEquals("85.00%", result.formattedConfidence)
        assertEquals("Confidence acceptable.", result.message)
    }

    @Test
    fun testNormalLowConfidence() {
        val result = validator.validate(0.4)
        assertFalse(result.isValid)
        assertEquals("40.00%", result.formattedConfidence)
        assertEquals("Low confidence detected.", result.message)
    }

    @Test
    fun testBoundaryThresholdExact() {
        val result = validator.validate(0.6)
        assertTrue(result.isValid)
    }

    @Test
    fun testBoundaryThresholdJustBelow() {
        val result = validator.validate(0.599)
        assertFalse(result.isValid)
    }

    @Test
    fun testInvalidHigh() {
        val result = validator.validate(1.5)
        assertFalse(result.isValid)
        assertEquals("N/A", result.formattedConfidence)
        assertTrue(result.message.contains("Invalid"))
    }

    @Test
    fun testInvalidLow() {
        val result = validator.validate(-0.1)
        assertFalse(result.isValid)
        assertTrue(result.message.contains("Invalid"))
    }

    @Test
    fun testDynamicThreshold() {
        validator.setThreshold(0.9)
        val result = validator.validate(0.8)
        assertFalse("Should be invalid under new higher threshold", result.isValid)
        
        validator.setThreshold(0.2)
        val result2 = validator.validate(0.3)
        assertTrue("Should be valid under new lower threshold", result2.isValid)
    }
}
