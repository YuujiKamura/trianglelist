package com.jpaver.trianglelist

import org.junit.Test

class DXFwriterTest {

    @Test
    fun testDXFwriter() {
        val trilist = TriangleList()
        val dxfwr = DxfFileWriter( trilist )
    }

}