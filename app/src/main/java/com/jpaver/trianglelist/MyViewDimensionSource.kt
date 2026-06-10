package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement

/**
 * MyView (Android 画面 + PDF 印刷) の寸法系描画が使う座標の出所。ADR 0003 Phase 2c。
 *
 * DxfFileWriter / SfcWriter の private layoutTriple と同型で、Triangle のキャッシュ
 * (dimOnPath / pathS) と同じ引数順を common の式 DimensionLayout.layout(gapPaperMm=0) に渡す:
 *   A = (pointAB, point[0], dim.vertical.a, dim.horizontal.a)
 *   B = (pointBC, pointAB, dim.vertical.b, dim.horizontal.b)
 *   C = (point[0], pointBC, dim.vertical.c, dim.horizontal.c)
 *   測点名 = (pointAB, point[0], SIDE_SOKUTEN, dim.horizontal.s)
 * (出所は TriangleDimExtensions.kt:26-29 を Read で実測)
 *
 * 式 ≡ キャッシュの数値同値は DimensionLayoutParityTest が全分岐で証明済み。画面は
 * テキスト golden が録れないため、この pure helper を「配線 (辺の対応 / y 反転 / shadow の
 * B・C)」の回帰テスト対象として切り出す。MyView 本体は View 継承で JVM 単体テスト不向きな
 * ため、Android 非依存の object としてここに置く。
 */
object MyViewDimensionSource {

    /** 3 辺 (A/B/C) の寸法配置。dimOnPath[0]/[1]/[2] に対応。 */
    fun triple(tri: Triangle): Triple<DimensionPlacement, DimensionPlacement, DimensionPlacement> {
        val scale = tri.scaleFactor
        val dimheight = tri.dimHeight
        return Triple(
            DimensionLayout.layout(tri.pointAB, tri.point[0], tri.dim.vertical.a, tri.dim.horizontal.a, scale, dimheight, 0f),
            DimensionLayout.layout(tri.pointBC, tri.pointAB, tri.dim.vertical.b, tri.dim.horizontal.b, scale, dimheight, 0f),
            DimensionLayout.layout(tri.point[0], tri.pointBC, tri.dim.vertical.c, tri.dim.horizontal.c, scale, dimheight, 0f)
        )
    }

    /** 測点名の配置。pathS に対応。 */
    fun sokuten(tri: Triangle): DimensionPlacement =
        DimensionLayout.layout(
            tri.pointAB, tri.point[0],
            DimensionLayout.SIDE_SOKUTEN, tri.dim.horizontal.s,
            tri.scaleFactor, tri.dimHeight, 0f
        )

    /** 影三角形は B/C 辺の寸法値のみ描く (drawShadowTriangle)。shadowTri の dimOnPath[1]/[2] に対応。 */
    fun shadowBC(tri: Triangle): Pair<DimensionPlacement, DimensionPlacement> {
        val (_, b, c) = triple(tri)
        return Pair(b, c)
    }
}
