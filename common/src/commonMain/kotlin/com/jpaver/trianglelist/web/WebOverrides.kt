package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.setDimPath
import com.jpaver.trianglelist.setDimPoint
import com.jpaver.trianglelist.setPointNumber

/**
 * Web 段階2e (task #15): 手動 override の適用層。
 *
 * ADR 0003 Decision B の「position = デフォルト式 ⊕ 手動 override」の override 側。
 * CSV は dim 配置を round-trip しない (WebCsvReader に dim の出現ゼロ) ため、
 * W/H フリップ・番号サークル移動は TS 側が JSON で保持し、parse 後の TriangleList に
 * ここで適用してから render/export する。
 *
 * 記録は**結果値** (候補選択の到達値) で持つ — controlHorizontal は cycleIncrement
 * (Dims.kt:130)、controlVertical は flipVertical (Dims.kt:155) で状態依存のため、
 * tap 回数の再生は非冪等。値で持てば適用が冪等になる (ADR 0003 B(a)
 * 「候補選択 = 式のパラメータ差し替えとして記録」)。
 *
 * JSON 形式 (TS 側 main.ts の overrides state がそのまま JSON.stringify したもの):
 *   {"dims":   [{"tri":1,"side":0,"h":3,"v":1}, ...],
 *    "numbers": [{"tri":1,"x":1.25,"y":0.8}, ...]}
 * - tri = 1-based 三角形番号、side = 0/1/2 (A/B/C)、4 = 測点 (器のみ、UI は段階外)
 * - h = horizontal 到達値 (0..4、>2 で旗揚げ)、v = vertical 到達値 (1=外/3=内)。
 *   h/v は optional — 触った軸だけ記録すると flag の立ち方が controlDim* と一致する
 * - numbers = pointnumber のモデル座標
 *
 * parser は kotlinx.serialization 非依存の専用品 (commonMain に JSON ライブラリが無い)。
 * 値はフラットな数値のみ・文字列やネストは来ない、という上記形式の前提に依存する。
 */
object WebOverrides {

    data class DimOverride(val tri: Int, val side: Int, val h: Int?, val v: Int?)
    // 座標は Double — PointXY の内部型 (ADR 0004 内部座標の double 化) と揃え、
    // JSON 文字列との round-trip を桁落ちなしにする
    data class NumberOverride(val tri: Int, val x: Double, val y: Double)
    data class Overrides(
        val dims: List<DimOverride> = emptyList(),
        val numbers: List<NumberOverride> = emptyList(),
    )

    private const val SIDE_A = 0
    private const val SIDE_B = 1
    private const val SIDE_C = 2
    private const val SIDE_SOKUTEN = 4

    fun applyJson(trilist: TriangleList, overridesJson: String) =
        apply(trilist, parse(overridesJson))

    fun parse(json: String): Overrides {
        if (json.isBlank()) return Overrides()
        val dims = objectsIn(arrayAfterKey(json, "dims")).mapNotNull { obj ->
            val tri = intField(obj, "tri") ?: return@mapNotNull null
            val side = intField(obj, "side") ?: return@mapNotNull null
            val h = intField(obj, "h")
            val v = intField(obj, "v")
            if (h == null && v == null) null else DimOverride(tri, side, h, v)
        }
        val numbers = objectsIn(arrayAfterKey(json, "numbers")).mapNotNull { obj ->
            val tri = intField(obj, "tri") ?: return@mapNotNull null
            val x = numField(obj, "x") ?: return@mapNotNull null
            val y = numField(obj, "y") ?: return@mapNotNull null
            NumberOverride(tri, x, y)
        }
        return Overrides(dims, numbers)
    }

    /**
     * TriangleList へ値を適用する。controlDimHorizontal/Vertical
     * (TriangleDimExtensions.kt:32-42) が mutation 後にやるのと同じ手順
     * (値設定 + flag + setDimPath/setDimPoint) を、cycle/flip の代わりに
     * 到達値の直接代入で行う。flag の立て方も controlHorizontal (side A は立てない) /
     * controlVertical (A/B/C とも立てる) と同一 — setAlignByChild / autoAlign からの
     * 保護条件を app と揃えるため。
     *
     * 範囲外の tri / 不正値は黙って skip (古い autosave が行削除後に残るケース)。
     */
    fun apply(trilist: TriangleList, overrides: Overrides) {
        for (d in overrides.dims) {
            if (d.tri < 1 || d.tri > trilist.size()) continue
            val tri = trilist.getBy(d.tri)
            d.h?.takeIf { it in 0..4 }?.let { h ->
                when (d.side) {
                    SIDE_A -> tri.dim.horizontal.a = h
                    SIDE_B -> { tri.dim.horizontal.b = h; tri.dim.flag[1].isMovedByUser = true }
                    SIDE_C -> { tri.dim.horizontal.c = h; tri.dim.flag[2].isMovedByUser = true }
                    SIDE_SOKUTEN -> { tri.dim.horizontal.s = h; tri.dim.flagS.isMovedByUser = true }
                }
            }
            d.v?.takeIf { it == 1 || it == 3 }?.let { v ->
                when (d.side) {
                    SIDE_A -> { tri.dim.vertical.a = v; tri.dim.flag[0].isMovedByUser = true }
                    SIDE_B -> { tri.dim.vertical.b = v; tri.dim.flag[1].isMovedByUser = true }
                    SIDE_C -> { tri.dim.vertical.c = v; tri.dim.flag[2].isMovedByUser = true }
                }
            }
            tri.setDimPath()
            tri.setDimPoint()
        }
        for (n in overrides.numbers) {
            if (n.tri < 1 || n.tri > trilist.size()) continue
            // setPointByUser (PointNumberManager.kt:18): isMovedByUser が立ち、
            // arrangePointNumbers の autoAlign (同:36 early return) から保護される。
            // pointcenter から遠すぎる点は app と同じ BORDER 判定で無視される
            trilist.getBy(n.tri).setPointNumber(PointXY(n.x, n.y), true)
        }
    }

    // ---- mini JSON parser (フラット数値のみの上記形式専用) ----

    /** "key": [ ... ] の配列の中身を返す (無ければ空文字) */
    private fun arrayAfterKey(json: String, key: String): String {
        val k = json.indexOf("\"$key\"")
        if (k < 0) return ""
        val start = json.indexOf('[', k)
        if (start < 0) return ""
        var depth = 0
        for (i in start until json.length) {
            when (json[i]) {
                '[' -> depth++
                ']' -> { depth--; if (depth == 0) return json.substring(start + 1, i) }
            }
        }
        return ""
    }

    /** 配列の中身から { ... } の各オブジェクト本体を切り出す */
    private fun objectsIn(arrayBody: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in arrayBody.indices) {
            when (arrayBody[i]) {
                '{' -> { if (depth == 0) start = i; depth++ }
                '}' -> { depth--; if (depth == 0 && start >= 0) { out.add(arrayBody.substring(start + 1, i)); start = -1 } }
            }
        }
        return out
    }

    private fun numField(obj: String, key: String): Double? =
        Regex("\"$key\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)")
            .find(obj)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun intField(obj: String, key: String): Int? = numField(obj, key)?.toInt()
}
