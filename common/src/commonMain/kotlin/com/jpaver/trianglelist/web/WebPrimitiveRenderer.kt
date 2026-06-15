package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.isCollide
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement
import com.jpaver.trianglelist.setDimPath
import com.jpaver.trianglelist.setLengthStr
import com.jpaver.trianglelist.viewmodel.formattedString

/**
 * Web 段階1 (insight #60/#61): TriangleList → 描画プリミティブ JSON。
 *
 * 座標計算・寸法配置 (DimensionLayout)・番号サークル位置は全部こちら (Kotlin/common) で
 * 済ませ、JS 側は素朴に描くだけにする — UX parity の核を common に保つ。
 * 構成は app/datamanager/DxfFileWriter.writeTriangle (ADR 0003 Phase 2a) を踏襲:
 *   - 辺 3 本 (point[0]→pointAB→pointBC→point[0])
 *   - 寸法値: DimensionLayout.layout の dimpoint + calcDimAngle、A辺は
 *     「先頭三角形 or connectionSide>2」のときだけ (親との共有辺の重複防止)
 *   - 旗揚げ線: horizontal>2 のとき placement の pointA→pointB
 *   - 番号: pointnumber を中心にサークル (r=textSize*0.85) + 番号テキスト
 *     (DrawingFileWriter.writePointNumber と同じ。引出線は段階1 スコープ外)
 *
 * JSON 形式 (フラット配列、座標はモデル座標系 = y 上向き。y 反転は JS 側):
 *   {"type":"line","layer":"tri|dim","x1":..,"y1":..,"x2":..,"y2":..}
 *   {"type":"text","layer":"dim|num","text":"..","x":..,"y":..,"angle":deg,"size":..,"align":1|2|3}
 *     align は DXF 垂直コード: 1=点が文字の下端 (文字は点の上), 2=中央, 3=点が文字の上端
 *   {"type":"circle","layer":"num","cx":..,"cy":..,"r":..}
 *   {"type":"fill","layer":"fill","x1":..,"y1":..,"x2":..,"y2":..,"x3":..,"y3":..,"color":idx,"tri":N}
 *     color は CSV 列 10 (= Triangle.mycolor、既定 4) の index。実色は TS 側パレットが解決
 *
 * 段階2e (task #15) の識別フィールド: TS 側の当たり判定と W/H cycle のために
 *   - dim テキスト: 末尾に "tri":番号,"side":0|1|2,"h":水平値,"v":垂直値
 *     (h/v は現在実効値。TS は実効値から次の cycle/flip 値を計算する —
 *      vertical の初期値は接続構造依存で TS から推測できないため JSON で渡す)
 *   - 番号サークル/番号テキスト: 末尾に "tri":番号
 */
object WebPrimitiveRenderer {

    /** 寸法文字高さ (モデル単位)。サンプル図面 (辺 2-6m) で読める大きさ */
    const val DEFAULT_TEXT_SIZE = 0.25f

    /** アプリの textSize 初期値 (MyView.kt:117 `var textSize = 30f`)。
     *  CSV の TextSize 行 (writeCSV:2785) はこの単位 (view px) で入るので、比率で
     *  web のモデル単位文字高さへ写す。行が無い CSV は比率 1 = 従来出力 (golden 不変) */
    const val APP_DEFAULT_TEXT_SIZE = 30f

    /** CSV 文字列から JSON プリミティブまで一気通貫 (wasmJs facade の本体) */
    fun renderCsv(csv: String, scale: Float): String = renderCsv(csv, scale, "")

    /**
     * 段階2e (task #15): overrides 付き経路。parse (+setScale) 後に WebOverrides を
     * model へ適用してから render する。render 内の setDimPathTextSize は dim の
     * horizontal/vertical 値を読み直すだけ (値は保持される)、arrangePointNumbers は
     * isMovedByUser flag (PointNumberManager.kt:36) が override 適用済みの番号を保護する。
     * overridesJson が空なら renderCsv(csv, scale) と完全同一出力。
     */
    fun renderCsv(csv: String, scale: Float, overridesJson: String): String {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        if (scale != 1f && scale > 0f) trilist.setScale(PointXY(0f, 0f), scale)
        WebOverrides.applyJson(trilist, overridesJson)
        // CSV の TextSize 行 (アプリ texplus/minus FAB ±5f の保存値) を比率で反映。
        // 0 以下は不正値として既定にフォールバック (アプリ adjustTextSize の下限クランプ相当)
        val sizeRatio = (doc.textSize?.takeIf { it > 0f } ?: APP_DEFAULT_TEXT_SIZE) / APP_DEFAULT_TEXT_SIZE
        val effScale = if (scale > 0f) scale else 1f
        val textSize = DEFAULT_TEXT_SIZE * sizeRatio * effScale
        // 混在リスト: 台形 + 台形子三角形を figureRows から構築し render に渡す。
        // figureRows に台形/TriTrap 行が無ければ空リスト → 出力は完全に従来と同一 (golden 不変)。
        val (traps, trapTris) = CsvCodec.buildFigures(doc, trilist, effScale)
        return render(trilist, textSize, CsvCodec.buildDeductions(doc), effScale, traps, trapTris)
    }

    fun render(trilist: TriangleList, textSize: Float): String =
        render(trilist, textSize, DeductionList(), 1f)

    fun render(
        trilist: TriangleList,
        textSize: Float,
        dedlist: DeductionList,
        scale: Float,
        traps: List<Rectangle> = emptyList(),
        trapTris: List<Triangle> = emptyList(),
    ): String {
        // MyView.setAllTextSize → trianglelist.setDimPathTextSize と同じ経路で dimHeight を配る
        trilist.setDimPathTextSize(textSize)
        trilist.arrangePointNumbers()

        val sb = StringBuilder()
        sb.append('[')
        var first = true
        fun item(json: String) {
            if (!first) sb.append(',')
            sb.append(json)
            first = false
        }

        // 塗り (アプリ MyView.drawEntities:572-576 の写し — 全三角形を mycolor で塗ってから
        // 線・文字を重ねる)。z-order をアプリと同じ「塗り全部 → 線・文字」にするため先に全部出す。
        // 色は index のまま出す (CSV 列 10 = Triangle.mycolor、既定 4)。実色はパレットを持つ
        // TS 側 (web/src/main.ts FILL_PALETTE) が解決する — アプリも index → 色配列の二段
        // (MainActivity.resColors / MyView.darkColors_) で同じ構図
        for (i in 1..trilist.size()) {
            val tri = trilist.getBy(i)
            item(
                """{"type":"fill","layer":"fill","x1":${tri.point[0].x},"y1":${tri.point[0].y},"x2":${tri.pointAB.x},"y2":${tri.pointAB.y},"x3":${tri.pointBC.x},"y3":${tri.pointBC.y},"color":${tri.mycolor},"tri":${tri.mynumber}}"""
            )
        }

        // 台形を親に持つ三角形 (TriTrap) の塗り + 番号/dimHeight/番号位置の配布。trilist 外なので
        // arrangePointNumbers/setDimPathTextSize が回らない → ここで手配り。番号は三角形→台形→台形子の通し。
        // z-order を保つため塗りはこの塗りパスで出し、線・寸法は台形の後 (下の描画パス) で重ねる。
        val trapTriBase = trilist.size() + traps.size
        for ((j, t) in trapTris.withIndex()) {
            t.setDimPath(textSize)
            t.mynumber = trapTriBase + j + 1
            t.pointnumber = t.pointcenter
            item(
                """{"type":"fill","layer":"fill","x1":${t.point[0].x},"y1":${t.point[0].y},"x2":${t.pointAB.x},"y2":${t.pointAB.y},"x3":${t.pointBC.x},"y3":${t.pointBC.y},"color":${t.mycolor},"tri":${t.mynumber}}"""
            )
        }

        for (i in 1..trilist.size()) {
            val tri = trilist.getBy(i)
            val pca = tri.pointCA
            val pab = tri.pointAB
            val pbc = tri.pointBC

            // 辺 (DrawingFileWriter.writeTriangleLines と同じ順)。tri/side 識別子を載せて、
            // web 側の辺選択ハイライト等が「物理 side で line を直接取れる」ようにする
            // (描画順 != side 番号の場合分けを上位から消す、user 指針 2026-06-14)。
            val tn = tri.mynumber
            item(line(tri.point[0], pab, "tri", ""","tri":$tn,"side":0"""))
            item(line(pab, pbc, "tri", ""","tri":$tn,"side":1"""))
            item(line(pbc, tri.point[0], "tri", ""","tri":$tn,"side":2"""))

            // 寸法配置 (DxfFileWriter.layoutTriple と同じ引数)
            val scale = tri.scaleFactor.toDouble()
            val dimheight = tri.dimHeight.toDouble()
            val placeA = DimensionLayout.layout(tri.pointAB, tri.point[0], tri.dim.vertical.a, tri.dim.horizontal.a, scale, dimheight, 0.0)
            val placeB = DimensionLayout.layout(tri.pointBC, tri.pointAB, tri.dim.vertical.b, tri.dim.horizontal.b, scale, dimheight, 0.0)
            val placeC = DimensionLayout.layout(tri.point[0], tri.pointBC, tri.dim.vertical.c, tri.dim.horizontal.c, scale, dimheight, 0.0)

            tri.setLengthStr()

            // A辺の寸法は先頭 or 再接続 (connectionSide>2) のときだけ (DxfFileWriter.writeTriangle と同じ)
            if (tri.mynumber == 1 || tri.connectionSide > 2) {
                item(dimText(tri.strLengthA, placeA, pab.calcDimAngle(pca), textSize, tri.mynumber, 0, tri.dim.horizontal.a, tri.dim.vertical.a))
            }
            item(dimText(tri.strLengthB, placeB, pbc.calcDimAngle(pab), textSize, tri.mynumber, 1, tri.dim.horizontal.b, tri.dim.vertical.b))
            item(dimText(tri.strLengthC, placeC, pca.calcDimAngle(pbc), textSize, tri.mynumber, 2, tri.dim.horizontal.c, tri.dim.vertical.c))

            // 旗揚げ線 (DxfFileWriter.writeDimFlagsFromLayout と同じ条件)
            if (tri.dim.horizontal.a > 2) item(line(placeA.pointA, placeA.pointB, "dim"))
            if (tri.dim.horizontal.b > 2) item(line(placeB.pointA, placeB.pointB, "dim"))
            if (tri.dim.horizontal.c > 2) item(line(placeC.pointA, placeC.pointB, "dim"))

            // 番号サークル + 番号 (DrawingFileWriter.writePointNumber と同じ r=ts*0.85、中央寄せ)
            val pn = tri.pointnumber
            val circleR = textSize * 0.85f
            item("""{"type":"circle","layer":"num","cx":${pn.x},"cy":${pn.y},"r":$circleR,"tri":${tri.mynumber}}""")
            item(text(tri.mynumber.toString(), pn, 0.0, textSize, 2, "num", ""","tri":${tri.mynumber}"""))

            // 引き出し矢印線 (MyView.drawTriNumber:878-900 の鏡写し): サークルが三角形の外に
            // 出ているとき、図形センターからサークルの縁まで線を引き、センター側に 10° 回転の
            // 返し (チェック矢印) を付ける。式は app と同じモデル空間、表示時の Y 反転で同じ見た目
            if (!tri.isCollide(tri.pointnumber)) {
                val pc = tri.pointcenter
                val pnOffsetToC = pn.offset(pc, (circleR * 1.1f).toDouble())
                val arrowTail = pc.offset(pn, pc.lengthTo(pnOffsetToC) * 0.3).rotate(pc, 10.0)
                item(line(pc, pnOffsetToC, "num"))
                item(line(pc, arrowTail, "num"))
            }
        }

        // 控除 (Deduction)。正 = DxfFileWriter.writeDeduction (DxfFileWriter.kt:240-282)。
        // ded はビュー空間 (y 下向き) で持つので、DxfFileWriter.writeEntities:299 と同じく
        // Y 反転 (web は viewscale=1) でモデル空間に揃えてから描く
        if (dedlist.size() > 0) {
            val dedModel = dedlist.clone()
            dedModel.scale(PointXY(0f, 0f), scale, -scale)
            for (n in 1..dedModel.size()) renderDeduction(dedModel.get(n), textSize, scale, ::item)
        }

        // 台形 (混在リスト)。三角形・控除を全部描いた後に最後に重ねるだけ。
        // 既存の三角形描画・控除描画・z-order (塗り→線→文字) は一切触らない。
        // 番号は三角形からの通し: 三角形が N 個なら台形は N+1, N+2... (図形種別を意識しない統合採番)。
        val triCount = trilist.size()
        for ((idx, rect) in traps.withIndex()) renderTrapezoid(rect, triCount + idx + 1, textSize, scale, ::item)

        // 台形を親に持つ三角形の線・寸法・番号 (台形の後に重ねる)。底辺A は台形と共有なので寸法を出さない
        // (接続済み三角形と同じ — 共有辺の寸法は親側が持つ)。B/C 寸法と番号サークルは通常の三角形と同形。
        for (t in trapTris) {
            val pca = t.pointCA
            val pab = t.pointAB
            val pbc = t.pointBC
            val tn = t.mynumber
            item(line(t.point[0], pab, "tri", ""","tri":$tn,"side":0"""))
            item(line(pab, pbc, "tri", ""","tri":$tn,"side":1"""))
            item(line(pbc, t.point[0], "tri", ""","tri":$tn,"side":2"""))
            val sf = t.scaleFactor.toDouble()
            val dh = t.dimHeight.toDouble()
            val placeB = DimensionLayout.layout(t.pointBC, t.pointAB, t.dim.vertical.b, t.dim.horizontal.b, sf, dh, 0.0)
            val placeC = DimensionLayout.layout(t.point[0], t.pointBC, t.dim.vertical.c, t.dim.horizontal.c, sf, dh, 0.0)
            t.setLengthStr()
            item(dimText(t.strLengthB, placeB, pbc.calcDimAngle(pab), textSize, t.mynumber, 1, t.dim.horizontal.b, t.dim.vertical.b))
            item(dimText(t.strLengthC, placeC, pca.calcDimAngle(pbc), textSize, t.mynumber, 2, t.dim.horizontal.c, t.dim.vertical.c))
            if (t.dim.horizontal.b > 2) item(line(placeB.pointA, placeB.pointB, "dim"))
            if (t.dim.horizontal.c > 2) item(line(placeC.pointA, placeC.pointB, "dim"))
            val pn = t.pointnumber
            val circleR = textSize * 0.85f
            item("""{"type":"circle","layer":"num","cx":${pn.x},"cy":${pn.y},"r":$circleR,"tri":${t.mynumber}}""")
            item(text(t.mynumber.toString(), pn, 0.0, textSize, 2, "num", ""","tri":${t.mynumber}"""))
        }

        sb.append(']')
        return sb.toString()
    }

    /**
     * 台形 1 個を描く (混在リスト段1)。Rectangle.calcPoint() の頂点で 4 辺 + 各辺の寸法 +
     * 番号サークルを出す。頂点 (editmodel/Rectangle.kt:22-35、PointXY.crossOffset で実機検証):
     *   baseline.left = 底辺左(基点 BL), baseline.right = 底辺右(BR),
     *   leftB = 上辺左(TL, 基点から延長 length の直交先), rightB = 上辺右(TR)。
     * 4 辺は BL→BR(底辺A)→TR(傾斜側)→TL(上辺C)→BL(延長B) で閉じる。
     * 寸法は三角形と同じ共通の式層 (DimensionLayout) に通す — 図形ごとの別経路を作らない (段A)。
     * 辺ごとの寄せは Rectangle の純データ (dimVertical/dimHorizontal、既定は三角形と同値の外1/中央0)。
     * 傾斜側 D右脚 (BR→TR) は派生辺なので寸法なし。番号は三角形からの通し (num は triCount+idx+1)。
     */
    private fun renderTrapezoid(rect: Rectangle, num: Int, textSize: Float, scale: Float, item: (String) -> Unit) {
        val lp = rect.calcPoint()
        val bl = lp.a.left
        val br = lp.a.right
        val tl = lp.b.left
        val tr = lp.b.right

        // 4 辺 (layer "tri"、三角形の辺と同じレイヤ)。物理 side: 0=A底辺, 1=B左脚/延長, 2=C上辺, 3=D右脚
        // 描画順は外周巡回 (bl→br→tr→tl→bl) で side 順 (0→3→2→1) と一致しないので、
        // 個々の line に物理 side を載せる (web 側は side 直引きで反対辺ハイライトを防ぐ)。
        item(line(bl, br, "tri", ""","tri":$num,"side":0"""))   // A 底辺
        item(line(br, tr, "tri", ""","tri":$num,"side":3"""))   // D 右脚
        item(line(tr, tl, "tri", ""","tri":$num,"side":2"""))   // C 上辺
        item(line(tl, bl, "tri", ""","tri":$num,"side":1"""))   // B 左脚/延長

        // 寸法 — 三角形と同じ共通の式層 (DimensionLayout) に通す。図形ごとの別経路を作らない。
        // 外周順の辺: bl→br(A底辺) / tr→tl(C上辺) / tl→bl(B延長/左脚) の3本。D右脚(br→tr)は派生辺で寸法なし。
        // 三角形の dimText 呼びと同形: layout(end, start, v, h, scale, height) → dimText。
        val ds = rect.dimScale.toDouble()
        val dh = rect.dimHeight.toDouble()
        // A底辺・C上辺は辺そのものの実長 (/scale)。接続台形では底辺=親辺長になる。
        fun emitMeasured(start: PointXY, end: PointXY, v: Int, h: Int, sideIdx: Int) {
            val place = DimensionLayout.layout(end, start, v, h, ds, dh, 0.0)
            val len = (start.lengthTo(end) / scale).toFloat()
            item(dimText(len.formattedString(2), place, start.calcDimAngle(end), textSize, num, sideIdx, h, v))
            if (h > 2) item(line(place.pointA, place.pointB, "dim"))
        }
        // 接続済み (nodeA != null) は底辺=親辺で共有 → 寸法は親側が持つので子は出さない
        // (接続済み三角形が辺A寸法を親に任せるのと同じ思想、user 指摘 2026-06-14 「7.0 が二重表示」)
        if (rect.nodeA == null) emitMeasured(bl, br, rect.dimVertical.a, rect.dimHorizontal.a, 0)   // A 底辺
        emitMeasured(tr, tl, rect.dimVertical.c, rect.dimHorizontal.c, 2)   // C 上辺
        // D 右脚 — 派生辺 (底辺+上辺+延長から幾何で決まる) だが、辺長は観察可能 (= 接続子三角形の
        // 親辺長の SoT)。寸法を出さないと「D辺タップで表記が無い」(user 指摘 2026-06-14)。
        // align は固定値 (v=1 外向き / h=0 中央) — DimAligns は (a, b, c, s) 4 値だが s は予備で
        // D に紐づく規約が無いので、Rectangle データから引かずに既定値で出す
        emitMeasured(br, tr, 1, 0, 3)   // D 右脚

        // B 延長 = 底辺/上辺間の「垂線」の長さ (rect.length)。左脚 (bl→tl) の斜辺長ではない。
        // 垂線の描画起点は「底辺と上辺の短い方」を選ぶ (user 指摘 2026-06-14「底辺に固定してるのも
        // 間違い。上辺と底辺の短い方を起点にすべき」)。短辺側に垂線根を置く方が視認的にずれが小さく
        // 寸法線がはみ出しにくい。
        val bottomShorter = rect.widthA <= rect.widthB
        val baseStart = if (bottomShorter) bl else tl
        val baseEnd = if (bottomShorter) br else tr
        // Y 上向きモデル座標で「台形内側」に向けるには、底辺起点は反時計回り (default -90.0)、
        // 上辺起点は時計回り (+90.0) で perpendicular を取る。上辺起点で default のままだと
        // perpFoot が台形外側に向く (user 指摘 2026-06-14「垂線を延ばす方向が逆」)。
        val perpFoot = baseStart.crossOffset(baseEnd, rect.length, if (bottomShorter) -90.0 else 90.0)
        val placeB = DimensionLayout.layout(perpFoot, baseStart, rect.dimVertical.b, rect.dimHorizontal.b, ds, dh, 0.0)
        val extLen = (rect.length / scale).toFloat()
        item(dimText(extLen.formattedString(2), placeB, baseStart.calcDimAngle(perpFoot), textSize, num, 1, rect.dimHorizontal.b, rect.dimVertical.b))
        // 垂線起点の verify 用 meta (距離 tie で誤判定しないよう、実装の判定をそのまま prim に乗せる)
        item("""{"type":"meta","kind":"perp","tri":$num,"perpFrom":"${if (bottomShorter) "bl" else "tl"}"}""")
        if (rect.alignment != 0) item(line(baseStart, perpFoot, "guide"))
        if (rect.dimHorizontal.b > 2) item(line(placeB.pointA, placeB.pointB, "dim"))

        // 番号サークル + 番号 (三角形と同じ r=textSize*0.85、重心に中央寄せ)
        val cx = (bl.x + br.x + tr.x + tl.x) / 4.0
        val cy = (bl.y + br.y + tr.y + tl.y) / 4.0
        val center = PointXY(cx, cy)
        val circleR = textSize * 0.85f
        item("""{"type":"circle","layer":"num","cx":${center.x},"cy":${center.y},"r":$circleR}""")
        item(text("$num", center, 0.0, textSize, 2, "num"))
    }


    /**
     * DxfFileWriter.writeDeduction:240-282 の写し (色 = 赤系は layer "ded" として TS 側で塗る):
     * - 旗揚げ線 point→pointFlag
     * - infoStr テキスト + 下線 (writeTextAndLine = DxfEntity.kt:172-181: テキストは
     *   p1 + (ts, ts*0.2)、左寄せ・下端基準、下線は p1→p2)。pointFlag の左右で分岐
     * - Circle: 中心 point、半径 lengthX/2 (writeCircle:270)
     * - Box: writeDedRect:274-281 = shapeAngle 逆回転で setBox し直して 4 辺
     */
    private fun renderDeduction(ded: Deduction, textSize: Float, scale: Float, item: (String) -> Unit) {
        val infoStrLength = ded.infoStr.length * textSize + 0.3f
        val point = ded.point
        val pointFlag = ded.pointFlag
        val textOffsetX = if (ded.type == "Box") -0.5f else 0f
        // 控除番号タグ — TS 側が選択中の控除だけ色替え表示するための識別子
        val tag = ""","ded":${ded.num}"""

        item(line(point, pointFlag, "ded", tag))
        if (point.x <= pointFlag.x) { // ptFlag is RIGHT from pt
            item(dedTextAndLine(ded.infoStr, pointFlag, pointFlag.plus((infoStrLength + textOffsetX).toDouble(), 0.0), textSize, tag))
        } else {                      // ptFlag is LEFT from pt
            val p1 = pointFlag.plus((-ded.getInfo().length * textSize - textOffsetX).toDouble(), 0.0)
            item(dedTextAndLine(ded.infoStr, p1, pointFlag, textSize, tag))
        }

        if (ded.type == "Circle") {
            item("""{"type":"circle","layer":"ded","cx":${point.x},"cy":${point.y},"r":${ded.lengthX / 2 * scale}$tag}""")
        }
        if (ded.type == "Box") {
            // writeDedRect: 逆回転で box を組み直す (Y 反転後の空間では shapeAngle の符号が逆)
            ded.shapeAngle = -ded.shapeAngle
            ded.setBox(scale.toDouble())
            item(line(ded.pLTop, ded.pLBtm, "ded", tag))
            item(line(ded.pLTop, ded.pRTop, "ded", tag))
            item(line(ded.pRTop, ded.pRBtm, "ded", tag))
            item(line(ded.pLBtm, ded.pRBtm, "ded", tag))
        }
    }

    /** DxfEntity.writeTextAndLine と同形: テキスト (左寄せ・点が下端) + 下線。2 つの prim を結合して返す */
    private fun dedTextAndLine(st: String, p1: PointXY, p2: PointXY, textSize: Float, tag: String = ""): String =
        text(st, p1.plus(textSize.toDouble(), textSize.toDouble() * 0.2), 0.0, textSize, 1, "ded", ""","alignH":0$tag""") +
            "," + line(p1, p2, "ded", tag)

    private fun line(p1: PointXY, p2: PointXY, layer: String): String = line(p1, p2, layer, "")

    private fun line(p1: PointXY, p2: PointXY, layer: String, extra: String): String =
        """{"type":"line","layer":"$layer","x1":${p1.x},"y1":${p1.y},"x2":${p2.x},"y2":${p2.y}$extra}"""

    /**
     * SoT 一本化 段2 受け口 (2026-06-15): 混在 EditList<EditObject> を直接受けて prim を出す
     * 新ルート。Strangler Fig — 既存 render(trilist, ...) は一切いじらず、新口で機能を段階的に
     * 完備して揃ったら renderCsv をこちらに切り替える。
     *
     * 段階1 (現): 塗り + 辺 + 番号サークル + 番号テキストの最小スケルトン (golden 完全等価ではない)。
     * 段階2 (予定): 寸法レイアウト (DimensionLayout) を多態で配布、Deduction overlay、引き出し矢印。
     * 段階3 (予定): renderCsv 内で build/buildFigures → buildAll に切替、既存 render(trilist) を撤去。
     *
     * EditObject の多態 (sideCount / vertices / getLine) を使って形状ごとの kind 分岐を消す方針:
     *   - Triangle.sideCount=3, Rectangle.sideCount=4 で stride を決定
     *   - vertices() で塗り/番号サークル中心を一律算出
     *   - getLine(side) で辺を取り出し layer="tri" の line prim 化
     *
     * 注: Triangle.pointAB/pointBC/pointCA と vertices() の順序整合は実機検証済 (TriangleTest)。
     */
    fun render(
        list: EditList<EditObject>,
        textSize: Float,
        dedlist: DeductionList = DeductionList(),
        scale: Float = 1f,
    ): String {
        val sb = StringBuilder()
        sb.append('[')
        var first = true
        fun item(json: String) {
            if (!first) sb.append(',')
            sb.append(json)
            first = false
        }

        // 段階1a: 塗り (z-order「塗り全部→線・文字」確保のため先に全部出す)
        list.forEachItemIndexed { num, obj ->
            when (obj) {
                is Triangle -> {
                    val verts = obj.vertices()
                    if (verts.size >= 3) {
                        item(
                            """{"type":"fill","layer":"fill","x1":${verts[0].x},"y1":${verts[0].y},"x2":${verts[1].x},"y2":${verts[1].y},"x3":${verts[2].x},"y3":${verts[2].y},"color":${obj.mycolor},"tri":$num}"""
                        )
                    }
                }
                // Rectangle は塗らない (既存 renderTrapezoid も塗りなし)
            }
        }

        // 段階1b: 辺 (多態 getLine) + 番号サークル + 番号テキスト
        list.forEachItemIndexed { num, obj ->
            for (side in 0 until obj.sideCount) {
                val ln = obj.getLine(side)
                item(line(ln.left, ln.right, "tri", ""","tri":$num,"side":$side"""))
            }
            // 番号位置: Triangle は事前計算済 pointnumber、Rectangle は重心 (vertices 平均)
            val center = when (obj) {
                is Triangle -> obj.pointnumber
                else -> {
                    val v = obj.vertices()
                    if (v.isEmpty()) PointXY(0f, 0f)
                    else {
                        var sx = 0f; var sy = 0f
                        for (p in v) { sx += p.x.toFloat(); sy += p.y.toFloat() }
                        PointXY(sx / v.size, sy / v.size)
                    }
                }
            }
            val circleR = textSize * 0.85f
            item("""{"type":"circle","layer":"num","cx":${center.x},"cy":${center.y},"r":$circleR,"tri":$num}""")
            item(text(num.toString(), center, 0.0, textSize, 2, "num", ""","tri":$num"""))
        }

        sb.append(']')
        return sb.toString()
    }

    private fun dimText(str: String, place: DimensionPlacement, angle: Double, textSize: Float, tri: Int, side: Int, h: Int, v: Int): String =
        text(str, place.dimpoint, angle, textSize, place.verticalDxf, "dim", ""","tri":$tri,"side":$side,"h":$h,"v":$v""")

    // extra は識別フィールド (",key":value 形式) をオブジェクト末尾に足すための口。
    // commonTest の count ベース assert (prefix 部分一致) を壊さないため末尾固定
    private fun text(str: String, p: PointXY, angle: Double, size: Float, align: Int, layer: String, extra: String = ""): String =
        """{"type":"text","layer":"$layer","text":"${escapeJson(str)}","x":${p.x},"y":${p.y},"angle":$angle,"size":$size,"align":$align$extra}"""

    private fun escapeJson(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append("\\u" + c.code.toString(16).padStart(4, '0')) else sb.append(c)
            }
        }
        return sb.toString()
    }
}
