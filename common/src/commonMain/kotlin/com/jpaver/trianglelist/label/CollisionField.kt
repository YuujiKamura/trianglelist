package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import kotlin.math.hypot
import kotlin.math.max

/** 障害物の種別。線分 (三角形の辺) / 確定済みラベル (OBB) / 円 (番号サークル等)。 */
enum class ObstacleKind { EDGE, LABEL, CIRCLE }

/**
 * query にヒットした相手。相手の id と種別、重なり深さ (model mm) を返す。
 * depthMm は 0 = 境界接触 (contact、寄り添い)、> 0 = めり込み (intrusion)。
 * 閾値での足切りはここではしない ── 観測層が LabelBox.EPS を境に分けて報告する。
 */
data class Hit(val id: String, val kind: ObstacleKind, val depthMm: Float)

/**
 * メッシュ全体の障害物集合への衝突クエリ (ADR 0002 採用、Measurement 層)。
 *
 * 「接触はメッシュ全体の性質なのに判定が三角形単体に閉じていた」という 5 年の構造的原因
 * (ADR 0002 Context 3) への対処。障害物 (辺 = 線分、確定済みラベル = LabelBox、円) を
 * id/種別付きで登録し、候補 (LabelBox または円) を渡すと重なっている相手を全部返す。
 *
 * 円は一級プリミティブ (rev1)。外接 box 近似ではなく閉形式で判定する:
 * 円 vs OBB = box ローカルで円中心をクランプした最近点距離 (LabelBox 側に実装)、
 * 円 vs 線分 = 点と線分の最短距離 ≤ r、円 vs 円 = 中心距離 ≤ r1+r2。
 *
 * 状態は登録された障害物のみ。判定ロジックに閾値や図面知識 (面積/辺長/角度のプロキシ) は
 * 一切持ち込まない ── 純幾何の衝突クエリに徹する。性能は素朴な全走査 (三角形数百 ×
 * ラベル数個の規模で十分)。空間インデックスは入れない (YAGNI)。
 */
class CollisionField {
    private val edges = mutableListOf<Edge>()
    private val boxes = mutableListOf<Box>()
    private val circles = mutableListOf<Circle>()

    private data class Edge(val id: String, val start: PointXY, val end: PointXY)
    private data class Box(val id: String, val box: LabelBox)
    private data class Circle(val id: String, val center: PointXY, val radiusMm: Float)

    /** 辺 (線分) を障害物として登録する。 */
    fun addEdge(id: String, start: PointXY, end: PointXY) {
        edges.add(Edge(id, start, end))
    }

    /** 確定済みラベル (OBB) を障害物として登録する。 */
    fun addBox(id: String, box: LabelBox) {
        boxes.add(Box(id, box))
    }

    /** 円 (番号サークル等) を障害物として登録する。 */
    fun addCircle(id: String, center: PointXY, radiusMm: Float) {
        circles.add(Circle(id, center, radiusMm))
    }

    /**
     * 候補 box に重なっている障害物を、重なり深さ付きで全部返す。
     * @param excludeId 自分自身など除外したい障害物の id (null なら全件対象)。
     */
    fun query(box: LabelBox, excludeId: String? = null): List<Hit> {
        val hits = mutableListOf<Hit>()
        for (edge in edges) {
            if (edge.id == excludeId) continue
            box.penetrationDepthSegment(edge.start, edge.end)?.let {
                hits.add(Hit(edge.id, ObstacleKind.EDGE, it))
            }
        }
        for (entry in boxes) {
            if (entry.id == excludeId) continue
            box.penetrationDepth(entry.box)?.let {
                hits.add(Hit(entry.id, ObstacleKind.LABEL, it))
            }
        }
        for (circle in circles) {
            if (circle.id == excludeId) continue
            box.penetrationDepthCircle(circle.center, circle.radiusMm)?.let {
                hits.add(Hit(circle.id, ObstacleKind.CIRCLE, it))
            }
        }
        return hits
    }

    /**
     * 候補円に重なっている障害物を、重なり深さ付きで全部返す。
     * @param excludeId 自分自身など除外したい障害物の id (null なら全件対象)。
     */
    fun queryCircle(center: PointXY, radiusMm: Float, excludeId: String? = null): List<Hit> {
        val hits = mutableListOf<Hit>()
        for (edge in edges) {
            if (edge.id == excludeId) continue
            val distance = distancePointToSegment(center, edge.start, edge.end)
            if (distance <= radiusMm + LabelBox.EPS) {
                hits.add(Hit(edge.id, ObstacleKind.EDGE, max(0f, radiusMm - distance)))
            }
        }
        for (entry in boxes) {
            if (entry.id == excludeId) continue
            entry.box.penetrationDepthCircle(center, radiusMm)?.let {
                hits.add(Hit(entry.id, ObstacleKind.LABEL, it))
            }
        }
        for (circle in circles) {
            if (circle.id == excludeId) continue
            val distance = hypot(circle.center.x - center.x, circle.center.y - center.y)
            val radiusSum = radiusMm + circle.radiusMm
            if (distance <= radiusSum + LabelBox.EPS) {
                hits.add(Hit(circle.id, ObstacleKind.CIRCLE, max(0f, radiusSum - distance)))
            }
        }
        return hits
    }

    /** 点 p と線分 ab の最短距離。射影パラメータを [0,1] にクランプする標準形。 */
    private fun distancePointToSegment(p: PointXY, a: PointXY, b: PointXY): Float {
        val abX = b.x - a.x
        val abY = b.y - a.y
        val lengthSq = abX * abX + abY * abY
        val t = if (lengthSq == 0f) {
            0f // 退化線分 (長さ 0) は端点との距離
        } else {
            (((p.x - a.x) * abX + (p.y - a.y) * abY) / lengthSq).coerceIn(0f, 1f)
        }
        return hypot(p.x - (a.x + t * abX), p.y - (a.y + t * abY))
    }
}
