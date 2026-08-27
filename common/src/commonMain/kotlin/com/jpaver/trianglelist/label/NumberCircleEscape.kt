package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.Triangle

/**
 * 番号サークルの退避 (2026-08-27 user 指示)。
 *
 * 可読サイズ (JIS 3.5mm) に上げると寸法値が番号サークルに乗る。対処の順番は
 * 「まず番号サークルを動かす → それでも収まらなければ寸法値を動かす」── 番号は
 * 2 自由度で自由に動けるが、寸法値は辺に紐づいた離散スロットしか持たないので、
 * **自由な方を先に動かす**と拘束された方に余地が残る (AutoCAD の DIMATFIT が
 * 「矢印を先に、次に文字」と決めているのと同じ理屈)。
 *
 * 動かし方は「サイドの辺の方向にスライド」(user)。辺に沿って少しずらすだけで
 * 寸法値を置くスペースが空くことが多い。距離は小さい方から伸ばす ── 動かす量は
 * 少ないほどよい (元の位置が図形の重心付近で、番号の帰属が一番読みやすいため)。
 *
 * 図形の外に出す (= 旗揚げ) のは同じスライド方向をそのまま伸ばすだけで書ける
 * (user「簡単なのはそのままスライド方向に外に出してしまう事」) が、**既定では
 * 外に出さない** (allowFlagOut=false)。実データ 8.25 の干渉はスライドで足りる
 * (user「今回のケースではそれほど顕著に番号サークルが干渉してない。図形内で
 * スライドさせれば済む」) ── 外に出すと引出線が要り、番号の帰属が引出線頼みになる。
 *
 * 既存の PointNumberManager.autoAlign とは発火条件が別。autoAlign は面積・辺長の
 * 閾値 (小さく細い三角形) で動くが、ここは**実際に衝突している時だけ**動く ──
 * 可読サイズの文字は、閾値を通る普通の三角形でも番号にぶつかる。
 */
object NumberCircleEscape {

    /** 退避 1 件。to が図形の外なら旗揚げ (引出線は既存の描画が containsPoint で自動判定)。 */
    data class Move(
        val shapeNumber: Int,
        val from: PointXY,
        val to: PointXY,
        val isFlagOut: Boolean,
    )

    /** 番号サークルの半径。ModelOverlapAnalyzer が判定に使う値と同一でなければ意味がない。 */
    fun circleRadius(textSize: Float): Double = (textSize * 0.85f).toDouble()

    /** スライド距離の梯子 (円半径の倍数)。小さい方から試す = 動かす量は少ないほどよい。 */
    private val STEPS = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0)

    /**
     * 判定に持たせる余白の比率 (2026-08-27 user「⑪みたいなのも厳密に言うと番号を
     * すこしスライドさせてやると寸法が見やすくなる」)。
     *
     * 「重なっていない」= 見やすい、ではない。境界すれすれで避けた文字は詰まって読みにくい。
     * さらに判定と描画のメトリクスには実測差がある (common の近似は desktop の実測
     * MS Gothic より 4〜9% 細い ── 実データ 8.25 の box 実測比 1.04〜1.09)。
     * どちらも「判定を少し大きめの文字で行う」ことで同時に吸収できる。
     */
    const val DEFAULT_CLEARANCE: Float = 0.10f

    fun solve(
        list: EditList<out CycleShape>,
        textSize: Float,
        scale: Float = 1f,
        sokutenListVector: Int = 0,
        thresholdAngle: Float = 125f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
        allowFlagOut: Boolean = false,
        clearance: Float = DEFAULT_CLEARANCE,
    ): List<Move> {
        // 判定だけ少し大きい文字で行う (余白 + メトリクス差の吸収)。図面に描かれる
        // 文字の大きさは変わらない ── 変えるのは「どこまで近付いたら駄目とするか」だけ
        val judgeSize = textSize * (1f + clearance)
        val boxes = ModelOverlapAnalyzer.boxes(list, judgeSize, scale, sokutenListVector, thresholdAngle, metrics)
        val radius = circleRadius(judgeSize)
        val moves = mutableListOf<Move>()

        list.forEachItemIndexed { num, shape ->
            // user が自分で動かした番号は触らない (2026-08-27 user「ユーザーが旗揚げを
            // 動かした場合はそっちを優先するっていう、従来のポリシー」)。自動補正は
            // 繋がり方によっては逆に重なる向きへ出すことがあり、完全ではない ──
            // 人が直した結果を自動で上書きするのが一番damageが大きい
            if (shape is Triangle && shape.pointNumber.flag.isMovedByUser) return@forEachItemIndexed
            val anchor = shape.pointNumberAnchor()
            // 粗判定 (外接半径 + 円半径より遠ければ当たらない) で落としてから精密判定
            fun near(box: LabelBox, at: PointXY): Boolean {
                val limit = box.boundingRadiusMm + radius + LabelBox.EPS
                val dx = box.center.x - at.x
                val dy = box.center.y - at.y
                return dx * dx + dy * dy <= limit * limit
            }
            val hits = boxes.filter { (_, box) ->
                near(box, anchor) && box.penetrationDepthCircle(anchor, radius) != null
            }
            if (hits.isEmpty()) return@forEachItemIndexed

            // 逃げる向きの基準: ぶつかっている寸法値の平均位置から見て反対側
            val awayX = hits.sumOf { anchor.x - it.second.center.x } / hits.size
            val awayY = hits.sumOf { anchor.y - it.second.center.y } / hits.size
            val away = PointXY(awayX, awayY).normalize()

            // 候補方向: 各辺に沿った向き (両方向) と、各辺の中点へ向かう向き。
            // 「逃げたい向き」との一致度が高い順に試す ── 同点は辺の並び順で決まるので決定的
            val directions = buildList {
                for (edge in shape.edges()) {
                    val along = (edge.right - edge.left).normalize()
                    add(along)
                    add(PointXY(-along.x, -along.y))
                    val mid = edge.left.calcMidPoint(edge.right)
                    add((mid - anchor).normalize())
                }
            }.filter { it.x != 0.0 || it.y != 0.0 }
                .sortedByDescending { it.x * away.x + it.y * away.y }

            // 距離を外側の loop にする = 全方向を試してから次の距離へ。
            // 「動かす量が最小の解」を選ぶため (方向を先に固定すると遠くまで飛びやすい)
            var found: Move? = null
            outer@ for (step in STEPS) {
                val distance = radius * step
                for (dir in directions) {
                    val candidate = PointXY(anchor.x + dir.x * distance, anchor.y + dir.y * distance)
                    val inside = shape.containsPoint(candidate)
                    if (!inside && !allowFlagOut) continue
                    val clear = boxes.none { (_, box) ->
                        near(box, candidate) && box.penetrationDepthCircle(candidate, radius) != null
                    }
                    if (!clear) continue
                    found = Move(num, anchor, candidate, isFlagOut = !inside)
                    break@outer
                }
            }
            if (found != null) moves.add(found)
        }
        return moves
    }

    /**
     * 退避結果をモデルに適用する。番号位置を持てるのは Triangle (pointnumber) だけ ──
     * Rectangle 等は pointNumberAnchor() が重心固定なので対象外 (そもそも重心は
     * 図形の中で、寸法値とぶつかりにくい)。
     *
     * isAutoAligned を立てるのは、後段の autoAlign に上書きされないため
     * (autoAlign は先頭で isMovedByUser || isAutoAligned なら現在値を返す)。
     * isMovedByUser は立てない ── 動かしたのは自動処理であって user ではない。
     */
    fun apply(list: EditList<out CycleShape>, moves: List<Move>) {
        val byNumber = moves.associateBy { it.shapeNumber }
        list.forEachItemIndexed { num, shape ->
            val move = byNumber[num] ?: return@forEachItemIndexed
            if (shape is Triangle) {
                shape.pointnumber = move.to
                // isEscaped: 既定位置へ戻す再計算 (calcPoints 等) から外すための印。
                // isMovedByUser は立てない ── 動かしたのは自動処理であって user ではない
                shape.pointNumber.flag.isEscaped = true
            }
        }
    }
}
