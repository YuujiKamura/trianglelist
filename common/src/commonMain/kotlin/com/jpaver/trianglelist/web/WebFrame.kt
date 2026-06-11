package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.DrawingFileWriter
import com.jpaver.trianglelist.viewmodel.TitleParamStr

/**
 * Web 段階2g: 図面枠 (A3) を画面プリミティブとして出す。
 *
 * 形の正は DXF/SFC と同じ DrawingFileWriter.writeDrawingFrame / writeTopTitle
 * (外枠 40x27cm 中心 (21,14.85) + 右下タイトル枠 + 題字) — writer の writeLine /
 * writeTextHV を prim JSON に落とすだけで、枠のレイアウト定義は一切複製しない。
 *
 * 座標系: 枠は paper cm 系で書かれる。DXF は図形を枠中心 (21*ps, 14.85*ps) へ動かす
 * (DxfFileWriter.writeEntities:301-317) が、画面は編集中の図形を動かせないので逆に
 * 枠側を「図形中心 - 枠中心」だけ平行移動する — 相対配置は DXF 出力と同一。
 * ps (printscale) は trilist.getPrintScale(1f) の自動縮尺 (0.5 = 1/50 等)。
 * align は DXF TEXT group 72/73 (DxfEntity.writeTextHV) → prim の alignH/align に写す。
 */
object WebFrame {

    /** CSV → 図面枠 prim JSON 配列 (layer "frame")。三角形が無ければ空配列 */
    fun renderFrame(csv: String): String {
        val trilist = WebCsvReader.read(csv)
        if (trilist.size() < 1) return "[]"
        val ps = trilist.getPrintScale(1f)
        val header = WebDrawingExport.parseHeader(csv)
        val center = trilist.center
        val writer = FramePrimWriter(ps, center.x - 21f * ps, center.y - 14.85f * ps)
        writer.zumeninfo = WebDrawingExport.defaultZumenInfo()
        writer.titleTri_ = TitleParamStr()
        writer.titleDed_ = TitleParamStr()
        writer.printscale_ = ps
        writer.setNames(header.koujiname, header.rosenname, header.gyousyaname, header.zumennum)
        val textsize = trilist.getPrintTextScale(1f, "dxf") // DxfFileWriter.textscale_:41 と同値
        writer.writeDrawingFrame(1f, textsize)
        writer.writeTopTitle(1f, textsize)
        return "[" + writer.out.joinToString(",") + "]"
    }

    /** writeLine/writeTextHV を prim JSON に落とす writer。paper cm → モデル座標は ×ps + 平行移動 */
    private class FramePrimWriter(
        private val ps: Float,
        private val ox: Double,
        private val oy: Double,
    ) : DrawingFileWriter() {
        val out = mutableListOf<String>()

        private fun mx(p: PointXY) = PointXY((p.x * ps + ox).toFloat(), (p.y * ps + oy).toFloat())

        override fun writeLine(p1: PointXY, p2: PointXY, color: Int, scale: Float) {
            val a = mx(p1)
            val b = mx(p2)
            out.add("""{"type":"line","layer":"frame","x1":${a.x},"y1":${a.y},"x2":${b.x},"y2":${b.y}}""")
        }

        override fun writeTextHV(
            text: String,
            point: PointXY,
            color: Int,
            textsize: Float,
            alignH: Int,
            alignV: Int,
            angle: Double,
            scale: Float,
        ) {
            if (text.isBlank()) return
            val p = mx(point)
            // DXF 72 (0=left,1=center) → prim alignH (0=left, 省略=center)。
            // DXF 73 (0=baseline,1=bottom,2=middle,3=top) → prim align (1=下端,2=中央,3=上端)
            val h = if (alignH == 0) ""","alignH":0""" else ""
            val v = when (alignV) {
                2 -> 2
                3 -> 3
                else -> 1 // baseline/bottom は「点が文字の下端」
            }
            val esc = text.replace("\\", "\\\\").replace("\"", "\\\"")
            out.add(
                """{"type":"text","layer":"frame","text":"$esc","x":${p.x},"y":${p.y},"angle":$angle,"size":${textsize * ps},"align":$v$h}"""
            )
        }
    }
}
