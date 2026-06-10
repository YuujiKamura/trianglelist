package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import kotlin.math.max
import kotlin.math.min

/**
 * テキスト実寸の回転付き矩形 (OBB = Oriented Bounding Box)。
 *
 * ラベル当たり判定 (ADR 0002 採用) の最下層 Measurement の中核。寸法値・番号サークル・
 * 控除名は「中心・幅・高さ・回転」で表せる実寸の矩形として扱い、辺や他ラベルとの
 * 「重なっている」事実を分離軸定理 (SAT) で判定する。状態は持たない (純幾何)。
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
     * OBB 同士の交差判定 (分離軸定理)。矩形の分離軸は各矩形の 2 つのエッジ法線。
     * 全 4 軸で投影区間が重なれば交差。1 つでも分離する軸があれば非交差。
     * 境界接触 (面積ゼロのかすめ) は交差として扱う ── 重なりリスクを保守側に検出する。
     */
    fun intersects(other: LabelBox): Boolean {
        val cornersA = corners()
        val cornersB = other.corners()
        val axes = axesOf(cornersA) + axesOf(cornersB)
        for (axis in axes) {
            if (!overlapsOnAxis(cornersA, cornersB, axis)) return false
        }
        return true
    }

    /**
     * OBB と線分 (辺セグメント) の交差判定 (SAT)。
     * 分離軸は box の 2 エッジ法線 + 線分自身の法線の計 3 軸。
     * 退化線分 (長さ 0 = 点) は線分法線が消えるので box 2 軸だけで「点が box 内か」を判定する。
     * 端点が box の辺にちょうど接触するケースは交差として扱う。
     */
    fun intersectsSegment(start: PointXY, end: PointXY): Boolean {
        val cornersBox = corners()
        val segment = listOf(start, end)
        val segmentDir = end - start
        val segmentNormal = PointXY(-segmentDir.y, segmentDir.x)
        val axes = axesOf(cornersBox) + listOf(segmentNormal)
        for (axis in axes) {
            // 退化軸 (長さ 0 の線分の法線) は分離軸にならないので飛ばす
            if (axis.x == 0f && axis.y == 0f) continue
            if (!overlapsOnAxis(cornersBox, segment, axis)) return false
        }
        return true
    }

    private fun axesOf(corners: List<PointXY>): List<PointXY> {
        val edge0 = corners[1] - corners[0]
        val edge1 = corners[2] - corners[1]
        // エッジ (ex, ey) の法線は (-ey, ex)。正規化は区間比較に不要。
        return listOf(
            PointXY(-edge0.y, edge0.x),
            PointXY(-edge1.y, edge1.x),
        )
    }

    private fun overlapsOnAxis(a: List<PointXY>, b: List<PointXY>, axis: PointXY): Boolean {
        val (minA, maxA) = projectOnto(a, axis)
        val (minB, maxB) = projectOnto(b, axis)
        // 接触を交差に含めるため境界に EPS の許容を与える (浮動小数 + 回転誤差の吸収も兼ねる)
        return maxA >= minB - EPS && maxB >= minA - EPS
    }

    private fun projectOnto(points: List<PointXY>, axis: PointXY): Pair<Float, Float> {
        var minProj = Float.POSITIVE_INFINITY
        var maxProj = Float.NEGATIVE_INFINITY
        for (p in points) {
            val proj = p.x * axis.x + p.y * axis.y
            minProj = min(minProj, proj)
            maxProj = max(maxProj, proj)
        }
        return minProj to maxProj
    }

    private companion object {
        /** 境界接触を交差に含めるための許容幅 (model mm)。図面知識ではなく幾何ロバスト性のため。 */
        const val EPS: Float = 1e-4f
    }
}
