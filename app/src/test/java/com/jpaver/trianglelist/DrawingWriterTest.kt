@file:Suppress("DEPRECATION")

package com.jpaver.trianglelist

import com.jpaver.trianglelist.writer.DxfFileWriter
import junit.framework.Assert.assertEquals
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class DrawingWriterTest {

    @Test
    fun testPolymorph(){
        val trilist = TriangleList()
        val writer = DxfFileWriter( trilist )

        //assertEquals( "IAMOVERRIDE.", writer.writeTriangle() )
        assertEquals( "IAMBASE.IAMOVERRIDED.", writer.getPolymorphString() )
        // 基底クラスの関数で、書き出し手順を定義して、個別にオーバーライドされた関数と、されてないのとを混合することができる。
    }

    @Test
    fun testSfcwriter() {
        TriangleList()

        val header = """ISO-10303-21;
                        HEADER;
                        FILE_DESCRIPTION(('SCADEC level2 feature_mode'),
                                '2;1');
                        FILE_NAME('test2.sfc',
                                '2021-4-8T13:8:0',
                                ('\X2\62C55F53\X0\'),
                                (''),
                                'SCADEC_API_Ver3.30${'$'}${'$'}2.0',
                                'CADWe''ll \X2\571F6728\X0\ 10',
                                '');
                        FILE_SCHEMA(('ASSOCIATIVE_DRAUGHTING'));
                        ENDSEC;
                        DATA;""".trimIndent().encodeToByteArray()

        //val headerHex = Hex.stringToBytes( "a".toByte() )

        val outputStream = BufferedOutputStream( FileOutputStream( File("test.txt" ) ) )
        outputStream.write( header )

        //val sfcWriter = SfcWriter( trilist, outputStream, "test.txt" )

        //assertEquals( true, sfcWriter.compare( header ) ):
    }


    @Test
    fun testDXFwriter() {
        val trilist = TriangleList()
        DxfFileWriter( trilist )
    }

}