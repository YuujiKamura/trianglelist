package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 三角形および多角形（CycleShape）の幾何学的当たり判定（衝突・重複検出）ユーティリティ。
 *
 * 【検出対象】
 * 1. 離れた枝同士の交差・貫通（展開図の自己交差）
 * 2. 隣接する親子三角形の内向き折り返し（親の内部への潜り込み）
 * 3. 同一位置への重複生成
 * 4. 三角形内部への頂点侵入
 */
object TriangleCollisionDetector {

    const val DEFAULT_EPSILON = 1e-3

    /**
     * 2つの有向線分 (p1->p2) と (p3->p4) が真に交差（端点共有を除く内部交差）するか判定する。
     */
    fun doSegmentsCross(
        p1: PointXY,
        p2: PointXY,
        p3: PointXY,
        p4: PointXY,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        // 端点がほぼ一致している（頂点を共有している）場合は内部交差とみなさない
        if (arePointsClose(p1, p3, eps) || arePointsClose(p1, p4, eps) ||
            arePointsClose(p2, p3, eps) || arePointsClose(p2, p4, eps)
        ) {
            return false
        }

        // Bounding box クイック棄却
        if (max(p1.x, p2.x) < min(p3.x, p4.x) - eps ||
            min(p1.x, p2.x) > max(p3.x, p4.x) + eps ||
            max(p1.y, p2.y) < min(p3.y, p4.y) - eps ||
            min(p1.y, p2.y) > max(p3.y, p4.y) + eps
        ) {
            return false
        }

        val d1 = ccw(p3, p4, p1)
        val d2 = ccw(p3, p4, p2)
        val d3 = ccw(p1, p2, p3)
        val d4 = ccw(p1, p2, p4)

        // p1 と p2 が線分 p3-p4 の厳密に異なる側にあり、かつ p3 と p4 が線分 p1-p2 の厳密に異なる側にある
        val cross1 = (d1 > eps && d2 < -eps) || (d1 < -eps && d2 > eps)
        val cross2 = (d3 > eps && d4 < -eps) || (d3 < -eps && d4 > eps)

        return cross1 && cross2
    }

    /**
     * 点 p が三角形 (a, b, c) の厳密な内部（境界線を含まない）にあるか判定する。
     */
    fun isPointStrictlyInside(
        p: PointXY,
        a: PointXY,
        b: PointXY,
        c: PointXY,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        val c1 = ccw(a, b, p)
        val c2 = ccw(b, c, p)
        val c3 = ccw(c, a, p)

        val allPositive = c1 > eps && c2 > eps && c3 > eps
        val allNegative = c1 < -eps && c2 < -eps && c3 < -eps

        return allPositive || allNegative
    }

    /**
     * 点 p が凸多角形 vertices の厳密な内部にあるか判定する。
     */
    fun isPointStrictlyInsidePolygon(
        p: PointXY,
        vertices: List<PointXY>,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        val n = vertices.size
        if (n < 3) return false

        var hasPos = false
        var hasNeg = false

        for (i in 0 until n) {
            val v1 = vertices[i]
            val v2 = vertices[(i + 1) % n]
            val c = ccw(v1, v2, p)
            if (c > eps) hasPos = true
            if (c < -eps) hasNeg = true
            if (hasPos && hasNeg) return false
            if (abs(c) <= eps) {
                // 境界線上は厳密な内部ではない
                return false
            }
        }

        return hasPos || hasNeg
    }

    /**
     * 親子関係にある三角形同士で、子が親の基線から内側（親の内部方向）へ折り返しているか判定する。
     * 正常な展開図では親の対頂点と子の対頂点は基線の反対側に位置しなければならない。
     */
    fun isChildFoldingInward(
        child: Triangle,
        parent: Triangle,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        val side = child.connectionSide
        if (side !in 1..2) return false

        // 親の接続辺の両端点
        val pLine = parent.getLine(side)
        val pStart = pLine.left
        val pEnd = pLine.right

        // 親の対頂点 (side 1: B辺なら pointCA (point[0]), side 2: C辺なら pointAB)
        val parentOpposite = if (side == 1) parent.point[0] else parent.pointAB

        // 子の対頂点（apex）
        val childApex = child.pointBC

        val cpParent = ccw(pStart, pEnd, parentOpposite)
        val cpChild = ccw(pStart, pEnd, childApex)

        // 親が縮退していなければ |cpParent| > eps
        if (abs(cpParent) <= eps) return false

        // 子の頂点が親と同じ半平面側にある (同じ符号)、または基線上に潰れている場合 -> 内向き折り返し
        return (cpParent * cpChild) > eps || abs(cpChild) <= eps
    }

    /**
     * CycleShape 親（台形など）に対する子の内向き判定。
     */
    fun isChildFoldingInwardToCycleShape(
        child: CycleShape,
        parent: CycleShape,
        side: Int,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        if (!parent.isClosed()) return false
        val edge = parent.getLine(side)
        val outward = try {
            parent.outwardPerpUnit(side)
        } catch (_: Throwable) {
            return false
        }

        if (outward.x == 0.0 && outward.y == 0.0) return false

        val midX = (edge.left.x + edge.right.x) * 0.5
        val midY = (edge.left.y + edge.right.y) * 0.5

        // 子の重心または頂点が outward 側に向いているか
        val childCentroid = child.centroid()
        val dot = (childCentroid.x - midX) * outward.x + (childCentroid.y - midY) * outward.y
        return dot <= eps
    }

    /**
     * 2つの三角形 t1 と t2 が重なっているか（面積の重複・交差があるか）を総合判定する。
     */
    fun doTrianglesOverlap(
        t1: Triangle,
        t2: Triangle,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        if (t1 === t2) return false

        val v1 = listOf(t1.point[0], t1.pointAB, t1.pointBC)
        val v2 = listOf(t2.point[0], t2.pointAB, t2.pointBC)

        // 1. バウンディングボックスによる早期棄却
        val minX1 = min(min(v1[0].x, v1[1].x), v1[2].x)
        val maxX1 = max(max(v1[0].x, v1[1].x), v1[2].x)
        val minY1 = min(min(v1[0].y, v1[1].y), v1[2].y)
        val maxY1 = max(max(v1[0].y, v1[1].y), v1[2].y)

        val minX2 = min(min(v2[0].x, v2[1].x), v2[2].x)
        val maxX2 = max(max(v2[0].x, v2[1].x), v2[2].x)
        val minY2 = min(min(v2[0].y, v2[1].y), v2[2].y)
        val maxY2 = max(max(v2[0].y, v2[1].y), v2[2].y)

        if (maxX1 < minX2 - eps || minX1 > maxX2 + eps ||
            maxY1 < minY2 - eps || minY1 > maxY2 + eps
        ) {
            return false
        }

        // 2. 親子関係にある隣接ペアの内向き折り返しチェック
        if (t2.parentnumber == t1.mynumber && t2.connectionType_ == 0) {
            if (isChildFoldingInward(t2, t1, eps)) return true
        } else if (t1.parentnumber == t2.mynumber && t1.connectionType_ == 0) {
            if (isChildFoldingInward(t1, t2, eps)) return true
        }

        // 3. 辺同士の内部交差チェック
        for (i in 0 until 3) {
            val a1 = v1[i]
            val a2 = v1[(i + 1) % 3]
            for (j in 0 until 3) {
                val b1 = v2[j]
                val b2 = v2[(j + 1) % 3]
                if (doSegmentsCross(a1, a2, b1, b2, eps)) {
                    return true
                }
            }
        }

        // 4. 頂点侵入チェック（共有頂点を除く）
        for (pt in v1) {
            if (!isAnyPointClose(pt, v2, eps) && isPointStrictlyInside(pt, v2[0], v2[1], v2[2], eps)) {
                return true
            }
        }
        for (pt in v2) {
            if (!isAnyPointClose(pt, v1, eps) && isPointStrictlyInside(pt, v1[0], v1[1], v1[2], eps)) {
                return true
            }
        }

        // 5. 完全一致（全頂点が共有されている）チェック
        val sharedVertices = v1.count { isAnyPointClose(it, v2, eps) }
        if (sharedVertices >= 3) {
            return true
        }

        return false
    }

    /**
     * 一般の CycleShape 同士の重複判定。
     */
    fun doShapesOverlap(
        s1: CycleShape,
        s2: CycleShape,
        eps: Double = DEFAULT_EPSILON
    ): Boolean {
        if (s1 === s2) return false
        if (s1 is Triangle && s2 is Triangle) {
            return doTrianglesOverlap(s1, s2, eps)
        }

        val v1 = s1.vertices()
        val v2 = s2.vertices()
        if (v1.size < 3 || v2.size < 3) return false

        // 親子関係の内向き折り返し
        if (s2.parentnumber == s1.mynumber) {
            if (isChildFoldingInwardToCycleShape(s2, s1, s2.connectionSide, eps)) {
                return true
            }
        } else if (s1.parentnumber == s2.mynumber) {
            if (isChildFoldingInwardToCycleShape(s1, s2, s1.connectionSide, eps)) {
                return true
            }
        }

        // 辺の内部交差
        for (i in v1.indices) {
            val a1 = v1[i]
            val a2 = v1[(i + 1) % v1.size]
            for (j in v2.indices) {
                val b1 = v2[j]
                val b2 = v2[(j + 1) % v2.size]
                if (doSegmentsCross(a1, a2, b1, b2, eps)) {
                    return true
                }
            }
        }

        // 頂点侵入
        for (pt in v1) {
            if (!isAnyPointClose(pt, v2, eps) && isPointStrictlyInsidePolygon(pt, v2, eps)) {
                return true
            }
        }
        for (pt in v2) {
            if (!isAnyPointClose(pt, v1, eps) && isPointStrictlyInsidePolygon(pt, v1, eps)) {
                return true
            }
        }

        return false
    }

    /**
     * 2D 外積 (cross product): (p2 - p1) × (p3 - p1)
     * > 0: CCW (反時計回り), < 0: CW (時計回り), = 0: 共線 (collinear)
     */
    fun ccw(p1: PointXY, p2: PointXY, p3: PointXY): Double {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)
    }

    private fun arePointsClose(p1: PointXY, p2: PointXY, eps: Double): Boolean {
        return abs(p1.x - p2.x) <= eps && abs(p1.y - p2.y) <= eps
    }

    private fun isAnyPointClose(pt: PointXY, list: List<PointXY>, eps: Double): Boolean {
        return list.any { arePointsClose(pt, it, eps) }
    }
}
