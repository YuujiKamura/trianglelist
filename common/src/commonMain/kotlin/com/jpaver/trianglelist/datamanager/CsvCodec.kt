package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.ConnCode
import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.setColor
import com.jpaver.trianglelist.setDimAligns
import com.jpaver.trianglelist.setPointNumber
import com.jpaver.trianglelist.viewmodel.InputParameter

/**
 * CSV の文書モデルと codec (ADR 0008)。
 *
 * これまでの「ファイル ⇄ 生きた TriangleList の直結」(読みながら半構築のリストへ副作用を
 * 順番に当てる) をやめ、間に純データの文書 (CsvDoc) を置く。タイミング由来のバグ
 * (connectionSide が入る前に自動配置が走る / 手動値が後続の add に潰される、ADR 0006 付記)
 * は全部この直結から出ていた。
 *
 * - parse:    text → CsvDoc。計算しない・構築しない。未知の列・行は生のまま保持する
 *             (schema evolution の定石: 位置は再利用しない・未知フィールドは保持して書き戻す)
 * - build:    CsvDoc → TriangleList。named phases (幾何構築 → 手動配置 → リスト回転)。
 *             手動配置は「全行の add が終わった後」に当てるので、後続の add が先行行の
 *             保存値を潰す事故が構造的に起きない
 * - bake:     構築済み TriangleList → CsvDoc (完全形式 28 列)。アプリの保存
 *             (MainActivity.writeCSV:2745-2792) と同じ列順・同じ値。web の overrides を
 *             model に適用してから bake すれば、手動配置が CSV に書き戻る (ユーザー損失の解消)
 * - serialize: CsvDoc → text
 *
 * 列定義 (writeCSV / CsvLoader.TriangleColumn と同一):
 *   0-3  番号, 辺A, 辺B, 辺C
 *   4-5  親番号, 接続コード (1=B 2=C 3-8=二重断面 9/10=フロート)
 *   6-9  測点名, 番号サークル x, y, 移動フラグ
 *   10   色
 *   11-13 寸法 horizontal a/b/c   14-16 寸法 vertical a/b/c
 *   17-19 cp.side, cp.type, cp.lcr
 *   20-21 寸法手動フラグ b/c
 *   22-25 angle, pointCA.x, pointCA.y, angleInLocal (幾何キャッシュ — 読み側は再計算する)
 *   26-27 測点 horizontal, 測点手動フラグ
 */
object CsvCodec {

    /** 三角形 1 行。chunks は trim 済みの生の列 (列0 = 番号を含む)。未知の列も保持 */
    data class CsvRow(val chunks: List<String>) {
        val number: Int get() = chunks[0].toIntOrNull() ?: -1
    }

    /**
     * CSV 文書。preLines = 最初の図形行より前の行 (ヘッダ等)、postLines = それ以降の
     * 非図形行 (ListScale 等)。どちらも原文のまま保持して書き戻す。
     * ListAngle 行だけは数値として取り出す (リスト回転の SoT、ADR 0007)。
     * TextSize 行も同様に昇格 (寸法文字サイズの SoT)。
     * dedRows = "Deduction" 先頭の控除行。chunks は列0 = "Deduction" を含む生の列。
     *
     * figureRows = 三角形 + 台形 + 台形子三角形 (TriTrap) を CSV 出現順で保持する SoT。
     *   chunks[0] == "Trapezoid" → 台形行
     *   chunks[0] == "TriTrap"   → 台形子三角形行 (旧タグ形式、parse で内部変換済み)
     *   chunks[0].toIntOrNull() != null → 三角形行 (または混在通し番号の台形子三角形行)
     * B-FORCE: rows/trapRows/trapParentedTriRows バケツ廃止 (2026-06-15)。figureRows が唯一の SoT。
     */
    data class CsvDoc(
        val preLines: List<String>,
        val listAngle: Float?,
        val postLines: List<String>,
        val dedRows: List<CsvRow> = emptyList(),
        val textSize: Float? = null,
        // B04a: ListScale を named field に昇格 (旧: postLines に生文字列として残存)。
        val listScale: Float? = null,
        // 全図形行 (三角形 + 台形 + 台形子三角形) を CSV 出現順で保持する。種別は chunks[0] で判断。
        val figureRows: List<CsvRow> = emptyList(),
    )

    /**
     * 独立台形 (parent=-1) の構築向き (度)。三角形の独立は Triangle(...,180f) で add され
     * recoverState で angle-180 = -180° 回るので、最終実効向きは「底辺が +x・本体が -y (下垂れ)」。
     * 台形は trilist 外なので recoverState を受けない。本体を三角形と同じ下向きに揃えるため 180°
     * (= 底辺 -x・本体 -y) で構築する。段1 の独立向きの SoT。目視で最終確定する値 (B1 検証4)。
     */
    const val INDEP_TRAP_ANGLE = 180.0

    fun parse(text: String): CsvDoc {
        val preLines = mutableListOf<String>()
        val postLines = mutableListOf<String>()
        val dedRows = mutableListOf<CsvRow>()
        val figureRows = mutableListOf<CsvRow>()
        var listAngle: Float? = null
        var textSize: Float? = null
        var listScale: Float? = null  // B04a: named field に昇格
        // B-FORCE: 三角形カウントは「普通三角形行」を追跡するためだけに使う
        // (parent が三角形数を超えていれば台形/TriTrap 親の混在通し番号と解釈するため)
        var triCount = 0
        var hasFigure = false  // 最初の図形行より前を preLines、以降の非図形行を postLines に振る
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val chunks = line.split(",").map { it.trim() }
            if (chunks.firstOrNull() == "ListAngle") {
                listAngle = chunks.getOrNull(1)?.toFloatOrNull() ?: listAngle
                continue
            }
            if (chunks.firstOrNull() == "TextSize") {
                textSize = chunks.getOrNull(1)?.toFloatOrNull() ?: textSize
                continue
            }
            // B04a: ListScale を named field に昇格 (postLines に落とさない)
            if (chunks.firstOrNull() == "ListScale") {
                listScale = chunks.getOrNull(1)?.toFloatOrNull() ?: listScale
                continue
            }
            if (chunks.firstOrNull() == "Deduction") {
                dedRows.add(CsvRow(chunks))
                continue
            }
            if (chunks.firstOrNull() == "Trapezoid") {
                hasFigure = true
                figureRows.add(CsvRow(chunks))
                continue
            }
            if (chunks.firstOrNull() == "TriTrap") {
                // 旧 TriTrap タグ形式: 内部で "TriTrap" chunks のまま figureRows に入れる。
                // serialize 時に普通三角形行形式 (混在通し番号) に変換して書き出す。
                hasFigure = true
                figureRows.add(CsvRow(chunks))
                continue
            }
            val number = if (chunks.size >= 4) chunks[0].toIntOrNull() else null
            if (number == null || number < 0) {
                (if (!hasFigure) preLines else postLines).add(line)
                continue
            }
            hasFigure = true
            // 普通三角形行で parent が「現在までの三角形数」を超えていれば、混在通し番号で
            // 台形または台形子三角形 (TriTrap chain) を指していると解釈する。
            // 内部表現は旧 TriTrap タグの chunks 構造 ("TriTrap", num, ea, B, C, target_idx, side) に変換。
            val parentIdx = chunks.getOrNull(4)?.toIntOrNull() ?: -1
            if (parentIdx > triCount) {
                val targetIdx = parentIdx - triCount
                val converted = listOf(
                    "TriTrap",
                    chunks[0],
                    chunks.getOrNull(1) ?: "",
                    chunks.getOrNull(2) ?: "",
                    chunks.getOrNull(3) ?: "",
                    targetIdx.toString(),
                    chunks.getOrNull(5) ?: "0",
                )
                figureRows.add(CsvRow(converted))
                continue
            }
            figureRows.add(CsvRow(chunks))
            triCount++
        }
        return CsvDoc(preLines, listAngle, postLines, dedRows, textSize, listScale, figureRows)
    }

    fun serialize(doc: CsvDoc): String {
        val sb = StringBuilder()
        doc.preLines.forEach { sb.append(it).append('\n') }
        // figureRows を CSV 出現順に書く。
        // TriTrap 行は普通三角形行形式 (parent = 三角形数 + target_idx の混在通し番号) で書く。
        // user 2026-06-14「TriTrap みたいな妙なデータ型は廃止しろ」── 旧タグは parse 互換読みのみ、書き出しはタグなし。
        var ntri = 0
        for (row in doc.figureRows) {
            val c = row.chunks
            when {
                c.firstOrNull() == "Trapezoid" -> sb.append(c.joinToString(",")).append('\n')
                c.firstOrNull() == "TriTrap" -> {
                    // TriTrap タグを普通三角形行形式に変換して書く
                    val num = c.getOrNull(1) ?: ""
                    val ea = c.getOrNull(2) ?: ""
                    val b = c.getOrNull(3) ?: ""
                    val cc = c.getOrNull(4) ?: ""
                    val targetIdx = c.getOrNull(5)?.toIntOrNull() ?: 0
                    val side = c.getOrNull(6) ?: "0"
                    val parent = ntri + targetIdx
                    sb.append("$num,$ea,$b,$cc,$parent,$side").append('\n')
                }
                else -> {
                    sb.append(c.joinToString(",")).append('\n')
                    ntri++
                }
            }
        }
        // アプリ writeCSV と同じく図形行の後に書く。値の書式 ("ListAngle, x") も同一
        doc.listAngle?.let { sb.append("ListAngle, ").append(it).append('\n') }
        // B04a: ListScale を named field から書き出す (postLines には残らない)
        doc.listScale?.let { sb.append("ListScale, ").append(it).append('\n') }
        doc.postLines.forEach { sb.append(it).append('\n') }
        // アプリ writeCSV:2785 と同じ書式・同じ位置 (ListScale 等 postLines の後)
        doc.textSize?.let { sb.append("TextSize, ").append(it).append('\n') }
        // アプリ writeCSV:2789-2797 と同じく末尾 (ListScale/TextSize の後) に書く
        doc.dedRows.forEach { sb.append(it.chunks.joinToString(",")).append('\n') }
        return sb.toString()
    }

    /**
     * CsvDoc → TriangleList。figureRows を走査して三角形のみを構築する。
     * 台形 (Trapezoid) と台形子三角形 (TriTrap) は figureRows に保持されているが、
     * build() は三角形パイプラインに集中し、台形/TriTrap は buildFigures() が扱う。
     *
     * 3 phases:
     *   1. 幾何構築 — 180° 基底で add (自動配置 setDimsUnconnectedSideToOuter を含む)
     *   2. 手動配置・メタの復元 — 全行 add の後に当てる
     *   3. リスト回転 — recoverState で絶対角度へ (ADR 0007)
     */
    // B05: applyRecoverState default=true で後方互換。Android は false で呼び二重回転を回避。
    fun build(doc: CsvDoc, applyRecoverState: Boolean = true): TriangleList {
        val trilist = TriangleList()
        val built = mutableListOf<Pair<CsvRow, Triangle>>()

        // phase 1: 幾何構築 (CsvLoader.buildTriangle と同じ分岐)
        // figureRows から三角形行 (Trapezoid/TriTrap 以外) のみ処理する
        for (row in doc.figureRows) {
            val c = row.chunks
            // 台形行・TriTrap 行はスキップ (buildFigures() が扱う)
            val tag = c.firstOrNull()
            if (tag == "Trapezoid" || tag == "TriTrap") continue
            val lengthA = c.getOrNull(1)?.toFloatOrNull() ?: continue
            val lengthB = c.getOrNull(2)?.toFloatOrNull() ?: continue
            val lengthC = c.getOrNull(3)?.toFloatOrNull() ?: continue
            val parent = c.getOrNull(4)?.toIntOrNull() ?: -1
            val conn = c.getOrNull(5)?.toIntOrNull() ?: -1

            if (conn < 1) {
                trilist.add(Triangle(lengthA, lengthB, lengthC, PointXY(0f, 0f), 180f), true)
            } else {
                if (parent < 1 || parent > trilist.size()) continue
                val ptri = trilist.getBy(parent)
                // 完全形式 (列17-19 = cp.side/type/lcr) があれば優先 (CsvLoader.readCParamSafe と同形)
                val cpSide = c.getOrNull(17)?.toIntOrNull()
                val cpType = c.getOrNull(18)?.toIntOrNull()
                val cpLcr = c.getOrNull(19)?.toIntOrNull()
                val cp = if (cpSide != null && cpType != null && cpLcr != null) {
                    ConnParam(cpSide, cpType, cpLcr, lengthA)
                } else {
                    ConnCode.toConnParam(conn, lengthA) ?: continue
                }
                trilist.add(Triangle(ptri, cp, lengthB, lengthC), true)
            }
            val tri = trilist.getBy(trilist.size())
            tri.connectionSide = conn
            built.add(row to tri)
        }

        // phase 2: 手動配置・メタの復元 (CsvLoader.finalizeBuildTriangle と同項目)
        for ((row, tri) in built) applyRowMeta(row.chunks, tri)

        // phase 3: リスト回転。行が無い CSV も angle=0 → -180° でアプリと同じ向き
        trilist.angle = doc.listAngle ?: 0f
        // B05: Android は setEditLists 内で recoverState を呼ぶため false で渡して二重回転を回避
        if (applyRecoverState) trilist.recoverState(PointXY(0f, 0f))
        return trilist
    }

    /**
     * figureRows から台形 (Trapezoid) と台形子三角形 (TriTrap) を構築する。
     * B-FORCE: buildTrapezoids / buildTrapParentedTriangles / buildMixed を統合した後継。
     * trilist は build(doc) で構築済みを渡す。台形の親 (三角形) が trilist に在る前提。
     * scale は三角形と同じ実効倍率。
     *
     * 戻り値: Pair<traps, trapTris>
     *   first  = 台形 (Rectangle) のリスト
     *   second = 台形子三角形 (Triangle) のリスト ── class は普通三角形と同一 Triangle、 list 分離のみ
     *
     * TODO (user 指示 2026-06-15「三角形を台形の子かどうかでデータクラスを分けてるのは変な実装」):
     * trapTris を別 list にする構造を解消し、 全 Triangle を trilist に統合する refactor が要る。
     * 番号管理 (= CSV parent 参照の混在通し番号 = 三角形数 + 台形 idx) の再設計を伴うため
     * 段階的に進める ── 詳細は memory/feedback-tritrap-data-class-unify.md。
     */
    fun buildFigures(doc: CsvDoc, trilist: TriangleList, scale: Float = 1f): Pair<List<Rectangle>, List<Triangle>> {
        val traps = mutableListOf<Rectangle>()
        val trapTris = mutableListOf<Triangle>()
        val s = if (scale > 0f) scale.toDouble() else 1.0
        val sf = if (scale > 0f) scale else 1f

        for (row in doc.figureRows) {
            val c = row.chunks
            when (c.firstOrNull()) {
                "Trapezoid" -> {
                    val length = c.getOrNull(2)?.toFloatOrNull() ?: continue
                    val widthA = c.getOrNull(3)?.toFloatOrNull() ?: continue
                    val widthB = c.getOrNull(4)?.toFloatOrNull() ?: continue
                    val parent = c.getOrNull(5)?.toIntOrNull() ?: -1
                    val side = c.getOrNull(6)?.toIntOrNull() ?: 0
                    val align = c.getOrNull(7)?.toIntOrNull() ?: 0
                    val parentKind = c.getOrNull(8)?.toIntOrNull() ?: 0
                    val l = length * s; val wa = widthA * s; val wb = widthB * s
                    fun indep() = Rectangle(l, wa, wb, angle = INDEP_TRAP_ANGLE, basepoint = PointXY(0f, 0f), alignment = align)
                    when {
                        parent < 1 -> traps.add(indep())
                        parentKind == 1 -> {
                            val pIdx = parent - 1
                            if (pIdx < 0 || pIdx >= traps.size) traps.add(indep())
                            else traps.add(Rectangle(l, wa, wb, nodeA = traps[pIdx], side = side, alignment = align))
                        }
                        parent > trilist.size() -> traps.add(indep())
                        else -> traps.add(Rectangle(l, wa, wb, nodeA = trilist.getBy(parent), side = side, alignment = align))
                    }
                }
                "TriTrap" -> {
                    val b = c.getOrNull(3)?.toFloatOrNull() ?: continue
                    val cc = c.getOrNull(4)?.toFloatOrNull() ?: continue
                    val target = c.getOrNull(5)?.toIntOrNull() ?: continue
                    val side = c.getOrNull(6)?.toIntOrNull() ?: continue
                    val ntrap = traps.size
                    val parent: EditObject? = when {
                        target in 1..ntrap -> traps[target - 1]
                        target > ntrap && (target - ntrap - 1) in trapTris.indices -> trapTris[target - ntrap - 1]
                        else -> null
                    }
                    if (parent == null) continue
                    trapTris.add(Triangle(parent, side, b * sf, cc * sf))
                }
                // 三角形行はスキップ (build() が処理済み)
            }
        }
        return Pair(traps, trapTris)
    }

    /**
     * SoT 一本化版 (2026-06-15): figureRows の出現順を pin した混在 EditList を返す。
     * 既存の build() (三角形のみ) と buildFigures() (台形/TriTrap) を呼んで、結果を
     * CSV 出現順で 1 本の EditList<EditObject> に積む。並行運用 — 既存 API は据え置き。
     *
     * 将来 TriangleList を解体する際の置き換え先候補。今は WebPrimitiveRenderer や DXF
     * writer の入力として使える「型純粋な混在 SoT」を提供する。
     */
    fun buildAll(doc: CsvDoc, scale: Float = 1f, applyRecoverState: Boolean = true): EditList<EditObject> {
        val trilist = build(doc, applyRecoverState)
        val (traps, trapTris) = buildFigures(doc, trilist, scale)
        return composeAll(doc, trilist, traps, trapTris)
    }

    /**
     * SoT 一本化 段3 最終スワップ (2026-06-15): 既に build → setScale → WebOverrides 等の変形が
     * 済んだ trilist / traps / trapTris を「figureRows 順」 で 1 本の EditList<EditObject> に詰め直す。
     * renderCsv は外側で applyJson/setScale を当てた trilist を活かしたまま新 render(list) に流す。
     * buildAll は内部で build/buildFigures を呼んでこの API に委譲するだけ — composition root だけ
     * 分離して「外で変形した状態」 を保持できるようにした。
     */
    fun composeAll(
        doc: CsvDoc,
        trilist: TriangleList,
        traps: List<Rectangle>,
        trapTris: List<Triangle>,
    ): EditList<EditObject> {
        val mixed = EditList<EditObject>()
        var triIdx = 0
        var trapIdx = 0
        var trapTriIdx = 0
        for (row in doc.figureRows) {
            when (row.chunks.firstOrNull()) {
                "Trapezoid" -> if (trapIdx < traps.size) mixed.add(traps[trapIdx++])
                "TriTrap"   -> if (trapTriIdx < trapTris.size) mixed.add(trapTris[trapTriIdx++])
                else        -> if (triIdx < trilist.size()) {
                    triIdx += 1
                    mixed.add(trilist.getBy(triIdx))
                }
            }
        }
        return mixed
    }

    /**
     * dedRows → DeductionList。アプリの CsvLoader.buildDeductions (CsvLoader.kt:369-392、
     * viewscale=1) と同値: 列 8-11 (point/pointFlag) は `PointXY(x, -y)` で Y 反転して
     * ビュー空間 (y 下向き) に戻し、列 12 が空でなければ shapeAngle。type (列 6) は
     * "Box"/"Circle" 文字列、pl は MainActivity.typeToInt:924 と同写像 (Box=1, Circle=2)。
     *
     * 回転はここでは当てない — アプリのロード経路 (MainActivity.setEditLists:2904-2920) は
     * trilist.recoverState だけで dedlist は回さない (CSV の控除座標は保存時点の絶対値)。
     * 対話回転 (fabRotate:1584-1591) は別経路で、web では行の座標書き換え
     * (WebDeduction.rotateDeductionLine) が担う
     */
    // B02: 既存 1 引数版は 2 引数版 (viewscale=1f) へのラッパー。後方互換維持。
    fun buildDeductions(doc: CsvDoc): DeductionList = buildDeductions(doc, viewscale = 1f)

    // B-FORCE (2026-06-15): buildTrapezoids / buildTrapParentedTriangles / buildMixed / MixedBuild を廃止。
    // 後継は buildFigures(doc, trilist, scale) — figureRows を 1 ループで全図形種別を処理する。

    // -------------------------------------------------------------------------
    // B01: extractHeader / bakeHeader
    // -------------------------------------------------------------------------

    /**
     * CsvDoc.preLines から koujiname/rosenname/gyousyaname/zumennum を構造化して返す。
     * 旧 CsvLoader.readCsvHeaderLines:347-358 相当。後勝ち (同名行が複数あれば最後を使う)。
     */
    fun extractHeader(doc: CsvDoc): HeaderValues {
        val h = HeaderValues()
        for (line in doc.preLines) {
            val chunks = line.split(",").map { it.trim() }
            if (chunks.size < 2) continue
            when (chunks[0]) {
                "koujiname"   -> h.koujiname   = chunks[1]
                "rosenname"   -> h.rosenname   = chunks[1]
                "gyousyaname" -> h.gyousyaname = chunks[1]
                "zumennum"    -> h.zumennum    = chunks[1]
            }
        }
        return h
    }

    /**
     * HeaderValues を CsvDoc.preLines の先頭 4 行として書き込む。
     * 既存の同名行は filter で除去してから先頭に挿入するので重複しない。
     * BOM 付きコメント等の非ヘッダ行は末尾に残る。
     */
    fun bakeHeader(values: HeaderValues, doc: CsvDoc): CsvDoc {
        val headerKeys = setOf("koujiname", "rosenname", "gyousyaname", "zumennum")
        val cleaned = doc.preLines.filter { line ->
            line.split(",").firstOrNull()?.trim() !in headerKeys
        }
        val newPre = listOf(
            "koujiname, ${values.koujiname}",
            "rosenname, ${values.rosenname}",
            "gyousyaname, ${values.gyousyaname}",
            "zumennum, ${values.zumennum}",
        ) + cleaned
        return doc.copy(preLines = newPre)
    }

    // -------------------------------------------------------------------------
    // B02: buildDeductions(doc, viewscale) overload
    // -------------------------------------------------------------------------

    /**
     * viewscale 引数付き overload。旧 CsvLoader.buildDeductions:369-392 相当。
     * Android は viewscale != 1 で動くため、point/pointFlag を viewscale 倍して
     * ビュー空間に合わせる。viewscale=1f (デフォルト) は既存 1 引数版と完全等価。
     */
    fun buildDeductions(doc: CsvDoc, viewscale: Float): DeductionList {
        val dedlist = DeductionList()
        val s = if (viewscale > 0f) viewscale.toDouble() else 1.0
        for (row in doc.dedRows) {
            val c = row.chunks
            val num = c.getOrNull(1)?.toIntOrNull() ?: continue
            val lengthX = c.getOrNull(3)?.toFloatOrNull() ?: continue
            val lengthY = c.getOrNull(4)?.toFloatOrNull() ?: continue
            val pn = c.getOrNull(5)?.toIntOrNull() ?: 0
            val type = c.getOrNull(6) ?: ""
            val px = c.getOrNull(8)?.toFloatOrNull() ?: continue
            val py = c.getOrNull(9)?.toFloatOrNull() ?: continue
            val fx = c.getOrNull(10)?.toFloatOrNull() ?: continue
            val fy = c.getOrNull(11)?.toFloatOrNull() ?: continue
            dedlist.add(
                Deduction(
                    InputParameter(
                        c.getOrNull(2) ?: "", type, num,
                        lengthX, lengthY, 0f,
                        pn, typeToInt(type),
                        PointXY(px, -py).scale(s),
                        PointXY(fx, -fy).scale(s),
                    )
                )
            )
            val sa = c.getOrNull(12)
            if (!sa.isNullOrEmpty()) sa.toDoubleOrNull()?.let { dedlist.get(dedlist.size()).shapeAngle = it }
        }
        return dedlist
    }

    // -------------------------------------------------------------------------
    // B03: bakeDeductions(dedlist, doc, viewscale)
    // -------------------------------------------------------------------------

    /**
     * 編集後の DeductionList を CsvDoc.dedRows に焼き直す。
     * 旧 MainActivity.writeCSV:2790-2798 相当。viewscale の逆数で実寸・y 反転して書く。
     * viewscale=1f のとき既存 dedRows と同値になる (round-trip 等価)。
     */
    fun bakeDeductions(dedlist: DeductionList, doc: CsvDoc, viewscale: Float): CsvDoc {
        val s = if (viewscale > 0f) viewscale.toDouble() else 1.0
        val newDedRows = (1..dedlist.size()).map { i ->
            val dd = dedlist.get(i)
            val pAt = dd.point.scale(PointXY(0f, 0f), 1.0 / s, -1.0 / s)
            val fAt = dd.pointFlag.scale(PointXY(0f, 0f), 1.0 / s, -1.0 / s)
            CsvRow(listOf(
                "Deduction", "${dd.num}", dd.name, "${dd.lengthX}", "${dd.lengthY}",
                "${dd.overlap_to}", dd.type, "${dd.angle}",
                "${pAt.x}", "${pAt.y}", "${fAt.x}", "${fAt.y}", "${dd.shapeAngle}",
            ))
        }
        return doc.copy(dedRows = newDedRows)
    }

    // -------------------------------------------------------------------------
    // B04: applyListParams(doc, trilist, setTextSize)
    // -------------------------------------------------------------------------

    /**
     * CsvDoc 内の TextSize / ListScale をアプリ状態に適用する。
     * 旧 CsvLoader.readListParameter:394-409 相当。listAngle は build() 内適用済みなので触らない。
     * setTextSize は MyView の textSize setter を渡す (Android 専用経路; Web は別途扱う)。
     * ListScale: doc.listScale (named field) を優先し、fallback で postLines から探す (後方互換)。
     */
    fun applyListParams(
        doc: CsvDoc,
        trilist: TriangleList,
        setTextSize: (Float) -> Unit,
    ) {
        doc.textSize?.let { setTextSize(it) }
        val scale = doc.listScale ?: doc.postLines.firstNotNullOfOrNull { line ->
            val chunks = line.split(",").map { it.trim() }
            if (chunks.getOrNull(0) == "ListScale") chunks.getOrNull(1)?.toFloatOrNull() else null
        }
        scale?.let { trilist.setScale(PointXY(0f, 0f), it) }
    }

    /** MainActivity.typeToInt:924-929 と同写像 */
    fun typeToInt(type: String): Int = when (type) {
        "Box" -> 1
        "Circle" -> 2
        else -> 0
    }

    private fun applyRowMeta(c: List<String>, tri: Triangle) {
        // 測点名 (列6) と色 (列10) — 描画には出ないが bake / XLSX 経路のため保持
        c.getOrNull(6)?.let { if (it.isNotEmpty()) tri.name = it }
        c.getOrNull(10)?.toIntOrNull()?.let { tri.setColor(it) }
        // 番号サークル位置 (列7-9、ユーザー移動時のみ)。座標は絶対値で、flag=true が
        // recoverState の回転対象から外す
        if (c.getOrNull(9)?.toBoolean() == true) {
            val px = c.getOrNull(7)?.toFloatOrNull()
            val py = c.getOrNull(8)?.toFloatOrNull()
            if (px != null && py != null) tri.setPointNumber(PointXY(px, py), true)
        }
        // 寸法アライメント (列11-16)
        val aligns = (11..16).map { c.getOrNull(it)?.toIntOrNull() }
        if (aligns.all { it != null }) {
            tri.setDimAligns(aligns[0]!!, aligns[1]!!, aligns[2]!!, aligns[3]!!, aligns[4]!!, aligns[5]!!)
        }
        // 寸法の手動フラグ (列20-21)
        if (c.getOrNull(21) != null) {
            tri.dim.flag[1].isMovedByUser = c.getOrNull(20)?.toBoolean() ?: false
            tri.dim.flag[2].isMovedByUser = c.getOrNull(21)?.toBoolean() ?: false
        }
        // 測点アライメント (列26-27)
        c.getOrNull(26)?.toIntOrNull()?.let { tri.dim.horizontal.s = it }
        c.getOrNull(27)?.let { tri.dim.flagS.isMovedByUser = it.toBoolean() }
    }

    /**
     * 構築済み model → 完全形式 28 列の CsvDoc。列順・値はアプリの保存
     * (MainActivity.writeCSV:2760-2776) と同一 — web が書く CSV をアプリで開いても
     * 手動配置 (寸法フリップ・番号移動) が失われない。
     * preLines/postLines は元文書から引き継ぐ (ヘッダ・Deduction 等の素通し)
     */
    fun bake(trilist: TriangleList, original: CsvDoc): CsvDoc {
        // 三角形行を完全形式 28 列で再構築する。
        val triRows = (1..trilist.size()).map { i ->
            val mt = trilist.getBy(i)
            val pn = mt.pointnumber
            val cp = ConnCode.toConnParam(mt.connectionSide, mt.lengthNotSized[0], mt.cParam_.lcr)
                ?: mt.cParam_
            CsvRow(
                listOf(
                    "${mt.mynumber}", "${mt.lengthA_}", "${mt.lengthB_}", "${mt.lengthC_}",
                    "${mt.parentnumber}", "${mt.connectionSide}",
                    mt.name, "${pn.x}", "${pn.y}", "${mt.pointNumber.flag.isMovedByUser}",
                    "${mt.mycolor}",
                    "${mt.dim.horizontal.a}", "${mt.dim.horizontal.b}", "${mt.dim.horizontal.c}",
                    "${mt.dim.vertical.a}", "${mt.dim.vertical.b}", "${mt.dim.vertical.c}",
                    "${cp.side}", "${cp.type}", "${cp.lcr}",
                    "${mt.dim.flag[1].isMovedByUser}", "${mt.dim.flag[2].isMovedByUser}",
                    "${mt.angle}", "${mt.pointCA.x}", "${mt.pointCA.y}", "${mt.angleInLocal_}",
                    "${mt.dim.horizontal.s}", "${mt.dim.flagS.isMovedByUser}",
                )
            )
        }
        // figureRows を再構築: 三角形行は triRows で置き換え、台形/TriTrap 行は素通し。
        // これで「bake が三角形のみ再構築して台形を捨てる」問題 (user 報告 2026-06-14) を解消。
        var triIdx = 0
        val newFigureRows = original.figureRows.map { row ->
            val tag = row.chunks.firstOrNull()
            if (tag == "Trapezoid" || tag == "TriTrap") {
                row  // 台形/TriTrap は元の行をそのまま保持
            } else {
                triRows.getOrElse(triIdx) { row }.also { triIdx++ }
            }
        }
        // dedRows は素通し。B04a: listScale を trilist.scale から取得して named field に書き込む。
        return CsvDoc(
            original.preLines, trilist.angle, original.postLines, original.dedRows, original.textSize,
            trilist.scale,  // listScale
            newFigureRows,
        )
    }

    /**
     * Android 用 combined bake (B08): trilist + dedlist + header + original を
     * 1 呼び出しで CsvDoc に焼き直す。未知行 (figureRows の台形/TriTrap 部分 /
     * preLines の補助行 / postLines / textSize) は original から素通し。
     *
     * 内部は bakeHeader → bakeDeductions → bake(trilist, doc) の順で呼ぶ。
     * 順序固定でアトミック、呼び出し側 (MainActivity) は中間 doc を意識しない。
     *
     * Web 側は 1 段ずつ bake する経路を維持するため、既存の個別 fun は変更しない。
     */
    fun bake(
        trilist: TriangleList,
        dedlist: DeductionList,
        header: HeaderValues,
        original: CsvDoc,
        viewscale: Float,
    ): CsvDoc {
        val withHeader = bakeHeader(header, original)
        val withDed = bakeDeductions(dedlist, withHeader, viewscale)
        return bake(trilist, withDed)
    }
}
