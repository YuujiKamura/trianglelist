package com.jpaver.trianglelist.agent

import com.jpaver.trianglelist.dxf.DxfCircle
import com.jpaver.trianglelist.dxf.DxfLine
import com.jpaver.trianglelist.dxf.DxfLwPolyline
import com.jpaver.trianglelist.dxf.DxfText
import kotlin.math.abs
import kotlin.math.hypot

/**
 * エージェントが置いた図形を、画面上で掴んで動かす / 文字を書き換えるための操作。
 *
 * 編集対象は **エージェントレイヤの図形だけ**。ファイル由来 (baseParseResult) は触らない。
 * 「誰が置いたものか」が曖昧にならないので、後から差し戻しや検査がしやすい。
 */
object AgentEdit {

    /** 平面上の点と線分の距離。 */
    private fun distToSeg(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1; val dy = y2 - y1
        val len2 = dx * dx + dy * dy
        if (len2 < 1e-12) return hypot(px - x1, py - y1)
        var t = ((px - x1) * dx + (py - y1) * dy) / len2
        if (t < 0.0) t = 0.0; if (t > 1.0) t = 1.0
        return hypot(px - (x1 + dx * t), py - (y1 + dy * t))
    }

    /** テキストの概算幅。1文字あたり height*0.62 とする (日本語混在の当たり判定用)。 */
    private fun textWidth(t: DxfText): Double = t.text.length * t.height * 0.62

    /** モデル座標 (mx,my) から tol 以内にある図形の flat index。無ければ null。 */
    fun hitTest(mx: Double, my: Double, tol: Double): Int? {
        var best: Int? = null
        var bestD = tol
        AgentLayers.flat().forEachIndexed { i, (_, e) ->
            val d: Double = when (e) {
                is DxfLine -> distToSeg(mx, my, e.x1, e.y1, e.x2, e.y2)
                is DxfCircle -> abs(hypot(mx - e.centerX, my - e.centerY) - e.radius)
                is DxfLwPolyline -> {
                    var m = Double.MAX_VALUE
                    val v = e.vertices
                    for (k in 0 until v.size - 1)
                        m = minOf(m, distToSeg(mx, my, v[k].first, v[k].second, v[k + 1].first, v[k + 1].second))
                    if (e.isClosed && v.size > 1)
                        m = minOf(m, distToSeg(mx, my, v.last().first, v.last().second, v[0].first, v[0].second))
                    m
                }
                is DxfText -> {
                    val w = textWidth(e)
                    val cx = e.x + w / 2.0; val cy = e.y + e.height / 2.0
                    val ddx = maxOf(0.0, abs(mx - cx) - w / 2.0)
                    val ddy = maxOf(0.0, abs(my - cy) - e.height / 2.0)
                    hypot(ddx, ddy)
                }
                else -> Double.MAX_VALUE
            }
            if (d <= bestD) { bestD = d; best = i }
        }
        return best
    }

    /** flat index の図形を (dx,dy) 平行移動する。 */
    fun move(index: Int, dx: Double, dy: Double) {
        AgentLayers.replaceAt(index) { e ->
            when (e) {
                is DxfLine -> e.copy(x1 = e.x1 + dx, y1 = e.y1 + dy, x2 = e.x2 + dx, y2 = e.y2 + dy)
                is DxfCircle -> e.copy(centerX = e.centerX + dx, centerY = e.centerY + dy)
                is DxfLwPolyline -> e.copy(vertices = e.vertices.map { it.first + dx to it.second + dy })
                is DxfText -> e.copy(x = e.x + dx, y = e.y + dy)
                else -> e
            }
        }
    }

    /** flat index が TEXT ならその文字列を返す。 */
    fun textOf(index: Int): String? = (AgentLayers.flat().getOrNull(index)?.second as? DxfText)?.text

    /** flat index が TEXT ならその文字列を差し替える。 */
    fun setText(index: Int, s: String) {
        AgentLayers.replaceAt(index) { e -> if (e is DxfText) e.copy(text = s) else e }
    }

    /** 選択中の図形の代表点 (ハイライト表示用)。 */
    fun anchorOf(index: Int): Pair<Double, Double>? =
        when (val e = AgentLayers.flat().getOrNull(index)?.second) {
            is DxfLine -> (e.x1 + e.x2) / 2.0 to (e.y1 + e.y2) / 2.0
            is DxfCircle -> e.centerX to e.centerY
            is DxfLwPolyline -> e.vertices.map { it.first }.average() to e.vertices.map { it.second }.average()
            is DxfText -> e.x + textWidth(e) / 2.0 to e.y + e.height / 2.0
            else -> null
        }

    /** 選択中の図形の説明 (UI 表示用)。 */
    fun describe(index: Int): String {
        val p = AgentLayers.flat().getOrNull(index) ?: return "-"
        val kind = when (p.second) {
            is DxfLine -> "LINE"; is DxfCircle -> "CIRCLE"
            is DxfLwPolyline -> "POLYLINE"; is DxfText -> "TEXT"; else -> "?"
        }
        return "[" + index + "] " + kind + " layer=" + p.first
    }

    fun deleteAt(index: Int) = AgentLayers.deleteAt(index)
}
