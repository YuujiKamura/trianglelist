package com.jpaver.trianglelist.label

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
        fun collisions(): Map<String, ObstacleKind> =
            ModelOverlapAnalyzer.analyze(list, judgeSize, scale, sokutenListVector, thresholdAngle, metrics)
                .collisionKindByText

        val shapes = mutableMapOf<Int, CycleShape>()
        list.forEachItemIndexed { num, shape -> shapes[num] = shape }

        val originals = mutableMapOf<Pair<Int, Int>, Int>()
        val applied = mutableMapOf<Pair<Int, Int>, Int>()

        repeat(maxPasses) {
            var improved = false
            var baseline = collisions()
            if (baseline.isEmpty()) return@repeat

            // 対象は「文字どうし」で衝突している寸法値のみ。番号サークルとの衝突は
            // NumberCircleEscape の担当 (自由に動ける方を先に動かす)
            val targets = baseline.filterValues { it == ObstacleKind.LABEL }.keys.sorted()
            for (id in targets) {
                val (num, side) = parseId(id) ?: continue
                val shape = shapes[num] ?: continue
                if (isMovedByUser(shape, side)) continue
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
                    val after = collisions()
                    if (after.size < baseline.size) {
                        originals.getOrPut(num to side) { current }
                        applied[num to side] = candidate
                        baseline = after
                        improved = true
                        break
                    }
                    setHorizontal(shape, side, current) // 効かなかったら戻す
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
