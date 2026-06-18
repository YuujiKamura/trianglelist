package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.ConnCode
import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.setNumber
import com.jpaver.trianglelist.editmodel.setOnRectangle
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.setColor
import com.jpaver.trianglelist.setDimAligns
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
     * figureRows = 三角形 + 台形 (Rectangle) を CSV 出現順で保持する SoT。
     *   chunks[0] == "Rectangle" → 台形 (Rectangle) 行
     *   chunks[0].toIntOrNull() != null → 三角形行 (または混在通し番号の台形子三角形行)
     * user 確定 2026-06-16「まだトレープゾイド言ってんのか、トライトラップとか排除しろと言っただろうに」
     * ── CSV タグは Rectangle 1 種 (Kotlin class 名と一致)、後方互換も切る。
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
        // (parent が三角形数を超えていれば台形 (Rectangle) 親の混在通し番号と解釈するため)
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
            if (chunks.firstOrNull() == "Rectangle") {
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
            // user 確定 2026-06-16「親がどうとかでデータ型を分けるな、 トライトラップとか排除しろ」
            // ── Triangle 行は parent (混在通し番号) で親種別を解決する 1 種類のみ。
            // CSV タグは Rectangle のみ、 TriTrap タグは完全廃止 (後方互換も切る)。
            // triCount は build() の trilist.size() 上限判定にだけ使う。
            figureRows.add(CsvRow(chunks))
            val parentIdx = chunks.getOrNull(4)?.toIntOrNull() ?: -1
            // 普通三角形 (parent が三角形数以内、 つまり親が Triangle) のときだけ triCount を進める。
            // parent が三角形数を超える Triangle 行は build() で skip され buildMixed で構築される。
            if (parentIdx <= triCount) triCount++
        }
        return CsvDoc(preLines, listAngle, postLines, dedRows, textSize, listScale, figureRows)
    }

    fun serialize(doc: CsvDoc): String {
        val sb = StringBuilder()
        doc.preLines.forEach { sb.append(it).append('\n') }
        // figureRows を CSV 出現順に書く。 user 確定 2026-06-16「TriTrap タグ排除」
        // ── 台形は Rectangle タグ、 台形子三角形は普通三角形行 (parent = 混在通し番号) で書く。
        for (row in doc.figureRows) {
            sb.append(row.chunks.joinToString(",")).append('\n')
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
     * 台形 (Rectangle) は figureRows に保持されているが、build() は三角形パイプラインに集中し、
     * Rectangle は buildMixed() が扱う。 user 確定 2026-06-16: CSV タグは Rectangle 1 種 (TriTrap タグ廃止)。
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
        // figureRows から三角形行 (Rectangle 以外) のみ処理する
        for (row in doc.figureRows) {
            val c = row.chunks
            // Rectangle 行はスキップ (buildMixed() が扱う)
            val tag = c.firstOrNull()
            if (tag == "Rectangle") continue
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
                trilist.add(Triangle(ptri as Triangle?, cp, lengthB, lengthC), true)
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
     * figureRows から Rectangle (台形) と Triangle (台形子三角形を含む) を混在 EditList に構築する。
     * user 確定 2026-06-16「親がどうとかでデータ型を分けるな、 トライトラップとか排除しろ」 ──
     * CSV タグは Rectangle 1 種、 台形子三角形は普通 Triangle 行で parent=混在通し番号で表現。
     *
     * trilist は build(doc) で構築済みを渡す。CSV の parent は原則として figureRows の混在通し番号。
     * buildMixed はその番号から CycleShape を引き、接続自体は CycleShape.getLine(side) / initByParent に委譲する。
     * scale は三角形と同じ実効倍率。
     *
     * 戻り値: 混在 EditList<CycleShape> ── 全 Triangle/Rectangle が同じ list に存在 (figureRows 順)。
     * caller が分離したい場合は filterIsInstance<Rectangle>() / Triangle.
     */
    fun buildMixed(doc: CsvDoc, trilist: TriangleList, scale: Float = 1f): EditList<CycleShape> {
        val mixed = EditList<CycleShape>()
        val s = if (scale > 0f) scale.toDouble() else 1.0
        val sf = if (scale > 0f) scale else 1f
        val mixedObjects = mutableListOf<CycleShape>()
        val traps = mutableListOf<Rectangle>()
        val trapTris = mutableListOf<Triangle>()
        var triIdx = 0  // build(doc) に含まれる通常 Triangle 行の pull 位置

        fun mixedByNumber(number: Int): CycleShape? =
            if (number in 1..mixedObjects.size) mixedObjects[number - 1] else null

        // 旧 CSV 互換: RectChild 廃止移行中の parent = trilist.size() + Rectangle idx 形式。
        // 新規経路は mixedByNumber(parent) で解決する。
        fun legacyCompositeParent(number: Int): CycleShape? {
            val target = number - trilist.size()
            val ntrap = traps.size
            return when {
                target in 1..ntrap -> traps[target - 1]
                target > ntrap && (target - ntrap - 1) in trapTris.indices -> trapTris[target - ntrap - 1]
                else -> null
            }
        }

        fun append(obj: CycleShape) {
            mixed.add(obj)
            mixedObjects.add(obj)
        }

        fun pullTriangle(): Triangle? {
            triIdx += 1
            return if (triIdx <= trilist.size()) trilist.getBy(triIdx) else null
        }

        for (row in doc.figureRows) {
            val c = row.chunks
            when (c.firstOrNull()) {
                "Rectangle" -> {
                    val length = c.getOrNull(2)?.toFloatOrNull() ?: continue
                    val widthA = c.getOrNull(3)?.toFloatOrNull() ?: continue
                    val widthB = c.getOrNull(4)?.toFloatOrNull() ?: continue
                    val parent = c.getOrNull(5)?.toIntOrNull() ?: -1
                    val side = c.getOrNull(6)?.toIntOrNull() ?: 0
                    val align = c.getOrNull(7)?.toIntOrNull() ?: 0
                    val parentKind = c.getOrNull(8)?.toIntOrNull() ?: 0
                    val l = length * s; val wa = widthA * s; val wb = widthB * s
                    fun indep() = Rectangle(l, wa, wb, angle = INDEP_TRAP_ANGLE + (doc.listAngle ?: 0f), basepoint = PointXY(0f, 0f), alignment = align)
                    fun parentObject(): CycleShape? {
                        if (parent < 1) return null
                        // Primary schema: parent is the mixed figure number, independent of shape kind.
                        mixedByNumber(parent)?.let { return it }
                        // Compatibility for older Rectangle-on-Rectangle rows where parentKind=1 used local Rectangle number.
                        if (parentKind == 1) traps.getOrNull(parent - 1)?.let { return it }
                        // Compatibility for old triangle-parent rows whose parent number targeted the prebuilt TriangleList.
                        return if (parent in 1..trilist.size()) trilist.getBy(parent) else null
                    }
                    val pObj = parentObject()
                    val rect = when {
                        parent < 1 -> indep()
                        pObj == null -> indep()
                        else -> Rectangle(l, wa, wb, nodeA = pObj, side = side, alignment = align)
                    }
                    traps.add(rect)
                    applyRowMeta(c, rect)
                    append(rect)
                }
                else -> {
                    val lengthA = c.getOrNull(1)?.toFloatOrNull() ?: continue
                    val lengthB = c.getOrNull(2)?.toFloatOrNull() ?: continue
                    val lengthC = c.getOrNull(3)?.toFloatOrNull() ?: continue
                    val parent = c.getOrNull(4)?.toIntOrNull() ?: -1
                    val conn = c.getOrNull(5)?.toIntOrNull() ?: -1

                    // 二重構築の撤廃 (2026-06-16): build(doc) が trilist に積む普通三角形 (独立 or 親が
                    // 「ここまでに積んだ三角形」) は、recoverState/scale/override/番号まで処理済みの
                    // インスタンスをそのまま再利用する。build() と同一判定 (parent in 1..triIdx) で同期。
                    // Rectangle 子三角形 (親が混在通し番号で trilist 外) のみ従来どおり新規構築する。
                    // 親が Rectangle (混在通し番号で Rectangle が居る) なら trilist 再利用しない ──
                    // 独立 Triangle のままだと親 Rectangle と無関係に配置される、 setOnRectangle で
                    // 親辺に乗せる必要があるため。
                    val parentIsRectInMixed = parent > 0 && mixedByNumber(parent) is Rectangle
                    val isTrilistTri = (conn < 1 || parent in 1..triIdx) && !parentIsRectInMixed
                    val reused = if (isTrilistTri) pullTriangle() else null
                    if (reused != null) {
                        append(reused)
                    } else {
                        fun indep() = Triangle(lengthA * sf, lengthB * sf, lengthC * sf, PointXY(0f, 0f), 180f)
                        val tri = if (conn < 1 || parent < 1) {
                            indep()
                        } else {
                            // 混在通し番号で親を探す
                            val pObj = mixedByNumber(parent)
                            if (pObj == null) {
                                indep() // 親が見つからなければ独立に
                            } else {
                                // 完全形式 (列 17-19 = cp.side/type/lcr) の読み取り
                                val cpSide = c.getOrNull(17)?.toIntOrNull()
                                val cpType = c.getOrNull(18)?.toIntOrNull()
                                val cpLcr = c.getOrNull(19)?.toIntOrNull()
                                        // Rectangle 親: conn は直接 side 番号 (1=B/2=C/3=D)。
                                // ConnCode に通すと code=3 が「B辺双重断面」と衝突するため、親種別で分岐。
                                val child = when {
                                    cpSide != null && cpType != null && cpLcr != null -> {
                                        val cp = ConnParam(cpSide, cpType, cpLcr, lengthA * sf)
                                        Triangle(pObj as CycleShape, cp, lengthB * sf, lengthC * sf)
                                    }
                                    pObj is Rectangle -> {
                                        // 環閉合順統一規約 (2026-06-18) で Rectangle 親対応の専用
                                        // setOnRectangle path を通す ── Triangle 親限定 constructor
                                        // (Triangle?) と分離し、 既存 Triangle 親 logic は不変。
                                        val t = Triangle().apply { setNumber(parent) }
                                        t.setOnRectangle(pObj, conn, lengthB * sf, lengthC * sf)
                                        t
                                    }
                                    else -> {
                                        val cp = ConnCode.toConnParam(conn, lengthA * sf)
                                        if (cp != null) Triangle(pObj as CycleShape, cp, lengthB * sf, lengthC * sf)
                                        else Triangle(pObj, conn, lengthB * sf, lengthC * sf)
                                    }
                                }
                                child.parentnumber = parent
                                child
                            }
                        }
                        tri.connectionSide = conn
                        applyRowMeta(c, tri)
                        append(tri)
                    }
                }
            }
        }
        return mixed
    }

    /**
     * SoT 一本化 (2026-06-15): buildMixed の thin wrapper、 build() を呼んで trilist を作ってから委譲。
     * 既存 caller (= CsvCodecBuildAllTest) は無変更で動く。 外側で setScale / applyJson 等の変形を入れたい
     * 経路 (= WebPrimitiveRenderer.renderCsv) は build() と buildMixed() を直接呼ぶ。
     */
    fun buildAll(doc: CsvDoc, scale: Float = 1f, applyRecoverState: Boolean = true): EditList<CycleShape> =
        buildMixed(doc, build(doc, applyRecoverState), scale)

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

    // B-FORCE (2026-06-15): Rectangle 系の旧 build 関数群 / buildMixed / MixedBuild を廃止。
    // 後継は buildMixed(doc, trilist, scale) — figureRows を 1 ループで全図形種別を処理する。

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

    private fun applyRowMeta(c: List<String>, obj: CycleShape) {
        // 測点名: Triangle は列6、Rectangle は列9
        val nameIdx = if (c.firstOrNull() == "Rectangle") 9 else 6
        c.getOrNull(nameIdx)?.let { if (it.isNotEmpty()) obj.name = it }

        if (obj is Triangle) {
            // 色 (列10)
            c.getOrNull(10)?.toIntOrNull()?.let { obj.setColor(it) }
            // 番号サークル位置 (列7-9、ユーザー移動時のみ)。座標は絶対値で、flag=true が
            // recoverState の回転対象から外す
            if (c.getOrNull(9)?.toBoolean() == true) {
                val px = c.getOrNull(7)?.toFloatOrNull()
                val py = c.getOrNull(8)?.toFloatOrNull()
                if (px != null && py != null) {
                    obj.pointnumber = PointXY(px, py)
                    obj.pointNumber.flag.isMovedByUser = true
                    obj.pointNumber.flag.isAutoAligned = false
                }
            }
            // 寸法アライメント (列11-16)
            val aligns = (11..16).map { c.getOrNull(it)?.toIntOrNull() }
            if (aligns.all { it != null }) {
                obj.setDimAligns(aligns[0]!!, aligns[1]!!, aligns[2]!!, aligns[3]!!, aligns[4]!!, aligns[5]!!)
            }
            // 寸法の手動フラグ (列20-21)
            if (c.getOrNull(21) != null) {
                obj.dim.flag[1].isMovedByUser = c.getOrNull(20)?.toBoolean() ?: false
                obj.dim.flag[2].isMovedByUser = c.getOrNull(21)?.toBoolean() ?: false
            }
            // 測点アライメント (列26-27)
            c.getOrNull(26)?.toIntOrNull()?.let { obj.dim.horizontal.s = it }
            c.getOrNull(27)?.let { obj.dim.flagS.isMovedByUser = it.toBoolean() }
        }
    }

    /**
     * 構築済み model → 完全形式 28 列の CsvDoc。列順・値はアプリの保存
     * (MainActivity.writeCSV:2760-2776) と同一 — web が書く CSV をアプリで開いても
     * 手動配置 (寸法フリップ・番号移動) が失われない。
     * preLines/postLines は元文書から引き継ぐ (ヘッダ・Deduction 等の素通し)
     */
    fun bake(trilist: TriangleList, original: CsvDoc): CsvDoc {
        // 三角形行を完全形式 28 列で再構築する。
        val triRows = (1..trilist.size()).map { i -> rowForTriangle(trilist.getBy(i)) }
        // figureRows を再構築: 三角形行は triRows で置き換え、Rectangle 行は素通し。
        // user 確定 2026-06-16: CSV タグは Rectangle 1 種 (Trapezoid / TriTrap タグ廃止)。
        var triIdx = 0
        val newFigureRows = original.figureRows.map { row ->
            val tag = row.chunks.firstOrNull()
            if (tag == "Rectangle") {
                row  // Rectangle は元の行をそのまま保持
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
     * mixed EditList を WebPrimitiveRenderer と同じ図形通し番号で焼く。通常 Triangle は
     * TriangleList と同じインスタンス、Rectangle 子 Triangle は mixed 側だけに存在するため、
     * Web UI の override を CSV 行へ戻す経路ではこちらを使う。
     */
    fun bakeMixed(list: EditList<CycleShape>, original: CsvDoc, listAngle: Float?, listScale: Float?): CsvDoc {
        val newFigureRows = original.figureRows.mapIndexed { idx, row ->
            if (row.chunks.firstOrNull() == "Rectangle") {
                row
            } else {
                val obj = if (idx + 1 in 1..list.size()) list.get(idx + 1) else null
                val number = row.chunks.firstOrNull()?.toIntOrNull()
                if (obj is Triangle) rowForTriangle(obj, number ?: obj.mynumber) else row
            }
        }
        return CsvDoc(
            original.preLines, listAngle ?: original.listAngle, original.postLines, original.dedRows,
            original.textSize, listScale ?: original.listScale, newFigureRows,
        )
    }

    private fun rowForTriangle(mt: Triangle, number: Int = mt.mynumber): CsvRow {
        val pn = mt.pointnumber
        val cp = ConnCode.toConnParam(mt.connectionSide, mt.lengthNotSized[0], mt.cParam_.lcr)
            ?: mt.cParam_
        return CsvRow(
            listOf(
                "$number", "${mt.lengthA_}", "${mt.lengthB_}", "${mt.lengthC_}",
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

    /**
     * Android 用 combined bake (B08): trilist + dedlist + header + original を
     * 1 呼び出しで CsvDoc に焼き直す。未知行 (figureRows の Rectangle 部分 /
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
