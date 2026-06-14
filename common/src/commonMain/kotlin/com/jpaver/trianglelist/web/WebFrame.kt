package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.datamanager.DrawingFileWriter
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
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

    /** CSV → 図面枠 prim JSON 配列 (layer "frame")。図形 (三角形 / 台形 / 台形子三角形) が一つも無ければ空配列。
     *  user 指摘 2026-06-14「混在リスト全体の境界計算してセンタリングするのが出来てない」── trilist.center
     *  だけでは台形 / 台形子三角形が含まれず、枠中心と figure 全体中心がずれる。混在リスト全体 (trilist + traps
     *  + trapTris) の頂点 bbox 中心を center にする。paper 幾何中心は (20, 13.5)。 */
    fun renderFrame(csv: String): String {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val ps = trilist.getPrintScale(1f)
        // 描画 scale は 1f (WebPrimitiveRenderer.renderCsv の effScale と同値、印刷 ps と別軸)。
        // ここで ps を渡すと台形の幾何が壊れて build が空になる (frame: null が消えない原因)。
        val traps = CsvCodec.buildTrapezoids(doc, trilist, 1f)
        val trapTris = CsvCodec.buildTrapParentedTriangles(doc, traps, 1f)
        if (trilist.size() < 1 && traps.isEmpty() && trapTris.isEmpty()) return "[]"
        val header = WebDrawingExport.parseHeader(csv)
        val center = mixedFigureCenter(trilist, traps, trapTris)
        // paper-cm 系での paper 全体中心は (21, 14.85) (A3 42x29.7cm の中心、writeOuterFrame:432 と同値)。
        // 外枠 (40x27cm) も同じ中心に置かれる。ここを figure 全体中心に合わせる = 枠内センタリング。
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

    /** 混在リスト全体 (三角形 + 台形 + 台形子三角形) の頂点 bbox 中心。trilist.center は三角形の
     *  bbox 中心しか見ないので、台形だけ・台形子三角形だけが図面端に乗ると枠中心とズレる。 */
    private fun mixedFigureCenter(
        trilist: TriangleList,
        traps: List<Rectangle>,
        trapTris: List<Triangle>,
    ): PointXY {
        var minX = Double.POSITIVE_INFINITY; var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY; var maxY = Double.NEGATIVE_INFINITY
        fun add(p: PointXY) {
            val x = p.x.toDouble(); val y = p.y.toDouble()
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        for (i in 1..trilist.size()) {
            val t = trilist[i]
            add(t.point[0]); add(t.pointAB); add(t.pointBC)
        }
        for (r in traps) {
            val lp = r.calcPoint()
            add(lp.a.left); add(lp.a.right); add(lp.b.left); add(lp.b.right)
        }
        for (t in trapTris) {
            add(t.point[0]); add(t.pointAB); add(t.pointBC)
        }
        return PointXY(((minX + maxX) / 2.0).toFloat(), ((minY + maxY) / 2.0).toFloat())
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

        /**
         * タイトル欄の「値」テキストの paper-cm アンカー (DrawingFileWriter の書込み座標の鏡):
         * 工事名 writeDrawingFrame:273 (33.5, 6.7) / 路線名 :282 (33.5, 4.7) + writeTopTitle:195 (21, 26) /
         * 図面番号 :288 (39.5, 2.7) / 業者名 :290 (33.5, 1.7)。
         * web の dblclick 編集 (web/src/main.ts textEditTarget) が欄を特定する識別子で、
         * 値が空でも欄はクリックできる必要があるため、blank でもタグ付き prim は emit する
         * (デフォルト空白 + 枠内 dblclick で書換え、2026-06-12 user 要望)
         */
        private fun fieldAt(p: PointXY): String? = when {
            near(p, 33.5f, 6.7f) -> "koujiname"
            near(p, 33.5f, 4.7f) || near(p, 21f, 26f) -> "rosenname"
            near(p, 39.5f, 2.7f) -> "zumennum"
            near(p, 33.5f, 1.7f) -> "gyousyaname"
            else -> null
        }

        private fun near(p: PointXY, x: Float, y: Float): Boolean =
            kotlin.math.abs(p.x - x) < 0.01f && kotlin.math.abs(p.y - y) < 0.01f

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
            val field = fieldAt(point)
            if (text.isBlank() && field == null) return
            val p = mx(point)
            // DXF 72 (0=left,1=center) → prim alignH (0=left, 省略=center)。
            // DXF 73 (0=baseline,1=bottom,2=middle,3=top) → prim align (1=下端,2=中央,3=上端)
            val h = if (alignH == 0) ""","alignH":0""" else ""
            val v = when (alignV) {
                2 -> 2
                3 -> 3
                else -> 1 // baseline/bottom は「点が文字の下端」
            }
            val f = if (field != null) ""","field":"$field"""" else ""
            val esc = text.replace("\\", "\\\\").replace("\"", "\\\"")
            out.add(
                """{"type":"text","layer":"frame","text":"$esc","x":${p.x},"y":${p.y},"angle":$angle,"size":${textsize * ps},"align":$v$h$f}"""
            )
        }
    }
}
