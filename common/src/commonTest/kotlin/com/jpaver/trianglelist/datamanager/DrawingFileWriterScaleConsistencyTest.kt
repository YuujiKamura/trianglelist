package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.viewmodel.TitleParamStr
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 2026-08-13 発見の再現テスト: DXF は writeTopTitle/writeDrawingFrame を常に scale=1 で呼び、
 * SFC は scale=printscale_ (画面縮尺の実値、多くは 1 未満) を直接渡す。この 2 経路は数学的に
 * 等価のはずだったが、writeTopTitle の内部で TextSizePolicy.resolve(role, scale) が返す
 * scale 依存の量 (areaFs 由来の subtitleLineGap) を、scale 非依存の位置アンカー (ty) と
 * 算術演算 (引き算) で混ぜてから、その結果をさらに PointXY(x, y, scale) で scale 倍しているため、
 * scale が実質 2 重に掛かる。scale=1 (DXF) では 1^2=1 なので症状が出ず、scale≠1 (SFC) でだけ
 * 表題行の縦位置が正しい値からズレる。
 *
 * 検証方法: writeTopTitle を異なる scale で呼び、タイトルとサブタイトル行の縦方向ギャップを
 * scale で割った値が scale に依らず一定であること (= 線形性) を確認する。線形なら
 * gap(scale)/scale は定数のはず。2 重掛けバグがあると gap(scale)/scale は scale に比例して
 * 変化してしまう。
 */
class DrawingFileWriterScaleConsistencyTest {

    private class CapturingWriter : DrawingFileWriter() {
        override var trilist_: TriangleList = TriangleList()
        override var dedlist_: DeductionList = DeductionList()
        override var zumeninfo: ZumenInfo = ZumenInfo()
        override var titleTri_: TitleParamStr = TitleParamStr()
        override var titleDed_: TitleParamStr = TitleParamStr()

        data class Captured(val text: String, val size: Float, val x: Float, val y: Float)
        val captured = mutableListOf<Captured>()

        override fun writeTextHV(
            text: String,
            point: com.example.trilib.PointXY,
            color: Int,
            textsize: Float,
            alignH: Int,
            alignV: Int,
            angle: Double,
            scale: Float
        ) {
            captured.add(Captured(text, textsize, point.x.toFloat(), point.y.toFloat()))
        }
    }

    private fun buildWriter(): CapturingWriter {
        val w = CapturingWriter()
        w.zumeninfo.zumentitle = "面 積 展 開 図"
        w.rosenname_ = "市道○○号線"
        w.zumenAreaSegments.add(DrawingFileWriter.AreaSegment("面積: A=14.94㎡", w.WHITE))
        return w
    }

    private fun titleSubtitleGap(scale: Float): Float {
        val w = buildWriter()
        w.writeTopTitle(scale = scale, textsize = 0f)
        val titleY = w.captured.first { it.text == w.zumeninfo.zumentitle }.y
        val subtitleY = w.captured.first { it.text.startsWith(w.rosenname_) }.y
        return titleY - subtitleY
    }

    @Test
    fun `タイトルとサブタイトル行の縦ギャップはscaleに対して線形(SFCと同じscale直渡し経路)`() {
        // DXF は常に scale=1 で writeTopTitle を呼ぶ (unitscale_ 側で後掛けする流儀)。
        // SFC は scale=printscale_ をそのまま渡す (DxfFileWriter.kt / SfcWriter.kt で確認済み)。
        // 2 経路が等価であるためには、gap(scale)/scale が scale に依らず一定でなければならない。
        val gapAt1 = titleSubtitleGap(1f)
        val gapAt0_5 = titleSubtitleGap(0.5f)

        val ratioAt1 = gapAt1 / 1f
        val ratioAt0_5 = gapAt0_5 / 0.5f

        assertEquals(
            ratioAt1, ratioAt0_5, 1e-3f,
            "gap/scale が scale=1 と scale=0.5 で食い違う = scale が二重に掛かっている" +
                " (SFC の scale=printscale_ 直渡し経路で表題のサブタイトル行位置がDXFと異なる原因)",
        )
    }
}
