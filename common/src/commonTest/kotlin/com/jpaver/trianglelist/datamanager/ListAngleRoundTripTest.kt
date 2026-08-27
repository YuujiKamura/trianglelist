package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.fail

/**
 * リスト回転 (ListAngle) が CSV 往復で保存されることの gate。
 *
 * 2026-08-27 実機報告「図面をシェアして保存した後に、図形の回転状態がリセットされる」。
 * 共有チューザーから戻ると MainActivity.onResume:654 -> resumeCSV:2933 -> parseCSV:2884 が
 * private CSV を読み直してモデルを作り直す (setEditLists:2911)。つまり回転は
 * 「画面の状態」ではなく「CSV 往復で保存されるか」の問題になる。
 *
 * Android の実シーケンスをそのまま写す:
 *   初回ロード: parse -> build(applyRecoverState=false) -> applyListParams -> recoverState
 *   回転:       trilist.rotate(origin, deg, lastTapNumber, bSeparateFreeMode)
 *   保存:       bake -> serialize
 *   復帰:       parse -> build(applyRecoverState=false) -> applyListParams -> recoverState
 */
class ListAngleRoundTripTest {

    private val baseCsv = """
        koujiname, 市道○○号線 舗装打換工事
        rosenname, 市道○○号線
        gyousyaname, ○○建設株式会社
        zumennum, 1/1
        1,6.0,5.0,4.0,-1,-1,No.1
        2,5.0,4.0,3.0,1,1,No.2
        3,4.0,3.5,3.0,1,2,No.3
    """.trimIndent() + "\n"

    /** フロート接続 (connectionSide 9=BF / 10=CF) 入り。rotate の分岐条件が connectionSide>=9 */
    private val floatCsv = """
        koujiname, 市道○○号線 舗装打換工事
        rosenname, 市道○○号線
        gyousyaname, ○○建設株式会社
        zumennum, 1/1
        1,6.0,5.0,4.0,-1,-1,No.1
        2,5.0,4.0,3.0,1,9,No.2
        3,4.0,3.5,3.0,2,1,No.3
    """.trimIndent() + "\n"

    private fun load(csv: String): TriangleList {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc, applyRecoverState = false)
        CsvCodec.applyListParams(doc, trilist) { }
        trilist.recoverState(PointXY(0f, 0f))
        return trilist
    }

    /** writeCSV (MainActivity:2784) と同じ焼き方 */
    private fun save(trilist: TriangleList, original: CsvCodec.CsvDoc): String =
        CsvCodec.serialize(
            CsvCodec.bake(
                trilist = trilist,
                dedlist = DeductionList(),
                header = HeaderValues("市道○○号線 舗装打換工事", "市道○○号線", "○○建設株式会社", "1/1"),
                original = original,
                viewscale = 1f,
            )
        )

    private fun vertices(t: TriangleList): List<PointXY> =
        (1..t.size()).flatMap { i ->
            val tri = t.getBy(i)
            listOf(tri.point[0], tri.pointAB, tri.pointBC)
        }

    @Test
    fun `回転した図形は CSV 往復後も同じ向きのまま`() {
        val failures = mutableListOf<String>()

        // 軸の列挙:
        //  fixture       = 通常接続 / フロート接続あり
        //  separateFree  = fabRotate の実引数は true (MainActivity:1434,1438)。false も押さえる
        //  lastTapNumber = 未タップ 0 / 各三角形 1..3 (rotate の startnumber)
        //  deg           = 正負 + 90 度
        val fixtures = mapOf("通常" to baseCsv, "フロート" to floatCsv)
        for ((fxName, fx) in fixtures)
            for (separateFree in listOf(true, false))
                for (lastTap in listOf(0, 1, 2, 3))
                    for (deg in listOf(15f, -30f, 90f)) {
                        val label = "$fxName sepFree=$separateFree lastTap=$lastTap deg=$deg"
                        val originalDoc = CsvCodec.parse(fx)
                        val live = load(fx)
                        live.lastTapNumber = lastTap

                        live.rotate(PointXY(0f, 0f), deg, live.lastTapNumber, separateFree)
                        val afterRotate = vertices(live)
                        val angleAfterRotate = live.angle

                        val resumed = load(save(live, originalDoc))

                        if (abs(resumed.angle - angleAfterRotate) > 1e-3) {
                            failures += "$label: ListAngle が $angleAfterRotate -> ${resumed.angle} に変わった"
                            continue
                        }
                        val after = vertices(resumed)
                        val worst = afterRotate.zip(after).maxOfOrNull { (a, b) ->
                            maxOf(abs(a.x - b.x), abs(a.y - b.y))
                        } ?: 0.0
                        if (worst > 1e-3) {
                            failures += "$label: 復帰後の頂点が最大 $worst ずれた"
                        }
                    }

        if (failures.isNotEmpty()) {
            fail("回転が往復で失われるケース ${failures.size} 件:\n" + failures.joinToString("\n"))
        }
    }
}
