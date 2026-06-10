package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * テキスト実寸の回転付き矩形 (OBB = Oriented Bounding Box)。
 *
 * ラベル当たり判定 (ADR 0002 採用) の最下層 Measurement の中核。寸法値・番号サークル・
 * 控除名は「中心・幅・高さ・回転」で表せる実寸の矩形として扱い、辺や他ラベルとの
 * 「重なっている」事実を分離軸定理 (SAT) で判定する。状態は持たない (純幾何)。
 *
 * 交差判定に加えて重なり深さ (penetration depth, model mm) を返す。深さは
 * 「分離に必要な最小移動量」(SAT の MTV 大きさ、Ericson "Real-Time Collision
 * Detection" の標準形)。深さ 0 = 境界接触 (寄り添い)、深さ > 0 = めり込み、を
 * 観測層が区別できる素材になる ── 寸法値が自分の辺に接して置かれるのは正常配置
 * なので、接触とめり込みを同一視しない (rev1)。閾値での足切りはここではしない。
 *
 * 変数名規約 (ADR 0001 由来、scale/TextSizePolicy.kt に倣う): 寸法は単位 suffix を必ず持たせる。
 * widthMm / heightMm は model 空間のミリ (= paper mm × 縮尺分母)。無印 Float で渡り歩かない。
 *
 * @param center  矩形の中心 (model 空間)。
 * @param widthMm  回転前のローカル X 方向の幅 (model mm)。
 * @param heightMm 回転前のローカル Y 方向の高さ (model mm)。
 * @param rotationDeg 中心まわりの回転角 (度、反時計回り正 = PointXY.rotate と同じ向き)。
 */
class LabelBox(
    val center: PointXY,
    val widthMm: Float,
    val heightMm: Float,
    val rotationDeg: Float = 0f,
) {
    /**
     * 4 隅を返す。順序は回転前ローカルで左下→右下→右上→左上 (反時計回り) を
     * 中心まわりに rotationDeg 回転したもの。SAT の軸算出と投影に使う。
     */
    fun corners(): List<PointXY> {
        val halfWidthMm = widthMm / 2f
        val halfHeightMm = heightMm / 2f
        val local = listOf(
            PointXY(center.x - halfWidthMm, center.y - halfHeightMm),
            PointXY(center.x + halfWidthMm, center.y - halfHeightMm),
            PointXY(center.x + halfWidthMm, center.y + halfHeightMm),
            PointXY(center.x - halfWidthMm, center.y + halfHeightMm),
        )
        return local.map { it.rotate(center, rotationDeg) }
    }

    /**
     * OBB 同士の交差判定 (分離軸定理)。
     * 境界接触 (面積ゼロのかすめ) は交差として扱う ── 重なりリスクを保守側に検出する。
     */
    fun intersects(other: LabelBox): Boolean = penetrationDepth(other) != null

    /**
     * OBB 同士の重なり深さ (model mm)。分離していれば null。
     * 0 = 境界接触 (contact)、> 0 = めり込み (intrusion) の素材。
     */
    fun penetrationDepth(other: LabelBox): Float? {
        val cornersA = corners()
        val cornersB = other.corners()
        val axes = axesOf(cornersA) + axesOf(cornersB)
        return depthOverAxes(cornersA, cornersB, axes)
    }

    /**
     * OBB と線分 (辺セグメント) の交差判定 (SAT)。
     * 端点が box の辺にちょうど接触するケースは交差として扱う。
     */
    fun intersectsSegment(start: PointXY, end: PointXY): Boolean =
        penetrationDepthSegment(start, end) != null

    /**
     * OBB と線分の重なり深さ (model mm)。分離していれば null。
     * 線分が box の辺上を走る (寸法値が自分の辺に接して置かれる正常配置) なら 0、
     * 線分が box 内部を横切るなら「線分を box の外へ出すのに必要な移動量」> 0。
     * 退化線分 (長さ 0 = 点) は線分法線が消えるので box 2 軸だけで判定する。
     */
    fun penetrationDepthSegment(start: PointXY, end: PointXY): Float? {
        val cornersBox = corners()
        val segment = listOf(start, end)
        val segmentDir = end - start
        val segmentNormal = PointXY(-segmentDir.y, segmentDir.x)
        val axes = axesOf(cornersBox) + listOf(segmentNormal)
        return depthOverAxes(cornersBox, segment, axes)
    }

    /**
     * OBB と円の重なり深さ (model mm)。分離していれば null。
     * 閉形式: box ローカル座標で円中心を矩形にクランプした最近点との距離 d に対し
     * d ≤ r なら交差、深さ = r - d (境界接触で 0)。円中心が box 内部なら深さ = r。
     */
    fun penetrationDepthCircle(circleCenter: PointXY, radiusMm: Float): Float? {
        // 円中心を box ローカル (center 原点、回転を戻した軸並行) へ
        val unrotated = circleCenter.rotate(center, -rotationDeg)
        val localX = unrotated.x - center.x
        val localY = unrotated.y - center.y
        val clampedX = localX.coerceIn(-widthMm / 2f, widthMm / 2f)
        val clampedY = localY.coerceIn(-heightMm / 2f, heightMm / 2f)
        val distance = hypot(localX - clampedX, localY - clampedY)
        if (distance > radiusMm + EPS) return null
        return max(0f, radiusMm - distance)
    }

    private fun axesOf(corners: List<PointXY>): List<PointXY> {
        val edge0 = corners[1] - corners[0]
        val edge1 = corners[2] - corners[1]
        // エッジ (ex, ey) の法線は (-ey, ex)。深さを mm で比較するため使用時に正規化する。
        return listOf(
            PointXY(-edge0.y, edge0.x),
            PointXY(-edge1.y, edge1.x),
        )
    }

    /**
     * 全軸について「分離に必要な移動量」min(maxA-minB, maxB-minA) を取り、
     * 1 軸でも負 (= 分離) なら null、全軸で重なれば最小値を深さとして返す。
     * 内包・退化 (線分の点投影) も「重なり長さ」でなく移動量で測るので正しく拾える。
     */
    private fun depthOverAxes(a: List<PointXY>, b: List<PointXY>, axes: List<PointXY>): Float? {
        var minDepthMm = Float.POSITIVE_INFINITY
        for (axis in axes) {
            val length = sqrt(axis.x * axis.x + axis.y * axis.y)
            // 退化軸 (長さ 0 の線分の法線) は分離軸にならないので飛ばす
            if (length == 0f) continue
            val unitX = axis.x / length
            val unitY = axis.y / length
            val (minA, maxA) = projectOnto(a, unitX, unitY)
            val (minB, maxB) = projectOnto(b, unitX, unitY)
            // 接触を交差に含めるため境界に EPS の許容 (浮動小数 + 回転誤差の吸収も兼ねる)
            val separation = min(maxA - minB, maxB - minA)
            if (separation < -EPS) return null
            minDepthMm = min(minDepthMm, separation)
        }
        return max(0f, minDepthMm)
    }

    private fun projectOnto(points: List<PointXY>, unitX: Float, unitY: Float): Pair<Float, Float> {
        var minProj = Float.POSITIVE_INFINITY
        var maxProj = Float.NEGATIVE_INFINITY
        for (p in points) {
            val proj = p.x * unitX + p.y * unitY
            minProj = min(minProj, proj)
            maxProj = max(maxProj, proj)
        }
        return minProj to maxProj
    }

    companion object {
        /**
         * 境界接触を交差に含めるための許容幅 (model mm)。図面知識ではなく幾何ロバスト性のため。
         * 観測層が contact (深さ ≤ EPS) / intrusion (深さ > EPS) を分ける基準にも使う。
         *
         * 値の根拠: 座標は model mm で ~2.4e4 に達し、Float の相対精度 ~1.2e-7 から
         * 絶対ノイズは ~3e-3 mm、回転 (sin/cos 経由) でさらに積む。1e-4 ではノイズが
         * EPS を超えて「辺上に正確に置かれた回転寸法値」が偽 intrusion になった
         * (sample.dxf rot=84°/349°/76° で実測)。1e-2 mm はノイズの上、かつ視認可能な
         * 幾何のはるか下 (1/50 図面で紙 0.0002 mm)。
         */
        const val EPS: Float = 1e-2f
    }
}
