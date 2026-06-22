package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.DimOnPath
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.label.DimensionPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ADR 0003 Phase 2c の配線テスト。
 *
 * 画面 (MyView) は DXF/SFC のようなテキスト golden が録れない。式 ≡ キャッシュの数値同値は
 * DimensionLayoutParityTest が全分岐で証明済みなので、ここで担保するのは「MyView が辺 X に
 * どの placement を使うか」= 配線 (辺の対応間違い / y 反転漏れ / shadow の B・C 取り違え) の回帰。
 *
 * MyViewDimensionSource が返す placement の (pointA/pointB/offsetH/offsetV/dimpoint) が、
 * Triangle が同じ辺について持つキャッシュ dimOnPath[0..2] / pathS と一致することを照合する。
 * もし辺順を入れ替える / y 反転を落とす / shadow で B・C を取り違えると、この照合が fail する。
 *
 * fixture は DxfDimensionLayoutGoldenTest.dimVariantsTriList と同じ構成
 * (CENTER / INRIGHT / INLEFT / OUTERRIGHT / OUTERLEFT / vertical=3 / 測点名 を網羅)。
 * Phase 2a でキャッシュ汚染 (clone エイリアス) は式を正と確定済みのため、汚染の出ない
 * 単純列 (move/clone を経ない素の setDimPath 再構築) で配線だけを見る。
 */
class MyViewDimensionWiringTest {

    private val eps = 1e-3

    private fun dimVariantsTriList(): TriangleList {
        val tList = TriangleList()
        tList.add(Triangle(8f, 6f, 7f), true)
        tList.add(Triangle(tList[1], 1, 5f, 6f), true)
        tList.add(Triangle(tList[1], 2, 6f, 5f), true)

        tList[1].setDimAligns(0, 1, 2, 1, 1, 3)
        tList[1].setDimPath(0.05f)
        tList[1].setDimPoint()
        tList[2].setDimAligns(0, 3, 0, 1, 1, 1)
        tList[2].setDimPath(0.05f)
        tList[2].setDimPoint()
        tList[3].setDimAligns(0, 0, 4, 3, 1, 1)
        tList[3].setDimPath(0.05f)
        tList[3].setDimPoint()

        tList[1].name = "No.0"
        tList[1].setDimPath(0.05f)
        tList[2].name = "No.1"
        tList[2].setDimPath(0.05f)
        return tList
    }

    private fun assertPlacementMatchesCache(label: String, place: DimensionPlacement, cache: DimOnPath) {
        assertEquals("$label pointA.x", cache.pointA.x, place.pointA.x, eps)
        assertEquals("$label pointA.y", cache.pointA.y, place.pointA.y, eps)
        assertEquals("$label pointB.x", cache.pointB.x, place.pointB.x, eps)
        assertEquals("$label pointB.y", cache.pointB.y, place.pointB.y, eps)
        assertEquals("$label offsetH", cache.offsetH, place.offsetH, eps)
        assertEquals("$label offsetV", cache.offsetV, place.offsetV, eps)
        assertEquals("$label dimpoint.x", cache.dimpoint.x, place.dimpoint.x, eps)
        assertEquals("$label dimpoint.y", cache.dimpoint.y, place.dimpoint.y, eps)
    }

    @Test
    fun `各辺の placement は同じ辺のキャッシュ dimOnPath に一致する`() {
        val tList = dimVariantsTriList()
        for (n in 1..tList.size()) {
            val tri = tList[n]
            val (pa, pb, pc) = MyViewDimensionSource.triple(tri)
            assertPlacementMatchesCache("tri#$n A", pa, tri.dimOnPath[0])
            assertPlacementMatchesCache("tri#$n B", pb, tri.dimOnPath[1])
            assertPlacementMatchesCache("tri#$n C", pc, tri.dimOnPath[2])
        }
    }

    @Test
    fun `測点名の placement は pathS のキャッシュに一致する`() {
        val tList = dimVariantsTriList()
        for (n in 1..tList.size()) {
            val tri = tList[n]
            assertPlacementMatchesCache("tri#$n S", MyViewDimensionSource.sokuten(tri), tri.pathS)
        }
    }

    /** placement とキャッシュが全フィールド一致するか (辺ごとに dimpoint/offset が異なるので、別辺なら false) */
    private fun fullyMatches(place: DimensionPlacement, cache: DimOnPath): Boolean =
        kotlin.math.abs(place.pointA.x - cache.pointA.x) < eps &&
        kotlin.math.abs(place.pointA.y - cache.pointA.y) < eps &&
        kotlin.math.abs(place.pointB.x - cache.pointB.x) < eps &&
        kotlin.math.abs(place.pointB.y - cache.pointB.y) < eps &&
        kotlin.math.abs(place.offsetH - cache.offsetH) < eps &&
        kotlin.math.abs(place.offsetV - cache.offsetV) < eps &&
        kotlin.math.abs(place.dimpoint.x - cache.dimpoint.x) < eps &&
        kotlin.math.abs(place.dimpoint.y - cache.dimpoint.y) < eps

    @Test
    fun `辺をずらした placement はキャッシュに一致しない (配線テストが効くことの担保)`() {
        // 辺順を取り違えた配線 (A の placement を B/C のキャッシュと照合) なら全フィールド照合が
        // 崩れることを示す。これが pass してしまうとテスト自体が無力 = 配線回帰を検出できない。
        // (pointA だけだと共有頂点 pointAB の swap で偶然一致し得るため、全フィールドで判定する)
        val tri = dimVariantsTriList()[1]
        val (pa, _, _) = MyViewDimensionSource.triple(tri)
        assertEquals("A の placement は A 辺キャッシュには全一致するはず", true, fullyMatches(pa, tri.dimOnPath[0]))
        assertEquals("A の placement は B 辺キャッシュには全一致しないはず", false, fullyMatches(pa, tri.dimOnPath[1]))
        assertEquals("A の placement は C 辺キャッシュには全一致しないはず", false, fullyMatches(pa, tri.dimOnPath[2]))
    }

    @Test
    fun `shadowBC は B・C 辺の placement を順に返す`() {
        val tri = dimVariantsTriList()[1]
        val (b, c) = MyViewDimensionSource.shadowBC(tri)
        assertPlacementMatchesCache("shadow B", b, tri.dimOnPath[1])
        assertPlacementMatchesCache("shadow C", c, tri.dimOnPath[2])
    }
}
