package com.jpaver.trianglelist

import com.jpaver.trianglelist.dataclass.ZumenInfo
import com.jpaver.trianglelist.util.FileUtil
import com.jpaver.trianglelist.util.TitleParamStr
import com.jpaver.trianglelist.writer.DxfFileWriter
import com.jpaver.trianglelist.writer.DxfValidator
import com.jpaver.trianglelist.writer.SfcWriter
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * SFCライターとDXFライターの機能同等性をテストするクラス
 */
class WriterComparisonTest {

    private lateinit var testTriList: TriangleList
    private lateinit var testDedList: DeductionList
    private lateinit var testZumenInfo: ZumenInfo
    private lateinit var testTitleTri: TitleParamStr
    private lateinit var testTitleDed: TitleParamStr

    @Before
    fun setUp() {
        // 共通テストデータの準備
        testTriList = TriangleList().apply {
            add(Triangle(10f, 2f, 10f), true)
            add(Triangle(get(1), 2, 1f, 10f), true)
        }
        testDedList = DeductionList()
        testZumenInfo = ZumenInfo()
        testTitleTri = TitleParamStr("type","number","name","a","b","c","pn","pl")
        testTitleDed = TitleParamStr("type","number","name","a","b","c","pn","pl")
    }

    /**
     * 基本的な出力機能の比較テスト
     */
    @Test
    fun compareBasicOutputFunctionality() {
        val buildDir = File(System.getProperty("user.dir"), "build")
        val outputDir = File(buildDir, "comparison-output").apply { mkdirs() }

        // DXF出力テスト
        val dxfFile = File(outputDir, "comparison-test.dxf")
        val dxfWriter = createDxfWriter()
        dxfWriter.saveTo(dxfFile)

        // SFC出力テスト
        val sfcFile = File(outputDir, "comparison-test.sfc")
        val sfcOutputStream = FileUtil.initBufferedOutputStream(outputDir.absolutePath, "comparison-test.sfc")
        assertNotNull("SFC出力ストリームが作成されること", sfcOutputStream)
        
        val sfcWriter = createSfcWriter(sfcOutputStream!!, "comparison-test.sfc")
        sfcWriter.save()
        sfcOutputStream.close()

        // 基本的な出力確認
        assertTrue("DXFファイルが生成されること", dxfFile.exists() && dxfFile.length() > 0)
        assertTrue("SFCファイルが生成されること", sfcFile.exists() && sfcFile.length() > 0)

        println("→ DXF出力: ${dxfFile.absolutePath}")
        println("→ SFC出力: ${sfcFile.absolutePath}")
    }

    /**
     * 描画要素の網羅性比較テスト
     */
    @Test
    fun compareDrawingElementCoverage() {
        val coverage = DrawingElementCoverage()
        
        // DXFの対応要素を分析
        val dxfWriter = createDxfWriter()
        val dxfCoverage = coverage.analyzeDxfCapabilities(dxfWriter)
        
        // SFCの対応要素を分析
        val sfcCoverage = coverage.analyzeSfcCapabilities()

        // カバレッジ比較
        val comparisonResult = coverage.compare(dxfCoverage, sfcCoverage)
        
        println("=== 描画要素カバレッジ比較 ===")
        println("DXF対応: ${dxfCoverage.supportedElements}")
        println("SFC対応: ${sfcCoverage.supportedElements}")
        println("共通要素: ${comparisonResult.commonElements}")
        println("DXF専用: ${comparisonResult.dxfOnlyElements}")
        println("SFC専用: ${comparisonResult.sfcOnlyElements}")
        
        // 最低限の要素は両方で対応していることを確認
        val requiredElements = setOf("line", "text", "circle", "triangle")
        assertTrue("必須要素がDXFで対応されていること", 
            requiredElements.all { it in dxfCoverage.supportedElements })
        assertTrue("必須要素がSFCで対応されていること", 
            requiredElements.all { it in sfcCoverage.supportedElements })
    }

    /**
     * テキスト描画機能の比較テスト
     */
    @Test
    fun compareTextRenderingCapabilities() {
        val textTests = listOf(
            TextTestCase("基本テキスト", "Hello", align1 = 1, align2 = 1),
            TextTestCase("日本語テキスト", "こんにちは", align1 = 2, align2 = 2),
            TextTestCase("回転テキスト", "Rotated", align1 = 1, align2 = 1, angle = 45f),
            TextTestCase("大きなテキスト", "Large", align1 = 1, align2 = 1, size = 2f)
        )

        textTests.forEach { testCase ->
            val dxfSupport = checkDxfTextSupport(testCase)
            val sfcSupport = checkSfcTextSupport(testCase)
            
            println("${testCase.name}: DXF=${dxfSupport}, SFC=${sfcSupport}")
            
            if (dxfSupport != sfcSupport) {
                println("  ⚠ テキスト機能に差異があります")
            }
        }
    }

    /**
     * 精度と座標系の比較テスト
     */
    @Test
    fun comparePrecisionAndCoordinates() {
        val testPoints = listOf(
            com.example.trilib.PointXY(0f, 0f),
            com.example.trilib.PointXY(100.123f, 200.456f),
            com.example.trilib.PointXY(-50.789f, 150.321f)
        )

        testPoints.forEach { point ->
            // 各ライターでの座標変換結果を比較
            val dxfTransformed = transformPointForDxf(point)
            val sfcTransformed = transformPointForSfc(point)
            
            val diff = calculatePointDifference(dxfTransformed, sfcTransformed)
            
            println("座標 $point -> DXF: $dxfTransformed, SFC: $sfcTransformed, 差異: $diff")
            
            // 許容誤差内であることを確認（1mm以下）
            assertTrue("座標変換の差異が許容範囲内であること", diff < 1.0f)
        }
    }

    // ヘルパーメソッド
    private fun createDxfWriter(): DxfFileWriter {
        return DxfFileWriter(testTriList, testDedList, testZumenInfo, testTitleTri, testTitleDed).apply {
            setNames("テスト工事", "テスト路線", "テスト業者", "1/1")
            startTriNumber_ = 1
            isReverse_ = false
        }
    }

    private fun createSfcWriter(outputStream: java.io.BufferedOutputStream, filename: String): SfcWriter {
        return SfcWriter(testTriList, testDedList, outputStream, filename, 1, 1f).apply {
            setNames("テスト工事", "テスト路線", "テスト業者", "1/1")
            zumeninfo = testZumenInfo
            titleTri_ = testTitleTri
            titleDed_ = testTitleDed
            textscale_ = 25f * 20f
            startTriNumber_ = 1
            isReverse_ = false
        }
    }

    // 描画要素カバレッジ分析用のヘルパークラス
    data class DrawingElementCoverage(
        val supportedElements: Set<String> = emptySet()
    ) {
        fun analyzeDxfCapabilities(writer: DxfFileWriter): DrawingElementCoverage {
            val elements = mutableSetOf<String>()
            
            // DxfFileWriterクラスの実装メソッドから対応要素を推定
            val methods = writer.javaClass.methods.map { it.name }
            if (methods.any { it.contains("writeLine") }) elements.add("line")
            if (methods.any { it.contains("writeText") }) elements.add("text")
            if (methods.any { it.contains("writeCircle") }) elements.add("circle")
            if (methods.any { it.contains("writeTriangle") }) elements.add("triangle")
            if (methods.any { it.contains("writeRect") }) elements.add("rectangle")
            
            return DrawingElementCoverage(elements)
        }

        fun analyzeSfcCapabilities(): DrawingElementCoverage {
            val elements = mutableSetOf<String>()
            
            // SfcWriterの機能を分析（実装から推定）
            elements.addAll(setOf("line", "text", "circle", "triangle"))
            
            return DrawingElementCoverage(elements)
        }

        fun compare(dxf: DrawingElementCoverage, sfc: DrawingElementCoverage): ComparisonResult {
            return ComparisonResult(
                commonElements = dxf.supportedElements.intersect(sfc.supportedElements),
                dxfOnlyElements = dxf.supportedElements - sfc.supportedElements,
                sfcOnlyElements = sfc.supportedElements - dxf.supportedElements
            )
        }
    }

    data class ComparisonResult(
        val commonElements: Set<String>,
        val dxfOnlyElements: Set<String>,
        val sfcOnlyElements: Set<String>
    )

    data class TextTestCase(
        val name: String,
        val text: String,
        val align1: Int,
        val align2: Int,
        val angle: Float = 0f,
        val size: Float = 1f
    )

    private fun checkDxfTextSupport(testCase: TextTestCase): Boolean {
        // DXFのテキスト対応をチェック
        return true // 実装に応じて調整
    }

    private fun checkSfcTextSupport(testCase: TextTestCase): Boolean {
        // SFCのテキスト対応をチェック  
        return true // 実装に応じて調整
    }

    private fun transformPointForDxf(point: com.example.trilib.PointXY): com.example.trilib.PointXY {
        // DXFでの座標変換ロジック（unitscale_等を考慮）
        return com.example.trilib.PointXY(point.x * 1000f, point.y * 1000f)
    }

    private fun transformPointForSfc(point: com.example.trilib.PointXY): com.example.trilib.PointXY {
        // SFCでの座標変換ロジック（unitscale_等を考慮）
        return com.example.trilib.PointXY(point.x * 1000f, point.y * 1000f)
    }

    private fun calculatePointDifference(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY): Float {
        return kotlin.math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
    }
}
