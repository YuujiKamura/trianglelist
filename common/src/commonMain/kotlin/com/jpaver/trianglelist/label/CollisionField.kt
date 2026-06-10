package com.jpaver.trianglelist.label

import com.example.trilib.PointXY

/** 障害物の種別。線分 (三角形の辺) か、確定済みラベル (サークルは外接 box で登録) か。 */
enum class ObstacleKind { EDGE, LABEL }

/** query にヒットした相手。相手の id と種別だけを返す (幾何の中身は持たない)。 */
data class Hit(val id: String, val kind: ObstacleKind)

/**
 * メッシュ全体の障害物集合への衝突クエリ (ADR 0002 採用、Measurement 層)。
 *
 * 「接触はメッシュ全体の性質なのに判定が三角形単体に閉じていた」という 5 年の構造的原因
 * (ADR 0002 Context 3) への対処。障害物 (辺 = 線分、確定済みラベル = LabelBox) を id/種別付きで
 * 登録し、候補ラベルの LabelBox を渡すと重なっている相手を全部返す。
 *
 * 状態は登録された障害物のみ。判定ロジックに閾値や図面知識 (面積/辺長/角度のプロキシ) は
 * 一切持ち込まない ── 純幾何の衝突クエリに徹する。性能は素朴な全走査 (三角形数百 ×
 * ラベル数個の規模で十分)。空間インデックスは入れない (YAGNI)。
 */
class CollisionField {
    private val edges = mutableListOf<Edge>()
    private val boxes = mutableListOf<Box>()

    private data class Edge(val id: String, val start: PointXY, val end: PointXY)
    private data class Box(val id: String, val box: LabelBox)

    /** 辺 (線分) を障害物として登録する。 */
    fun addEdge(id: String, start: PointXY, end: PointXY) {
        edges.add(Edge(id, start, end))
    }

    /** 確定済みラベル (サークルは外接 box) を障害物として登録する。 */
    fun addBox(id: String, box: LabelBox) {
        boxes.add(Box(id, box))
    }

    /**
     * 候補 box に重なっている障害物を全部返す。
     * @param excludeId 自分自身など除外したい障害物の id (null なら全件対象)。
     */
    fun query(box: LabelBox, excludeId: String? = null): List<Hit> {
        val hits = mutableListOf<Hit>()
        for (edge in edges) {
            if (edge.id == excludeId) continue
            if (box.intersectsSegment(edge.start, edge.end)) {
                hits.add(Hit(edge.id, ObstacleKind.EDGE))
            }
        }
        for (entry in boxes) {
            if (entry.id == excludeId) continue
            if (box.intersects(entry.box)) {
                hits.add(Hit(entry.id, ObstacleKind.LABEL))
            }
        }
        return hits
    }
}
