package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.Triangle

/**
 * 寸法値の自動退避 (2026-08-27 user 指示)。番号サークルを逃がしても残る
 * 「文字どうしの重なり」を、寸法値そのものの配置を変えて解く。
 *
 * **候補は発明しない**。既にある horizontal コード (Dims.horizontal / DimensionLayout) を
 * そのまま使う:
 *   0 = 中央 (既定)
 *   1,2 = 辺に沿って寄せる (habayose = 辺長の 10%) ── user の言う「少しスライド」
 *   3,4 = 旗揚げ左右 (辺の延長線上に出して引出線を引く)
 * CSV に永続化済みの語彙なので、退避結果は既存の描画・書き出し・手動サイクル (W キー) と
 * そのまま地続きになる。
 *
 * 順番は「まずスライド → だめなら旗揚げ」。旗揚げは引出線が増えて図が賑やかになるので、
 * 済むならスライドで済ませる (user「１４と１５の辺に関しては少しスライドさせるだけで
 * 解決できる」)。
 *
 * 既存の Dims.autoDimHorizontalByAngle は同じ狙いの書きかけで、enableAutoHorizontal=false で
 * 封印されている。判定が「面積 5 以下 かつ 内角 20 度以下」── どちらも「文字が入らない」の
 * **代理指標**でしかない。当たり判定ができた今は代理をやめ、実際に衝突しているかで発火する
 * (user「今のように当たり判定が付与されていればより分かりやすい」)。ただし
 * 「鋭角側は場所が無い」という幾何の勘所は候補の優先順として残す。
 *
 * 探索は決定的 (乱択なし): 対象を id 昇順、候補を梯子順に見て、**全体の衝突数が
 * 真に減る**変更だけ採用する (discrete gradient descent)。単調減少なので必ず止まる。
 */
object DimensionTextEscape {

    /** 寸法値 1 本の配置変更。side は 0=A / 1=B / 2=C / 4=測点名。 */
    data class Move(val shapeNumber: Int, val side: Int, val from: Int, val to: Int)

    private const val CENTER = 0
    private const val IN_RIGHT = 1
    private const val IN_LEFT = 2
    private const val OUTER_RIGHT = 3
    private const val OUTER_LEFT = 4
    private const val SIDE_SOKUTEN = 4

    /**
     * 試す順。スライド (1,2) が先、旗揚げ (3,4) が後。preferRight は左右どちらを先に
     * 試すかだけの好み ── 衝突が減るかは実測で確かめるので、順番を外しても解は出る
     * (探索が遅くなるだけ)。
     */
    fun candidateLadder(current: Int, preferRight: Boolean, allowFlagOut: Boolean = true): List<Int> {
        val slide = if (preferRight) listOf(IN_RIGHT, IN_LEFT) else listOf(IN_LEFT, IN_RIGHT)
        val flag = if (preferRight) listOf(OUTER_RIGHT, OUTER_LEFT) else listOf(OUTER_LEFT, OUTER_RIGHT)
        return (slide + if (allowFlagOut) flag else emptyList()).filter { it != current }
    }

    /** 測点名は左右の 2 択しか無い (Dims.controlHorizontal の cycleIncrement(.., 1) と対)。 */
    private fun sokutenLadder(current: Int): List<Int> = listOf(0, 1).filter { it != current }

    /**
     * user が自分で配置を動かした辺か (W キーの手動サイクル = Dims.controlHorizontal が
     * isMovedByUser を立てる)。動かしていたらそちらを優先し、自動退避は触らない
     * (2026-08-27 user「ユーザーが旗揚げを動かした場合はそっちを優先するっていう、
     * 従来のポリシー」)。自動補正は図形の繋がり方によっては逆に重なる向きへ出すことが
     * あり完全ではない ── 人が直した結果を上書きするのが一番damageが大きい。
     * 既存の Dims.autoDimHorizontalByAngle も同じ条件で降りている。
     */
    private fun isMovedByUser(shape: CycleShape, side: Int): Boolean {
        if (shape !is Triangle) return false
        return when (side) {
            0, 1, 2 -> shape.dim.flag.getOrNull(side)?.isMovedByUser == true
            SIDE_SOKUTEN -> shape.dim.flagS.isMovedByUser
            else -> false
        }
    }

    /**
     * その図形で**一番短い辺**か (2026-08-28 user「三辺の中のもっとも狭い辺って、単純に
     * 横スライドしても見づらくなるケースが結構ある。５，６とか、８とか、むしろ動かさない
     * ほうが良い」)。
     *
     * 短い辺の寸法値は動かす余地そのものが無い。スライド (habayose = 辺長の 10%) は
     * 辺が短いほど効かず、旗揚げは辺の延長線上へ出るので短辺では文字が辺から離れすぎて
     * どの辺の寸法か読めなくなる ── 衝突は減っても図面としては悪化する。
     * 実データ 8.25 では #5 が 3 辺とも、#6 / #8 も動かされていた。
     *
     * 「衝突数が減れば良い」という探索の目的関数だけでは、この悪化を検出できない
     * (機械の数字は減っている)。動かしてはいけない対象を先に外す。
     */
    private fun isShortestSide(shape: CycleShape, side: Int): Boolean {
        if (shape !is Triangle) return false
        if (side !in 0..2) return false
        val lengths = listOf(shape.lengthA, shape.lengthB, shape.lengthC)
        val shortest = lengths.min()
        return lengths[side] <= shortest + 1e-6f
    }

    private fun horizontalOf(shape: CycleShape, side: Int): Int? = when (side) {
        0 -> shape.dimHorizontal.a
        1 -> shape.dimHorizontal.b
        2 -> shape.dimHorizontal.c
        SIDE_SOKUTEN -> shape.dimHorizontal.s
        else -> null // Rectangle の D 辺等はまだ対象外
    }

    private fun setHorizontal(shape: CycleShape, side: Int, value: Int) {
        when (side) {
            0 -> shape.dimHorizontal.a = value
            1 -> shape.dimHorizontal.b = value
            2 -> shape.dimHorizontal.c = value
            SIDE_SOKUTEN -> shape.dimHorizontal.s = value
        }
    }

    /** その辺に繋がっている子図形 (無ければ null)。side は 0=A / 1=B / 2=C。 */
    private fun childOf(shape: CycleShape, side: Int): CycleShape? = when (side) {
        0 -> shape.node.a
        1 -> shape.node.b
        2 -> shape.node.c
        else -> null
    }

    /**
     * 旗揚げ左右の優先順。**子がいる辺は回転順で右の端へ出す** (2026-08-27 user
     * 「４だとC辺に子がいて、今だと回転順から言うと左に出してるよな？これを右に変えれば
     * それでいい。6に関しても同じ」)。
     *
     * 実装コードの名前と実際に出る側が逆になっている点に注意: DimensionLayout の
     * pointsOuter は OUTERRIGHT(3) の時に leftP/rightP を入れ替えて呼ぶため、
     * **user の言う「右」= OUTER_LEFT(4)** になる。実測 (実データ 8.25 #4 C 辺):
     *   h=3 → 辺の中点から見て (-0.185, +0.983) = 左上へ
     *   h=4 → (+0.074, -0.997) = 右下へ
     * 名前で判断すると必ず取り違えるので、この対応を根拠として明記しておく。
     *
     * 子がいない辺は従来どおり 3 を先に試す (どちらでも成立するので既定を変えない)。
     */
    private fun outerOrder(shape: CycleShape, side: Int): List<Int> =
        if (childOf(shape, side) != null) listOf(OUTER_LEFT, OUTER_RIGHT)
        else listOf(OUTER_RIGHT, OUTER_LEFT)

    /** "dim:<図形番号>:<辺>" を分解する。ModelOverlapAnalyzer.boxes が作る id 形式と対。 */
    private fun parseId(id: String): Pair<Int, Int>? {
        val parts = id.split(":")
        if (parts.size != 3 || parts[0] != "dim") return null
        val num = parts[1].toIntOrNull() ?: return null
        val side = parts[2].toIntOrNull() ?: return null
        return num to side
    }

    fun solve(
        list: EditList<out CycleShape>,
        textSize: Float,
        scale: Float = 1f,
        sokutenListVector: Int = 0,
        thresholdAngle: Float = 125f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
        maxPasses: Int = 3,
        allowFlagOut: Boolean = true,
        clearance: Float = NumberCircleEscape.DEFAULT_CLEARANCE,
    ): List<Move> {
        // 判定だけ少し大きい文字で行う (余白 + 近似メトリクスと実測の差の吸収)。
        // 詳細は NumberCircleEscape.DEFAULT_CLEARANCE
        val judgeSize = textSize * (1f + clearance)

        val shapes = mutableMapOf<Int, CycleShape>()
        list.forEachItemIndexed { num, shape -> shapes[num] = shape }

        // ---- 衝突状態を差分で持つ ----
        // 候補を 1 つ試すたびに全 57 本を判定し直すと、実データの探索が 196ms かかる
        // (2026-08-27 実測)。編集のたびに走らせるにはこれでは遅い。
        // 1 本動かして変わるのは「その 1 本の当たり」と「相手側の当たり」だけなので、
        // 動かした box だけ再クエリして差分を反映する。
        //
        // 図形の辺 (EDGE) のうち、**その寸法値が乗っている辺 (自分の基線) 以外**は判定に入れる
        // (2026-08-28 user「6番の3.9とか、15番の5.32とか、自分の基線以外と接触してるケースが
        // あって、こういう場合も本来出したほうが良い」)。
        //
        // 従来は EDGE を丸ごと外していた。理由は「判定 box は縦アライメントのパディング分
        // グリフより大きく、辺沿いの寸法値はほぼ必ず**自分の辺**に当たる」(OverlapReport) で、
        // これは自分の基線にしか当てはまらない。他の辺に食い込んでいるのは本物の可読性問題で、
        // 実データ 8.25 でも 5.32 が 249.7mm、3.9 が 180.6mm 食い込んでいた。
        //
        // 共有辺 (親の B 辺 = 子の A 辺) は同じ幾何が 2 つの id で登録されるので、
        // 端点が一致する辺も「自分の基線」として一緒に除外する ── 片方だけ除外すると
        // 双子の側で必ず当たり、全寸法値が対象になってしまう。
        val obstacles = CollisionField()
        val ownEdgeIds = mutableMapOf<String, MutableSet<String>>()
        val edgeSegments = mutableListOf<Triple<String, PointXY, PointXY>>()
        list.forEachItemIndexed { num, obj ->
            obstacles.addCircle("circle:$num", obj.pointNumberAnchor(), (judgeSize * 0.85f).toDouble())
            obj.edges().forEachIndexed { side, line ->
                val id = "edge:$num:$side"
                obstacles.addEdge(id, line.left, line.right)
                edgeSegments.add(Triple(id, line.left, line.right))
            }
        }
        fun sameSegment(a1: PointXY, a2: PointXY, b1: PointXY, b2: PointXY): Boolean {
            val e = LabelBox.EPS
            fun near(p: PointXY, q: PointXY) = kotlin.math.abs(p.x - q.x) <= e && kotlin.math.abs(p.y - q.y) <= e
            return (near(a1, b1) && near(a2, b2)) || (near(a1, b2) && near(a2, b1))
        }
        for ((id, a, b) in edgeSegments) {
            val set = ownEdgeIds.getOrPut(id) { mutableSetOf(id) }
            for ((other, c, d) in edgeSegments) {
                if (other != id && sameSegment(a, b, c, d)) set.add(other)
            }
        }
        val boxById = linkedMapOf<String, LabelBox>()
        for ((id, box) in ModelOverlapAnalyzer.boxes(list, judgeSize, scale, sokutenListVector, thresholdAngle, metrics)) {
            boxById[id] = box
        }
        val partners = boxById.keys.associateWith { mutableSetOf<String>() }
        val circleHit = boxById.keys.associateWithTo(mutableMapOf()) { false }

        fun hitsOf(id: String, box: LabelBox): Pair<Set<String>, Boolean> {
            // 粗判定で遠い相手を落としてから SAT (CollisionField.query と同じ足切り)。
            // これが無いと 300 本 × 300 本の総当たりになる
            val reach = box.boundingRadiusMm
            val labelHits = boxById.entries
                .filter { other ->
                    if (other.key == id) return@filter false
                    val limit = reach + other.value.boundingRadiusMm + LabelBox.EPS
                    val dx = box.center.x - other.value.center.x
                    val dy = box.center.y - other.value.center.y
                    if (dx * dx + dy * dy > limit * limit) return@filter false
                    box.penetrationDepth(other.value) != null
                }
                .map { it.key }.toSet()
            // 自分の基線 (と共有辺の双子) だけ外して、円と他の辺を見る
            val own = parseId(id)?.let { (num, side) -> ownEdgeIds["edge:$num:$side"] } ?: emptySet<String>()
            val blocked = obstacles.query(box, excludeId = id).any { hit ->
                hit.kind == ObstacleKind.CIRCLE || (hit.kind == ObstacleKind.EDGE && hit.id !in own)
            }
            return labelHits to blocked
        }
        for ((id, box) in boxById) {
            val (labelHits, circle) = hitsOf(id, box)
            partners.getValue(id).addAll(labelHits)
            labelHits.forEach { partners.getValue(it).add(id) }
            circleHit[id] = circle
        }
        fun collidingCount(): Int =
            boxById.keys.count { partners.getValue(it).isNotEmpty() || circleHit.getValue(it) }

        /** 図形 num の box を作り直し、当たり状態を差分更新する。 */
        fun refreshShape(num: Int) {
            val shape = shapes[num] ?: return
            val fresh = ModelOverlapAnalyzer.boxesOf(shape, num, judgeSize, scale, sokutenListVector, metrics)
            for ((id, box) in fresh) {
                if (!boxById.containsKey(id)) continue
                // 旧状態を相手側から外す
                partners.getValue(id).forEach { partners.getValue(it).remove(id) }
                partners.getValue(id).clear()
                boxById[id] = box
                val (labelHits, circle) = hitsOf(id, box)
                partners.getValue(id).addAll(labelHits)
                labelHits.forEach { partners.getValue(it).add(id) }
                circleHit[id] = circle
            }
        }

        val originals = mutableMapOf<Pair<Int, Int>, Int>()
        val applied = mutableMapOf<Pair<Int, Int>, Int>()

        repeat(maxPasses) {
            var improved = false
            var baseline = collidingCount()
            if (baseline == 0) return@repeat

            // 対象は「文字どうし」で衝突している寸法値のみ。番号サークルとの衝突は
            // NumberCircleEscape の担当 (自由に動ける方を先に動かす)
            // 対象は「文字どうし」に加えて「番号サークル / 自分以外の辺」に当たっている寸法値。
            // 番号側が動けなかった時の二段目がここ ── 円だけを NumberCircleEscape に任せて
            // いると、番号が図形内に逃げ場を持たない図形で誰も動かさないまま残る
            val targets = boxById.keys
                .filter { partners.getValue(it).isNotEmpty() || circleHit.getValue(it) }
                .sortedWith(compareBy({ parseId(it)?.first ?: 0 }, { parseId(it)?.second ?: 0 }))
            for (id in targets) {
                val (num, side) = parseId(id) ?: continue
                val shape = shapes[num] ?: continue
                if (isMovedByUser(shape, side)) continue
                if (isShortestSide(shape, side)) continue
                val current = horizontalOf(shape, side) ?: continue
                val ladder = if (side == SIDE_SOKUTEN) {
                    sokutenLadder(current)
                } else {
                    val slide = listOf(IN_RIGHT, IN_LEFT)
                    val outer = if (allowFlagOut) outerOrder(shape, side) else emptyList()
                    (slide + outer).filter { it != current }
                }

                for (candidate in ladder) {
                    setHorizontal(shape, side, candidate)
                    refreshShape(num)
                    val after = collidingCount()
                    if (after < baseline) {
                        originals.getOrPut(num to side) { current }
                        applied[num to side] = candidate
                        baseline = after
                        improved = true
                        break
                    }
                    setHorizontal(shape, side, current) // 効かなかったら戻す
                    refreshShape(num)
                }
            }
            if (!improved) return@repeat
        }

        // 探索で実際にモデルを触っているので、呼び出し側が apply するまで元へ戻す
        // (solve は「提案を返す純関数」として使えないと、preview と本番で経路が割れる)
        for ((key, original) in originals) {
            val (num, side) = key
            shapes[num]?.let { setHorizontal(it, side, original) }
        }
        return applied.entries
            .map { (key, value) -> Move(key.first, key.second, originals.getValue(key), value) }
            .sortedWith(compareBy({ it.shapeNumber }, { it.side }))
    }

    fun apply(list: EditList<out CycleShape>, moves: List<Move>) {
        val byKey = moves.associateBy { it.shapeNumber to it.side }
        list.forEachItemIndexed { num, shape ->
            for (side in listOf(0, 1, 2, SIDE_SOKUTEN)) {
                byKey[num to side]?.let { setHorizontal(shape, side, it.to) }
            }
        }
    }
}
