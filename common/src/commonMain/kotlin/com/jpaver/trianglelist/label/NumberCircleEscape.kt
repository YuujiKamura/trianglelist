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

    /**
     * **サークルが収まる領域の中で、一番鋭い頂点から最も遠い点** (2026-08-28 user
     * 「番号をもっと外周側に目一杯スライドさせれば 3m を旗揚げしなくても中に置く
     * スペースがあったりする」「ビューワに出てるサンプルだと寄せてるように見えない」)。
     *
     * 内心 (= その三角形に描ける最大の円の中心) は「一番余裕のある点」だが、
     * **サークルが行ける限界ではない**。3,3,1 の三角形なら内接円半径 0.42m に対し
     * 番号サークルは 1:50 の JIS 3.5mm で半径 0.149m ── 差の分だけ短辺側へまだ寄れる
     * (頂点から 85.7% → 94.9%)。寄り切って初めて細長い三角形の内側に寸法値を置く余地が
     * 空き、旗揚げ (辺の延長線上へ飛ぶので細長い figure では引出線が図形の数倍になる) を
     * 避けられる。
     *
     * 「鋭い頂点から遠ざかる」を基準にする理由: 細長い三角形の余地は必ず鋭角の反対側にある。
     * 正三角形ではどの頂点も同じ鋭さなので退化し、内心 (= 重心) のまま動かない ──
     * 普通の三角形を動かさずに済むのは、寄せ量を magic number ではなく
     * 「サークルが収まるか」だけで決めているため。
     *
     * サークルが内接円より大きい (どこにも収まらない) 三角形は内心を返す。図形外へ出すかは
     * 退避側の判断で、ここは「図形内での最善」だけを返す。
     */
    fun placeAwayFromSharpestVertex(shape: CycleShape, textSize: Float): PointXY {
        val incenter = incenterOf(shape) ?: return shape.pointNumberAnchor()
        val triangle = shape as? Triangle ?: return incenter
        val radius = circleRadius(textSize)

        // 一番鋭い頂点 = 対辺が最も短い頂点 (辺長だけで決まるので角度計算に依存しない)
        val vertices = listOf(triangle.pointAB, triangle.pointBC, triangle.pointCA)
        val opposite = listOf(triangle.lengthC, triangle.lengthA, triangle.lengthB)
        val sharpest = vertices[opposite.indexOf(opposite.min())]

        // 正三角形 (対辺が全部同じ) は「一番鋭い頂点」が決まらないので内心のまま
        if (opposite.max() - opposite.min() <= 1e-6f) return incenter

        // 内心から「鋭い頂点の反対向き」へ、サークルが収まる限り進む。
        // 内心は最大クリアランス点なので、そこから離れるほど余裕は単調に減る ──
        // 二分探索で「収まる最遠点」が一意に決まる (刻み幅の magic number が要らない)
        val dx = incenter.x - sharpest.x
        val dy = incenter.y - sharpest.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len <= 0.0 || !len.isFinite()) return incenter
        val ux = dx / len
        val uy = dy / len

        // **限界まで押し込まない** (2026-08-28 user「極端にやると今みたいな形だな。でもまだ
        // スペースに余裕があるだろこの面積だと。そっちも観るって事なんだよ」)。クリアランスが
        // サークル半径ちょうどになる所まで寄せると、サークルが辺に接して角へ押し込まれた絵に
        // なる ── 面積に余裕がある三角形でそれをやる理由がない。
        //
        // 止める基準はクリアランス = (内接円半径 R + サークル半径 r) / 2。内心にいる時の
        // クリアランスが R (最大)、限界が r なので、その中点 = 「まだ寄れる余地」と
        // 「辺からの余白」を半分ずつ分ける配分。R が大きい (面積に余裕がある) ほど止まる位置は
        // 内心寄りになる = 面積を観ていることになる。
        val inradius = clearanceOf(triangle, incenter)
        val target = (inradius + radius) / 2.0

        // 図形の外に出ると「辺までの距離」はまた増えていく (頂点を通り過ぎた先は遠い) ので、
        // 内外判定を必ず併せる。これが無いと二分探索が図形外の遠い点を掴む
        fun fits(t: Double): Boolean {
            val p = PointXY(incenter.x + ux * t, incenter.y + uy * t)
            return triangle.containsPoint(p) && clearanceOf(triangle, p) >= target
        }
        if (clearanceOf(triangle, incenter) < radius) return incenter // どこにも入らない

        // 上限は三角形の外接的な大きさ (必ず収まらなくなる距離) から始める
        var lo = 0.0
        var hi = (triangle.lengthA + triangle.lengthB + triangle.lengthC).toDouble()
        repeat(40) {
            val mid = (lo + hi) / 2.0
            if (fits(mid)) lo = mid else hi = mid
        }
        return PointXY(incenter.x + ux * lo, incenter.y + uy * lo)
    }

    /**
     * リスト全体の番号を「収まる範囲で鋭角の反対側へ寄り切った位置」に置く。
     * 衝突の有無に関係なく走る ── 目的は番号の衝突を直すことではなく、
     * **細長い三角形の内側に寸法値を置く余地を空ける**こと (寄せてから退避を掛ける)。
     *
     * user が動かした番号は触らない。isEscaped を立てるのは calcPoints の
     * 「既定位置へ戻す」対象から外すため (isMovedByUser は立てない ── 動かしたのは自動処理)。
     */
    fun slideToOuter(list: EditList<out CycleShape>, textSize: Float) {
        if (textSize <= 0f) return
        list.forEachItem { shape ->
            if (shape !is Triangle) return@forEachItem
            if (shape.pointNumber.flag.isMovedByUser) return@forEachItem
            val to = placeAwayFromSharpestVertex(shape, textSize)
            if (!to.x.isFinite() || !to.y.isFinite()) return@forEachItem
            shape.pointnumber = to
            shape.pointNumber.flag.isEscaped = true
        }
    }

    private fun incenterOf(shape: CycleShape): PointXY? =
        (shape as? Triangle)?.let { com.jpaver.trianglelist.editmodel.PointNumberManager().incenter(it) }

    /** 点から 3 辺までの最短距離 (= そこに置ける円の半径)。 */
    private fun clearanceOf(triangle: Triangle, p: PointXY): Double {
        val v = listOf(triangle.pointAB, triangle.pointBC, triangle.pointCA)
        var min = Double.MAX_VALUE
        for (i in 0..2) {
            val a = v[i]
            val b = v[(i + 1) % 3]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0.0, 1.0)
            val cx = a.x + t * dx
            val cy = a.y + t * dy
            val d = kotlin.math.sqrt((p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy))
            if (d < min) min = d
        }
        return min
    }

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
