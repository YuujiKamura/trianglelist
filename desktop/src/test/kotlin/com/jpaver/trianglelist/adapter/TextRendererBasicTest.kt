package com.jpaver.trianglelist.adapter

import com.jpaver.trianglelist.parser.DxfParser
import com.jpaver.trianglelist.parser.DxfText
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * DxfParserとTextRendererの基本的なテスト
 */
class TextRendererBasicTest {

    private val testDxfPath = "C:\\Users\\yuuji\\StudioProjects\\test.dxf"

    @Test
    fun testDxfParserExists() {
        // DxfParserが作成できることを確認
        val parser = DxfParser()
        assertNotNull("DxfParserが作成できません", parser)
    }

    @Test
    fun testDxfFileExists() {
        // test.dxfファイルが存在することを確認
        val file = File(testDxfPath)
        assertTrue("テストファイルが存在しません: $testDxfPath", file.exists())
        assertTrue("ファイルが読み取り可能ではありません", file.canRead())
        assertTrue("ファイルが空です", file.length() > 0)
    }

    @Test
    fun testParseMethod() {
        // DxfParserのparseメソッドが存在し、呼び出せることを確認
        val parser = DxfParser()
        val result = parser.parse("")
        assertNotNull("parse結果がnullです", result)
        
        // 空の文字列をパースした場合、空のリストが返ることを確認
        assertEquals("空の文字列で線が見つかりました", 0, result.lines.size)
        assertEquals("空の文字列で円が見つかりました", 0, result.circles.size)
        assertEquals("空の文字列でテキストが見つかりました", 0, result.texts.size)
    }

    @Test
    fun testParseRealDxfFile() {
        // test.dxfファイルが存在することを確認
        val file = File(testDxfPath)
        assertTrue("テストファイルが存在しません: $testDxfPath", file.exists())
        
        // DXFファイルを読み込んでパース
        val dxfContent = file.readText()
        val parser = DxfParser()
        val result = parser.parse(dxfContent)
        
        // パース結果の基本チェック
        assertNotNull("パース結果がnullです", result)
        
        println("DXF parse results:")
        println("  Lines: ${result.lines.size}")
        println("  Circles: ${result.circles.size}")
        println("  Texts: ${result.texts.size}")
        println("  Header: ${result.header}")
        
        // TEXTエンティティが含まれていることを確認
        val textEntities = result.texts
        assertTrue("TEXTエンティティが見つかりません", textEntities.isNotEmpty())
        
        println("Found ${textEntities.size} TEXT entities:")
        textEntities.forEach { text ->
            println("  Text: '${text.text}' at (${text.x}, ${text.y}) align(${text.alignH}, ${text.alignV})")
        }
    }

    @Test
    fun testTextRendererCreation() {
        // TextRendererが作成できることを確認
        val textRenderer = TextRenderer()
        assertNotNull("TextRendererが作成できません", textRenderer)
    }
}
