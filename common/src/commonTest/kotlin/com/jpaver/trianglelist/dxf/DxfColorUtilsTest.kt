package com.jpaver.trianglelist.dxf

import kotlin.test.*

/**
 * DXF色システムのテスト
 */
class DxfColorUtilsTest {

    @Test
    fun test_basic_aci_colors() {
        val red = DxfColor.fromAci(1)
        val yellow = DxfColor.fromAci(2)
        val green = DxfColor.fromAci(3)
        
        assertEquals(1, red.aciIndex)
        assertEquals("Red", red.name)
        assertEquals(false, red.isBackgroundInverted)
        
        assertEquals(2, yellow.aciIndex)
        assertEquals("Yellow", yellow.name)
        assertEquals(false, yellow.isBackgroundInverted)
        
        assertEquals(3, green.aciIndex)
        assertEquals("Green", green.name)
        assertEquals(false, green.isBackgroundInverted)
    }

    @Test
    fun test_background_inverted_color() {
        val white = DxfColor.fromAci(7)
        
        assertEquals(7, white.aciIndex)
        assertEquals("White", white.name)
        assertTrue(white.isBackgroundInverted)
    }

    @Test
    fun test_byblock_color() {
        val byBlock = DxfColor.fromAci(0)
        
        assertEquals(0, byBlock.aciIndex)
        assertEquals("ByBlock", byBlock.name)
        assertEquals(false, byBlock.isBackgroundInverted)
    }

    @Test
    fun test_extended_colors() {
        val gray = DxfColor.fromAci(8)
        val lightGray = DxfColor.fromAci(9)
        val lightRed = DxfColor.fromAci(11)
        
        assertEquals(8, gray.aciIndex)
        assertEquals("Gray", gray.name)
        
        assertEquals(9, lightGray.aciIndex)
        assertEquals("LightGray", lightGray.name)
        
        assertEquals(11, lightRed.aciIndex)
        assertEquals("LightRed", lightRed.name)
    }

    @Test
    fun test_unknown_color() {
        val unknown = DxfColor.fromAci(999)
        
        assertEquals(999, unknown.aciIndex)
        assertEquals("Unknown", unknown.name)
        assertEquals(false, unknown.isBackgroundInverted)
    }
} 