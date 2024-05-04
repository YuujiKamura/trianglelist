package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.FileUtil
import org.junit.Assert
import org.junit.Test

class DxfWriterTest {

    @Test
    fun testTestSize() {
        val tri = Triangle(5f, 5f, 5f)
        val trilist = TriangleList(tri)
        val dedlist = DeductionList()

        val dxfwriter = DxfFileWriter(trilist, dedlist)

        Assert.assertEquals(0.25f, dxfwriter.textscale_ )

        val path = "testDxf"
        val filename = "test.dxf"
        //val fullPath = "$path${File.separator}$filename"
        val writer = FileUtil.initBufferedWriter( path, filename )
        if( writer==null ) return

        dxfwriter.writer = writer
        dxfwriter.save()
        writer.close()
    }
}