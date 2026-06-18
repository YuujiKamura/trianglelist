package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 親図形に子図形を接続したときに、子が**必ず親辺の外側に展開される**ことを cartesian で
 * pin する。Triangle / Rectangle の混在 + 各 connectableSide + alignment の全組合せを生成して、
 * 子の centroid が親の centroid から見て親辺の反対側にあるかを assert する。
 *
 * 「親種別 × 子種別 × side × alignment」を機械的に回す写経で、新規組合せでバグが入ったら
 * 即落ちる ── shadow が内向きに描かれる類の幾何バグを ユニットテストで捕捉する仕組み (user
 * 指示 2026-06-18「だいぶ論外なのでユニットテストで捕捉できる仕組み作れ」)。
 *
 * 判定式: 親辺中点 M、親 centroid Pc、子 centroid Cc を取り、
 *   (M - Pc) · (M - Cc) < 0  ⇔  Pc と Cc が M を挟んで反対側 (= 子は親辺の外側)
 */
class ChildExpansionDirectionTest {

    private data class Case(
        val parentKind: String,
        val childKind: String,
        val side: Int,
        val alignment: Int,
    ) {
        override fun toString() = "$parentKind→$childKind side=$side align=$alignment"
    }

    private fun parentCsv(parentKind: String): String = when (parentKind) {
        "Triangle"  -> "1,6.0,5.0,4.0,-1,-1"
        "Rectangle" -> "Rectangle,1,5,10,7,-1,0"
        else -> error("unknown parent kind: $parentKind")
    }

    /** 子の CSV 行 (parent=1、conn=side、alignment は Rectangle のみ反映)。 */
    private fun childCsv(parentKind: String, childKind: String, side: Int, alignment: Int): String = when {
        // Rectangle 親 + Triangle 子: B/C を大きめにして親辺長 (7 / 5.83 等) で上書き後も三角不等式 OK
        childKind == "Triangle" && parentKind == "Rectangle" -> "2,10.0,6.0,5.0,1,$side"
        childKind == "Triangle" -> "2,5.0,4.0,3.0,1,$side"   // Triangle 親既存挙動
        childKind == "Rectangle" -> "Rectangle,2,3,4,3,1,$side,$alignment"
        else -> error("unknown child kind: $childKind")
    }

    /** Triangle 親は side ∈ {1,2}、Rectangle 親は side ∈ {1,2,3} が接続可能。 */
    private fun connectableSides(parentKind: String): List<Int> = when (parentKind) {
        "Triangle"  -> listOf(1, 2)
        "Rectangle" -> listOf(1, 2, 3)
        else -> emptyList()
    }

    /** Rectangle 子は alignment 0/1/2 を回す、Triangle 子は意味がないので 0 のみで打ち切る。 */
    private fun childAlignments(childKind: String): List<Int> = when (childKind) {
        "Rectangle" -> listOf(0, 1, 2)
        else -> listOf(0)
    }

    /** 親辺中点 M を挟んで Pc と Cc が反対側にある = 子は親辺の外側へ展開されている。 */
    private fun expandsOutside(parent: CycleShape, side: Int, child: CycleShape): Boolean {
        val edge = parent.getLine(side)
        val mid = PointXY(((edge.left.x + edge.right.x) * 0.5f), ((edge.left.y + edge.right.y) * 0.5f))
        val pc = parent.centroid()
        val cc = child.centroid()
        val vpx = mid.x - pc.x; val vpy = mid.y - pc.y
        val vcx = mid.x - cc.x; val vcy = mid.y - cc.y
        return (vpx * vcx + vpy * vcy) < 0f
    }

    /**
     * 既知の失敗ケース (2026-06-18 発見、別 commit で fix 予定)。
     * 直ったら下のチェックが落ちて「list から消せ」と教えてくれる。
     */
    private val knownFailures = setOf<String>()

    @Test
    fun child_must_expand_to_outside_of_parent_edge_for_all_combinations() {
        val failures = mutableMapOf<String, String>()  // case label -> detail
        for (parentKind in listOf("Triangle", "Rectangle")) {
            for (childKind in listOf("Triangle", "Rectangle")) {
                for (side in connectableSides(parentKind)) {
                    for (alignment in childAlignments(childKind)) {
                        val case = Case(parentKind, childKind, side, alignment)
                        val csv = parentCsv(parentKind) + "\n" + childCsv(parentKind, childKind, side, alignment)
                        val doc = CsvCodec.parse(csv + "\n")
                        val mixed = CsvCodec.buildAll(doc)
                        if (mixed.size() != 2) {
                            failures[case.toString()] = "build failed (size=${mixed.size()})"
                            continue
                        }
                        val parent = mixed.get(1)
                        val child  = mixed.get(2)
                        if (!expandsOutside(parent, side, child)) {
                            val pc = parent.centroid(); val cc = child.centroid()
                            failures[case.toString()] =
                                "child centroid (${cc.x},${cc.y}) is inside parent edge (parent centroid ${pc.x},${pc.y})"
                        }
                    }
                }
            }
        }
        // 新規 (= 既知リストに無い) 失敗は即 fail させる ── shadow が内向きに描かれる類の
        // 幾何バグを継続的に捕捉する gate。
        val unexpected = failures.filterKeys { it !in knownFailures }
        assertTrue(
            unexpected.isEmpty(),
            "unexpected expansion direction violations (${unexpected.size}):\n" +
                    unexpected.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        )
        // 既知失敗が静かに直った場合の検出 ── list から消し忘れを防ぐ。
        val staleKnown = knownFailures - failures.keys
        assertTrue(
            staleKnown.isEmpty(),
            "known failures were fixed; remove from knownFailures list: $staleKnown"
        )
    }
}
