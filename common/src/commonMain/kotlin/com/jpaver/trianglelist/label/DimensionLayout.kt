package com.jpaver.trianglelist.label

import com.example.trilib.PointXY

/**
 * 寸法値のデフォルト配置の計算結果 (ADR 0003 の「式」層の出力)。
 * offsetV は dimpoint に織り込まれない (DimOnPath と同じ。描画側が法線方向に適用する)。
 */
data class DimensionPlacement(
    val dimpoint: PointXY,
    val offsetH: Float,
    val offsetV: Float,
    val verticalDxf: Int,
    val pointA: PointXY,
    val pointB: PointXY,
    val clockwise: String
)

/**
 * 寸法値のデフォルト配置式 (ADR 0003 Phase 1)。
 * app/editmodel/DimOnPath.kt の式を common に引っ越した pure 実装。状態を持たない。
 *
 * gapPaperMm=0 のとき DimOnPath と数値同値であることが唯一最大の要件。
 * gapPaperMm>0 は AutoCAD DIMGAP 相当の口 (Phase 4 で使う)。紙 mm を scale で
 * モデル単位に直し、最終 offsetV と同じ向き (寸法線からテキストを離す向き) に加算する。
 */
object DimensionLayout {

    // horizontal (DimOnPath と同じ int コード、CSV 永続化済みのため変更不可)
    const val CENTER = 0
    const val INRIGHT = 1
    const val INLEFT = 2
    const val OUTERRIGHT = 3
    const val OUTERLEFT = 4

    // vertical (1=下/外、3=上/内、4=測点名)
    const val SIDE_SOKUTEN = 4
    const val UPPER = 3
    const val LOWER = 1
    const val INNER = 3
    const val OUTER = 1

    fun layout(
        leftP: PointXY,
        rightP: PointXY,
        vertical: Int,
        horizontal: Int,
        scale: Float = 1.0f,
        dimheight: Float = 0.05f,
        gapPaperMm: Float = 0f
    ): DimensionPlacement {
        val offsetUpper = -dimheight * 0.2f
        val offsetLower = dimheight * 0.9f

        var v = vertical
        var pointA = leftP
        var pointB = rightP
        var offsetH = 0f
        var offsetV = 0f
        var clockwise = "C"

        if (v == 1) offsetV = offsetLower
        if (v == 3) offsetV = offsetUpper

        if (vertical == SIDE_SOKUTEN) {
            // 測点名: 辺の進行方向の手前外側に旗状に置く。horizontal は左右の別 (1 で逆側)
            var lp = leftP
            var rp = rightP
            if (horizontal == 1) {
                lp = rightP
                rp = leftP
            }
            val outerleft = lp.offset(rp, -3f * scale)
            val outerright = lp.offset(rp, -0.5f * scale)
            pointA = outerleft
            pointB = outerright
            // 上下逆さまにならない様に y 降順へ正規化
            if (pointA.y < pointB.y) {
                val tmp = pointA
                pointA = pointB
                pointB = tmp
            }
        } else {
            val lineLength = leftP.lengthTo(rightP)
            val habayose = lineLength * 0.1f

            when (horizontal) {
                CENTER -> {}
                INRIGHT -> offsetH = -habayose
                INLEFT -> offsetH = habayose
                OUTERRIGHT -> {
                    val outer = pointsOuter(rightP, leftP, lineLength, scale)
                    pointA = outer.pointA
                    pointB = outer.pointB
                    offsetH = outer.offsetH
                    // DimOnPath.kt:74 の vertical=flip(vertical) は、OUTER/INNER (L102-105) が
                    // init ブロックより後に宣言されているため init 実行中は両方 0 のままで、
                    // 実際には常に vertical=0 を代入する (意図された上下反転は機能していない)。
                    // Phase 1 は数値同値が要件のためこの実挙動を保存する。
                    v = 0
                }
                OUTERLEFT -> {
                    val outer = pointsOuter(leftP, rightP, lineLength, scale)
                    pointA = outer.pointA
                    pointB = outer.pointB
                    offsetH = outer.offsetH
                }
            }

            // 上下逆さまにならない様に反転 (DimOnPath は AB swap + offsetH 符号反転 +
            // offsetV の入れ替えのみ。vertical 変数は反転しない)
            if (pointA.x >= pointB.x) {
                clockwise = "A"
                offsetH = -offsetH
                val tmp = pointA
                pointA = pointB
                pointB = tmp
                if (v == 1) offsetV = offsetUpper
                if (v == 3) offsetV = offsetLower
            }
        }

        val dimpoint = pointA.calcMidPoint(pointB).offset(pointB, offsetH)

        // DIMGAP 相当 (Phase 4 の口)。gap=0 なら一切触らない = DimOnPath と同値。
        // 測点名 (offsetV=0) は寸法値ではないため gap 対象外。
        if (gapPaperMm != 0f && offsetV != 0f) {
            val gapModel = gapPaperMm * scale
            offsetV += if (offsetV > 0f) gapModel else -gapModel
        }

        return DimensionPlacement(
            dimpoint = dimpoint,
            offsetH = offsetH,
            offsetV = offsetV,
            verticalDxf = verticalDxf(leftP, rightP, v),
            pointA = pointA,
            pointB = pointB,
            clockwise = clockwise
        )
    }

    private data class OuterPoints(
        val pointA: PointXY,
        val pointB: PointXY,
        val offsetH: Float
    )

    // 左右旗揚げ: 辺の延長線上に HATAAGE ぶん出し、反対側へ隙間+辺長ぶん移動した区間に置く
    private fun pointsOuter(
        leftP: PointXY,
        rightP: PointXY,
        lineLength: Float,
        scale: Float
    ): OuterPoints {
        val sukima = 0.5f * scale
        val movement = sukima + lineLength
        val hataage = -3 * scale
        val habayose = -lineLength * 0.05f
        return OuterPoints(
            pointA = leftP.offset(rightP, hataage),
            pointB = rightP.offset(leftP, movement),
            offsetH = habayose
        )
    }

    // 基準線の方向が右向きか左向きかで上下を反転する (DXF の垂直位置合わせコード)
    private fun verticalDxf(leftP: PointXY, rightP: PointXY, vertical: Int): Int {
        if (vertical == OUTER) {
            return if (rightP.isVectorToRight(leftP)) LOWER else UPPER
        }
        return if (rightP.isVectorToRight(leftP)) UPPER else LOWER
    }
}
