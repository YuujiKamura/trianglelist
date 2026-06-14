package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.ConnCode
import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
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
     * CSV 文書。preLines = 最初の三角形行より前の行 (ヘッダ等)、postLines = それ以降の
     * 非三角形行 (ListScale 等)。どちらも原文のまま保持して書き戻す。
     * ListAngle 行だけは数値として取り出す (リスト回転の SoT、ADR 0007)。
     * TextSize 行も同様に昇格 (寸法文字サイズの SoT。アプリ writeCSV:2785 が書き
     * CsvLoader.readListParameter:404-407 が読む行。単位はアプリの view px、初期値 30)。
     * dedRows = "Deduction" 先頭の控除行 (ADR 0008 の残課題の昇格)。chunks は列0 =
     * "Deduction" を含む生の列で、未知の追加列 (14 列目以降) も保持して書き戻す
     */
    data class CsvDoc(
        val preLines: List<String>,
        val rows: List<CsvRow>,
        val listAngle: Float?,
        val postLines: List<String>,
        val dedRows: List<CsvRow> = emptyList(),
        val textSize: Float? = null,
        val trapRows: List<CsvRow> = emptyList(),
        // 図形行 (三角形 + 台形) を CSV の出現順そのままで保持する。種別は chunks[0] が
        // "Trapezoid" か数値かで判る。位置順ビルド (混在接続: 親が先に在れば子が解決できる) の入力。
        // 既存の rows/trapRows (種別分離) は不変 — 行順は分離で失われるため、ここに別途順序を持つ。
        val figureRows: List<CsvRow> = emptyList(),
        // 台形を親に持つ三角形の行 ("TriTrap" タグ)。普通の三角形行 (rows) とは別バケツにすることで
        // build() (= rows を使う golden パイプライン) はこれを一切見ない → golden 自明に不変。
        // 列: TriTrap, num, ea(情報・台形辺長で上書き), eb(=B), ec(=C), parent(台形群index 1始まり), side(1=左脚/2=上辺/3=右脚)。
        // 台形がビルド済みになってから buildTrapParentedTriangles で Triangle(台形, side, B, C) として構築する
        // (三角形ビルドが台形より先で親を参照できない「ビルド順の循環」を、別パスに後回しして解く)。
        val trapParentedTriRows: List<CsvRow> = emptyList(),
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
        val rows = mutableListOf<CsvRow>()
        val dedRows = mutableListOf<CsvRow>()
        val trapRows = mutableListOf<CsvRow>()
        val trapParentedTriRows = mutableListOf<CsvRow>()  // "TriTrap" 行 (台形を親に持つ三角形)
        val figureRows = mutableListOf<CsvRow>()   // 三角形+台形を出現順で保持 (位置順ビルド用)
        var listAngle: Float? = null
        var textSize: Float? = null
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
            if (chunks.firstOrNull() == "Deduction") {
                dedRows.add(CsvRow(chunks))
                continue
            }
            if (chunks.firstOrNull() == "Trapezoid") {
                val r = CsvRow(chunks)
                trapRows.add(r)
                figureRows.add(r)
                continue
            }
            if (chunks.firstOrNull() == "TriTrap") {
                // 台形を親に持つ三角形。build() (rows) には入れない → golden 不変。順序保持のため figureRows には入れる
                val r = CsvRow(chunks)
                trapParentedTriRows.add(r)
                figureRows.add(r)
                continue
            }
            val number = if (chunks.size >= 4) chunks[0].toIntOrNull() else null
            if (number == null || number < 0) {
                (if (rows.isEmpty()) preLines else postLines).add(line)
                continue
            }
            // 普通三角形行で parent が「現在までの三角形数」を超えていれば、混在通し番号で
            // 台形または台形子三角形 (TriTrap chain) を指していると解釈する。
            //   excess = parent - rows.size
            //   1 <= excess <= 台形数            → 台形親 (trapIdx = excess)
            //   excess > 台形数                  → TriTrap 親 (ttIdx = excess - 台形数、tritrap chain)
            // 内部表現は旧 TriTrap タグの chunks 構造 ("TriTrap", num, ea, B, C, target_idx, side) に
            // 揃え、target_idx は 「1..台形数 = 台形群 idx、台形数+1..= tritrap 通し idx」の連続番号。
            // build 側 (buildTrapParentedTriangles) が範囲判定で親 (Rectangle / Triangle) を取り分ける。
            // user 2026-06-15「台形にくっついてる三角形からも派生したい」── tritrap chain を model 直結。
            val parentIdx = chunks.getOrNull(4)?.toIntOrNull() ?: -1
            if (parentIdx > rows.size) {
                val targetIdx = parentIdx - rows.size
                val converted = listOf(
                    "TriTrap",
                    chunks[0],
                    chunks.getOrNull(1) ?: "",
                    chunks.getOrNull(2) ?: "",
                    chunks.getOrNull(3) ?: "",
                    targetIdx.toString(),
                    chunks.getOrNull(5) ?: "0",
                )
                val r = CsvRow(converted)
                trapParentedTriRows.add(r)
                figureRows.add(r)
                continue
            }
            val r = CsvRow(chunks)
            rows.add(r)
            figureRows.add(r)
        }
        return CsvDoc(preLines, rows, listAngle, postLines, dedRows, textSize, trapRows, figureRows, trapParentedTriRows)
    }

    fun serialize(doc: CsvDoc): String {
        val sb = StringBuilder()
        doc.preLines.forEach { sb.append(it).append('\n') }
        doc.rows.forEach { sb.append(it.chunks.joinToString(",")).append('\n') }
        // アプリ writeCSV と同じく三角形行の後に書く。値の書式 ("ListAngle, x") も同一
        doc.listAngle?.let { sb.append("ListAngle, ").append(it).append('\n') }
        doc.postLines.forEach { sb.append(it).append('\n') }
        // アプリ writeCSV:2785 と同じ書式・同じ位置 (ListScale 等 postLines の後)
        doc.textSize?.let { sb.append("TextSize, ").append(it).append('\n') }
        // アプリ writeCSV:2789-2797 と同じく末尾 (ListScale/TextSize の後) に書く
        doc.dedRows.forEach { sb.append(it.chunks.joinToString(",")).append('\n') }
        // 台形行は控除の後に書き戻す (schema evolution の定石、未知列も chunks 生のまま素通し)
        doc.trapRows.forEach { sb.append(it.chunks.joinToString(",")).append('\n') }
        // 台形を親に持つ三角形は普通三角形行形式で書く (parent = 三角形数 + 台形 idx の混在通し番号)。
        // 旧 TriTrap タグは parse 側で互換読みするが、新規書き出しはタグを使わない。
        // user 2026-06-14「TriTrap みたいな妙なデータ型は廃止しろ」── CSV schema からタグを廃止。
        val ntri = doc.rows.size
        doc.trapParentedTriRows.forEach { r ->
            val c = r.chunks
            val num = c.getOrNull(1) ?: ""
            val ea = c.getOrNull(2) ?: ""
            val b = c.getOrNull(3) ?: ""
            val cc = c.getOrNull(4) ?: ""
            val trapIdx = c.getOrNull(5)?.toIntOrNull() ?: 0
            val side = c.getOrNull(6) ?: "0"
            val parent = ntri + trapIdx
            sb.append("$num,$ea,$b,$cc,$parent,$side").append('\n')
        }
        return sb.toString()
    }

    /**
     * CsvDoc → TriangleList。3 phases:
     *   1. 幾何構築 — 180° 基底で add (自動配置 setDimsUnconnectedSideToOuter を含む)
     *   2. 手動配置・メタの復元 — 全行 add の後に当てる (CsvLoader の行ごと finalize と違い、
     *      後続の子の add が先行行の保存値を潰せない)
     *   3. リスト回転 — recoverState で絶対角度へ (アプリ load 経路と同一、ADR 0007)
     */
    fun build(doc: CsvDoc): TriangleList {
        val trilist = TriangleList()
        val built = mutableListOf<Pair<CsvRow, Triangle>>()

        // phase 1: 幾何構築 (CsvLoader.buildTriangle と同じ分岐)
        for (row in doc.rows) {
            val c = row.chunks
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
        trilist.recoverState(PointXY(0f, 0f))
        return trilist
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
    fun buildDeductions(doc: CsvDoc): DeductionList {
        val dedlist = DeductionList()
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
                        PointXY(px, -py),
                        PointXY(fx, -fy),
                    )
                )
            )
            val sa = c.getOrNull(12)
            if (!sa.isNullOrEmpty()) sa.toDoubleOrNull()?.let { dedlist.get(dedlist.size()).shapeAngle = it }
        }
        return dedlist
    }

    /**
     * trapRows → Rectangle リスト (混在リスト段1)。buildDeductions と同じ「TriangleList に
     * 混ぜない独立メソッド」形 — 三角形パイプライン (build/golden) を一切触らないため。
     *
     * 列 (trap-design.md contract): `Trapezoid, num, length, widthA, widthB, parent, side, align, parentKind`
     *   length=延長(辺B), widthA=底辺(辺A), widthB=上辺(辺C), parent=接続先番号(-1=独立),
     *   side=親の接続辺 (親=三角形:1=B/2=C, 親=台形:1=B左脚/2=C上辺/3=D右脚, 独立=0),
     *   align=上辺寄せ (0左/1中/2右, 省略時0で後方互換),
     *   parentKind=親の種別 (0=三角形(既定)/1=台形, 省略時0で後方互換 = R2 の 8 列 CSV は親=三角形のまま不変)。
     *
     * 独立 (parent<1): basepoint=原点・angle=INDEP_TRAP_ANGLE で構築 (本体を三角形と同じ下向き)。
     * 接続・親=三角形 (parentKind=0, parent>=1): nodeA=親三角形。Rectangle.calcPoint() の initByParent が
     *   parent.getLine(side) を呼ぶ (EditObject.kt:42)。Triangle.getLine の side 規約
     *   (TriangleUtilitiesExtensions.kt:145) は 1=B辺・2=C辺 で CSV と同値なので side を直に渡す。
     * 接続・親=台形 (parentKind=1): 親は台形番号 (台形群内の構築順 1 始まり)。構築順が「親は子より先」
     *   不変条件を満たす (親 < 現在の台形番号) ので、親は既に result に在る → result[parent-1] を nodeA に渡す。
     *   トポロジカルソート不要。initByParent が Rectangle 親で getLine(side) を side 分岐 (R3、EditObject.kt:37-41)。
     *   チェーン (台形→台形→…) は calcPoint が getLine 経由で再帰的に親を解決するので描画側の変更は不要。
     *   接続時 baseline は親辺で上書きされ widthA は幾何に効かない (= 底辺は親辺長になる)。
     *
     * scale は三角形と同じ実効倍率。trilist が setScale 済なら親辺は scale 済座標で返るので、
     * 台形の length/widthB も同 scale で構築して座標系を揃える (寸法値は描画側で実辺長/scale で復元)。
     */
    fun buildTrapezoids(doc: CsvDoc, trilist: TriangleList, scale: Float = 1f): List<Rectangle> {
        val result = mutableListOf<Rectangle>()
        val s = if (scale > 0f) scale.toDouble() else 1.0
        for (row in doc.trapRows) {
            val c = row.chunks
            val length = c.getOrNull(2)?.toFloatOrNull() ?: continue
            val widthA = c.getOrNull(3)?.toFloatOrNull() ?: continue
            val widthB = c.getOrNull(4)?.toFloatOrNull() ?: continue
            val parent = c.getOrNull(5)?.toIntOrNull() ?: -1
            val side = c.getOrNull(6)?.toIntOrNull() ?: 0
            // 8 列目 align (0左/1中/2右)。省略時 0 で後方互換 (R1 の 7 列 CSV は左寄せのまま)
            val align = c.getOrNull(7)?.toIntOrNull() ?: 0
            // 9 列目 parentKind (0=三角形 / 1=台形)。省略時 0 で後方互換 (R2 の 8 列 CSV は親=三角形のまま)
            val parentKind = c.getOrNull(8)?.toIntOrNull() ?: 0
            val l = length * s
            val wa = widthA * s
            val wb = widthB * s
            // 独立台形 (basepoint=原点・angle=INDEP_TRAP_ANGLE)。fallback でも使う
            fun indep() = Rectangle(l, wa, wb, angle = INDEP_TRAP_ANGLE, basepoint = PointXY(0f, 0f), alignment = align)
            when {
                parent < 1 -> result.add(indep())
                parentKind == 1 -> {
                    // 親は台形 (構築順 1 始まり)。親 < 現在の台形番号なので既に result に在る。
                    val pIdx = parent - 1
                    if (pIdx < 0 || pIdx >= result.size) result.add(indep())  // 前方参照/範囲外 → 独立 fallback
                    else result.add(Rectangle(l, wa, wb, nodeA = result[pIdx], side = side, alignment = align))
                }
                parent > trilist.size() -> result.add(indep())  // 三角形親が範囲外 → 独立 fallback (従来挙動)
                else -> result.add(Rectangle(l, wa, wb, nodeA = trilist.getBy(parent), side = side, alignment = align))
            }
        }
        return result
    }

    /**
     * "TriTrap" 行 → 台形を親に持つ三角形のリスト。buildTrapezoids でビルド済みの traps を親に取り、
     * Triangle(parent: EditObject, side, B, C) (= initByParent → getLine、Triangle.kt:258) で台形の辺に
     * 底辺(A)を乗せて構築する。これで「台形に三角形を接続」が成立する。三角形ビルド (build) が台形より
     * 先に走り親を参照できない循環を、台形ビルド後のこのパスに後回しして解く。
     * 三角形のみ / 既存混在 CSV は trapParentedTriRows が空 = 何も足さない (golden 不変)。
     *
     * 列: TriTrap, num, ea(情報・台形辺長で上書き), eb(=B), ec(=C), parent(台形群 index 1始まり),
     *     side(1=左脚B / 2=上辺C / 3=右脚D)。num は描画側で trilist 末尾に通し番号を振るので情報のみ。
     * scale は buildTrapezoids と同じ実効倍率 (台形が scale 済座標なので B/C も同 scale に揃える)。
     */
    fun buildTrapParentedTriangles(doc: CsvDoc, traps: List<Rectangle>, scale: Float = 1f): List<Triangle> {
        val result = mutableListOf<Triangle>()
        val s = if (scale > 0f) scale else 1f
        val ntrap = traps.size
        for (row in doc.trapParentedTriRows) {
            val c = row.chunks
            val b = c.getOrNull(3)?.toFloatOrNull() ?: continue
            val cc = c.getOrNull(4)?.toFloatOrNull() ?: continue
            val target = c.getOrNull(5)?.toIntOrNull() ?: continue
            val side = c.getOrNull(6)?.toIntOrNull() ?: continue
            // target_idx 解釈:
            //   1..ntrap          → 台形親 (Rectangle)
            //   ntrap+1.. (chain) → TriTrap 親 (= 既に build 済の result[ttIdx])
            // tritrap chain は親が先に在る不変条件 (CSV 出現順 = bake 順) なので、
            // result 末尾までで解決可。前方参照は無視 (continue)。
            val parent: EditObject? = when {
                target in 1..ntrap -> traps[target - 1]
                target > ntrap && (target - ntrap - 1) in result.indices -> result[target - ntrap - 1]
                else -> null
            }
            if (parent == null) continue
            result.add(Triangle(parent, side, b * s, cc * s))
        }
        return result
    }

    /**
     * 混在モデル (三角形 + 台形 + 台形子三角形) を 1 つにまとめた build 結果。
     * 三角形と台形を順不同に持つツリー構造の SoT。書き出し (DXF/SFC/XLSX) は
     * これを 1 引数で受け取れば 3 リスト同期 (numberTrapTris 等) を呼ばずに済む。
     */
    data class MixedBuild(
        val trilist: TriangleList,
        val traps: List<Rectangle>,
        val trapTris: List<Triangle>,
    )

    /**
     * build / buildTrapezoids / buildTrapParentedTriangles を 1 呼び出しに統合した混在 build。
     * 内部実装は既存 3 メソッドへの薄いコーディネータ (= 結果は完全同値)。新規呼び出し元は
     * これを使うことで、図形種別を意識せず混在モデルを 1 リクエストで取れる。
     *
     * trilist は呼び出し側で組んでから渡せる (overrides 適用後 / setChildsToAllParents 後 /
     * テスト用の作為 trilist 等)。省略時は build(doc) で内部生成。台形は trilist を親に取るので、
     * 「呼び出し側が手を入れた trilist」を反映させたい場合は必ず明示で渡すこと。
     *
     * user 2026-06-14「ツリー構造の中で三角形と台形を順不同に生成できるように基底に寄せたら
     * 解決できそうなもんだが」── EditObject / Triangle(parent: EditObject) の継ぎ目は既に
     * あって、各 build パスが種別ごとに分かれているだけ。それを 1 つにまとめる第一歩。
     */
    fun buildMixed(doc: CsvDoc, scale: Float = 1f, trilist: TriangleList = build(doc)): MixedBuild {
        val traps = buildTrapezoids(doc, trilist, scale)
        val trapTris = buildTrapParentedTriangles(doc, traps, scale)
        return MixedBuild(trilist, traps, trapTris)
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
        val rows = (1..trilist.size()).map { i ->
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
        // dedRows は素通し (控除は TriangleList の外。web の編集は行の置換/追加で dedRows 自体を更新する)。
        // textSize / trapRows / figureRows / trapParentedTriRows も素通し — 台形 (混在リスト) は
        // TriangleList の外側 (CsvDoc.trapRows / trapParentedTriRows) で持つので、bake が三角形のみ
        // 再構築して台形を捨てると保存 CSV から台形が消える (user 報告 2026-06-14「混在で保存→台形消失」)。
        return CsvDoc(
            original.preLines, rows, trilist.angle, original.postLines, original.dedRows, original.textSize,
            original.trapRows, original.figureRows, original.trapParentedTriRows,
        )
    }
}
