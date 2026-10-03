package com.jpaver.trianglelist.editmodel

import com.example.trilib.PointXY
import kotlin.math.abs
import kotlin.math.max

/**
 * 三角形リスト（展開図）の幾何学的完全性と接続不変条件を検証する機構。
 *
 * 【検証項目】
 * 1. 【当たり判定】三角形同士の重複・交差・侵入の検出
 * 2. 【基線一致判定】親子の共有辺の長さ・端点座標・外向き展開の一致検証
 * 3. 【幾何健全性】辺長正値性・三角不等式・非退化・非NaN/非Infinite
 * 4. 【トポロジ整合性】親番号の妥当性・循環参照の排除
 */
object GeometryIntegrityValidator {

    const val TOLERANCE_BASELINE_MM = 0.05
    const val TOLERANCE_COORDINATE_MM = 0.05

    enum class Severity {
        ERROR,
        WARNING
    }

    enum class IssueType {
        OVERLAP,
        INWARD_FOLD,
        BASELINE_LENGTH_MISMATCH,
        BASELINE_GAP,
        DEGENERATE_SHAPE,
        INVALID_COORDINATES,
        INVALID_PARENT,
        TOPOLOGY_CYCLE
    }

    data class IntegrityIssue(
        val severity: Severity,
        val type: IssueType,
        val triangleNumber: Int,
        val relatedTriangleNumber: Int? = null,
        val message: String
    ) {
        override fun toString(): String {
            val related = if (relatedTriangleNumber != null) " (vs #$relatedTriangleNumber)" else ""
            return "[$severity][$type] #$triangleNumber$related: $message"
        }
    }

    /**
     * TriangleList 全体の幾何・接続整合性を一括検証する。
     */
    fun validateList(list: TriangleList): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        val size = list.size()
        if (size == 0) return issues

        // 1. 各三角形の幾何健全性と接続（基線一致）の個別検証
        for (i in 1..size) {
            val t = list.getBy(i)
            issues.addAll(validateIndividualShape(t, list))
        }

        // 2. 三角形ペア間の総当たり当たり判定（衝突・重複検出）
        // 土木測量展開図では地形や作業途中により重なりが意図的・実測上に生じ得るため WARNING とする
        for (i in 1..size) {
            val t1 = list.getBy(i)
            for (j in (i + 1)..size) {
                val t2 = list.getBy(j)
                if (TriangleCollisionDetector.doShapesOverlap(t1, t2)) {
                    issues.add(
                        IntegrityIssue(
                            severity = Severity.WARNING,
                            type = IssueType.OVERLAP,
                            triangleNumber = t1.mynumber,
                            relatedTriangleNumber = t2.mynumber,
                            message = "Triangle #${t1.mynumber} and Triangle #${t2.mynumber} overlap or penetrate each other."
                        )
                    )
                }
            }
        }

        return issues
    }

    /**
     * 単一の図形とその親との接続関係を検証する。
     */
    fun validateIndividualShape(t: Triangle, list: TriangleList): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        val num = t.mynumber

        // 1. 座標値の健全性 (NaN / Infinite)
        val vertices = listOf(t.point[0], t.pointAB, t.pointBC)
        for (v in vertices) {
            if (v.x.isNaN() || v.y.isNaN() || v.x.isInfinite() || v.y.isInfinite()) {
                issues.add(
                    IntegrityIssue(
                        severity = Severity.ERROR,
                        type = IssueType.INVALID_COORDINATES,
                        triangleNumber = num,
                        message = "Triangle #$num has invalid coordinates: (${v.x}, ${v.y})"
                    )
                )
                return issues
            }
        }

        // 2. 辺長と三角不等式
        val a = t.lengthA_
        val b = t.lengthB_
        val c = t.lengthC_
        if (a <= 0f || b <= 0f || c <= 0f) {
            issues.add(
                IntegrityIssue(
                    severity = Severity.ERROR,
                    type = IssueType.DEGENERATE_SHAPE,
                    triangleNumber = num,
                    message = "Triangle #$num has non-positive sides: A=$a, B=$b, C=$c"
                )
            )
            return issues
        }

        if (a + b <= c || b + c <= a || c + a <= b) {
            issues.add(
                IntegrityIssue(
                    severity = Severity.ERROR,
                    type = IssueType.DEGENERATE_SHAPE,
                    triangleNumber = num,
                    message = "Triangle #$num violates triangle inequality: A=$a, B=$b, C=$c"
                )
            )
            return issues
        }

        // 3. 親子接続の基線一致判定
        if (num == 1) {
            // ルート図形
            if (t.parentnumber > 0) {
                issues.add(
                    IntegrityIssue(
                        severity = Severity.WARNING,
                        type = IssueType.INVALID_PARENT,
                        triangleNumber = num,
                        message = "Root triangle #1 has parentnumber=${t.parentnumber} (expected <= 0)"
                    )
                )
            }
        } else {
            val pn = t.parentnumber
            if (pn <= 0 || pn >= num) {
                issues.add(
                    IntegrityIssue(
                        severity = Severity.ERROR,
                        type = IssueType.TOPOLOGY_CYCLE,
                        triangleNumber = num,
                        relatedTriangleNumber = pn,
                        message = "Triangle #$num has invalid parentnumber=$pn (must be in 1..${num - 1})"
                    )
                )
                return issues
            }

            if (pn in 1..list.size()) {
                val parent = list.getBy(pn)
                issues.addAll(validateBaselineConnection(t, parent))
            }
        }

        return issues
    }

    /**
     * 【基線一致判定】
     * 子三角形 t が親図形 parent の接続辺に対して幾何学的に正しく合致しているか判定する。
     *
     * 接続種別 (ConnectionSide):
     * - 通常直接接続: B(1), C(2)
     * - 二重断面接続: BR(3), BL(4), CR(5), CL(6), BC(7), CC(8)
     * - フロート接続: FB(9), FC(10)
     */
    fun validateBaselineConnection(child: Triangle, parent: Triangle): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        val side = child.connectionSide
        val cNum = child.mynumber
        val pNum = parent.mynumber

        if (side !in 1..10) {
            issues.add(
                IntegrityIssue(
                    severity = Severity.ERROR,
                    type = IssueType.INVALID_PARENT,
                    triangleNumber = cNum,
                    relatedTriangleNumber = pNum,
                    message = "Invalid connection side $side on parent #$pNum (must be 1..10)"
                )
            )
            return issues
        }

        // 二重断面 (codes 3..8 または connectionType_ == 2)
        // フロート接続 (codes 9..10 または connectionType_ == 1)
        val isFloat = side in 9..10 || child.connectionType_ == 1
        val isDoubleSection = side in 3..8 || child.connectionType_ == 2
        val isNormalDirect = !isFloat && !isDoubleSection

        // 通常接続 (B=1, C=2 かつ connectionType_ == 0) の場合のみ、A辺長と親接続辺の厳密一致を検証する
        if (isNormalDirect) {
            val parentSide = side
            val parentSideLen = parent.length[parentSide]
            val childSideALen = child.lengthA_
            val diffLen = abs(childSideALen - parentSideLen)

            if (diffLen > TOLERANCE_BASELINE_MM) {
                issues.add(
                    IntegrityIssue(
                        severity = Severity.ERROR,
                        type = IssueType.BASELINE_LENGTH_MISMATCH,
                        triangleNumber = cNum,
                        relatedTriangleNumber = pNum,
                        message = "Baseline length mismatch: child #$cNum side A ($childSideALen) != parent #$pNum side $parentSide ($parentSideLen), diff=$diffLen"
                    )
                )
            }

            // 基線端点の座標一致判定 (時計回り展開: 親の終端 -> 始端)
            val parentLine = parent.getLine(parentSide)
            val expectedStart = parentLine.right
            val expectedEnd = parentLine.left

            val actualStart = child.point[0]
            val actualEnd = child.pointAB

            val distStart = actualStart.lengthTo(expectedStart)
            val distEnd = actualEnd.lengthTo(expectedEnd)

            if (distStart > TOLERANCE_COORDINATE_MM || distEnd > TOLERANCE_COORDINATE_MM) {
                issues.add(
                    IntegrityIssue(
                        severity = Severity.ERROR,
                        type = IssueType.BASELINE_GAP,
                        triangleNumber = cNum,
                        relatedTriangleNumber = pNum,
                        message = "Baseline gap detected: child #$cNum is detached from parent #$pNum edge $parentSide (startGap=$distStart, endGap=$distEnd)"
                    )
                )
            }

            // 内向き折り返しの判定（重なり警告）
            if (TriangleCollisionDetector.isChildFoldingInward(child, parent)) {
                issues.add(
                    IntegrityIssue(
                        severity = Severity.WARNING,
                        type = IssueType.INWARD_FOLD,
                        triangleNumber = cNum,
                        relatedTriangleNumber = pNum,
                        message = "Child #$cNum folds inward onto parent #$pNum interior"
                    )
                )
            }
        }

        return issues
    }

    /**
     * 【モデル層ガード】
     * 新たに candidate 三角形をリストに追加可能か事前検査する。
     * 追加を拒絶すべき理由があればエラーのリストを返す（空なら安全に追加可能）。
     */
    fun checkCanAdd(
        list: TriangleList,
        candidate: Triangle,
        checkOverlap: Boolean = true
    ): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        val nextNumber = candidate.mynumber.takeIf { it > 0 } ?: (list.size() + 1)

        // 1. 候補三角形自身の健全性チェック
        val a = candidate.lengthA_
        val b = candidate.lengthB_
        val c = candidate.lengthC_
        if (a <= 0f || b <= 0f || c <= 0f || a + b <= c || b + c <= a || c + a <= b) {
            issues.add(
                IntegrityIssue(
                    severity = Severity.ERROR,
                    type = IssueType.DEGENERATE_SHAPE,
                    triangleNumber = nextNumber,
                    message = "Candidate triangle #$nextNumber has invalid side lengths: A=$a, B=$b, C=$c"
                )
            )
            return issues
        }

        // 2. 親が存在する場合の基線一致判定
        val pn = candidate.parentnumber
        if (pn in 1..list.size()) {
            val parent = list.getBy(pn)
            issues.addAll(validateBaselineConnection(candidate, parent))
        }

        // 3. 既存の全三角形との当たり判定（衝突チェック）
        // 測量展開図の意図的重なりや作業途中の重なりを拒絶してしまわないよう WARNING とする
        if (checkOverlap) {
            for (i in 1..list.size()) {
                val existing = list.getBy(i)
                if (TriangleCollisionDetector.doShapesOverlap(candidate, existing)) {
                    issues.add(
                        IntegrityIssue(
                            severity = Severity.WARNING,
                            type = IssueType.OVERLAP,
                            triangleNumber = nextNumber,
                            relatedTriangleNumber = existing.mynumber,
                            message = "Candidate triangle #$nextNumber collides with existing triangle #${existing.mynumber}"
                        )
                    )
                }
            }
        }

        return issues
    }
}
