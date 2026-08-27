package com.jpaver.trianglelist

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.TriangleList
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

/**
 * 画面と図面 (書き出し) の配置が一致すること。
 *
 * 2026-08-27 user「基本アプリと図面が食い違うのはバグだと思っていい。期待値と違うわけだから」。
 *
 * 正は**紙 (書き出し)**、画面はそのプレビュー。したがって配置は元モデルが書き出し用の
 * 文字サイズで確定し、画面はその結果を描くだけにする。画面用の文字サイズ (px) で
 * 画面のコピーだけを別に整えると、同じ図面が画面と DXF で違う配置になる。
 */
class ScreenExportParityTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("repo root not found")
    }

    private fun realList(): TriangleList = CsvCodec.build(
        CsvCodec.parse(File(repoRoot(), "samples/8.25_bad.csv").readText(java.nio.charset.Charset.forName("MS932"))),
    )

    /** 配置の指紋 (番号位置 + 寸法の horizontal)。座標は縮尺で変わるので horizontal と相対位置で見る。 */
    private fun placement(list: TriangleList): String = buildString {
        list.forEachItemIndexed { num, s ->
            append(num).append(':')
            append(s.dimHorizontal.a).append(s.dimHorizontal.b).append(s.dimHorizontal.c).append(s.dimHorizontal.s)
            append('|')
        }
    }

    @Test
    fun `画面用に attach したコピーと元モデルの配置が一致する`() {
        val source = realList()
        source.arrangeLabelsForDrawing()
        val expected = placement(source)

        // MyView.setTriangleList と同じ: 確定済みモデルを clone して画面用に scale する
        val viewCopy = source.clone()
        viewCopy.attachToTheView(PointXY(0f, 0f), 47.6f, 30f)

        assertEquals(expected, placement(viewCopy), "画面用コピーの配置が元モデルと違う")
    }

    @Test
    fun `書き出しに渡すコピーも同じ配置になる`() {
        val source = realList()
        source.arrangeLabelsForDrawing()
        val expected = placement(source)

        // saveDXF と同じ: trianglelist.clone() を writer に渡す
        assertEquals(expected, placement(source.clone()), "書き出し用コピーの配置が元モデルと違う")
    }

    @Test
    fun `配置の確定は繰り返しても変わらない`() {
        val source = realList()
        source.arrangeLabelsForDrawing()
        val once = placement(source)
        source.arrangeLabelsForDrawing()

        assertEquals(once, placement(source), "確定を 2 回呼ぶと配置が動く")
    }
}
