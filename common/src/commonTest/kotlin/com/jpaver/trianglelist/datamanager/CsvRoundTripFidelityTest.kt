package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.setColor
import com.jpaver.trianglelist.setDimAligns
import kotlin.test.Test
import kotlin.test.fail

/**
 * 保存 -> 復元で図面の状態が 1 つも落ちないことの総当たり gate。
 *
 * 2026-08-27 に立て続けに出た事故はすべて同じ形だった:
 * 「モデルには入っているが CSV 往復のどこかで落ちる」。
 * 個別に pin して回るのではなく、変えられる状態を軸として列挙し、その直積を
 * 1 件ずつ往復させて全項目を突き合わせる。守りたい状態を足したら軸か snapshot に
 * 1 行足せば、全組合せが自動的に張られる。
 *
 * 経路は Android の実シーケンスと同じ:
 *   parse -> build(applyRecoverState=false) -> applyListParams -> recoverState
 *   -> 変更 -> bake -> serialize -> (同じ経路で再ロード)
 */
class CsvRoundTripFidelityTest {

    // ---- 軸の列挙 --------------------------------------------------------
    /** 図形構成: 接続の種類 (単独 / 通常 B・C 接続 / フロート) */
    private val shapes = mapOf(
        "単独" to listOf("1,6.0,5.0,4.0,-1,-1"),
        "BC接続" to listOf("1,6.0,5.0,4.0,-1,-1", "2,5.0,4.0,3.0,1,1", "3,4.0,3.5,3.0,1,2"),
        "フロート" to listOf("1,6.0,5.0,4.0,-1,-1", "2,5.0,4.0,3.0,1,9", "3,4.0,3.5,3.0,2,1"),
    )
    private val angles = listOf(0f, 15f, -30f, 90f, 180f)
    private val scales = listOf(1f, 0.5f, 2f)

    /** 測点名: 無し / 半角 / 全角 (文字幅の分岐と CP932 の両方を踏む) */
    private val names = listOf("", "No.1", "測点イ")

    /** 寸法アライメント horizontal (a,b,c) */
    private val hAligns = listOf(Triple(0, 0, 0), Triple(1, 3, 4), Triple(2, 4, 3))
    private val movedNumbers = listOf(false, true)

    private data class Case(
        val shape: String,
        val angle: Float,
        val scale: Float,
        val name: String,
        val h: Triple<Int, Int, Int>,
        val moved: Boolean,
    ) {
        override fun toString() =
            shape + " angle=" + angle + " scale=" + scale + " name=" + name + " h=" + h + " moved=" + moved
    }

    private fun allCases(): List<Case> = buildList {
        for (s in shapes.keys)
            for (a in angles)
                for (sc in scales)
                    for (n in names)
                        for (h in hAligns)
                            for (m in movedNumbers)
                                add(Case(s, a, sc, n, h, m))
    }

    // ---- 経路 ------------------------------------------------------------
    private fun csvFor(shape: String): String = buildString {
        appendLine("koujiname, 市道○○号線 舗装打換工事")
        appendLine("rosenname, 市道○○号線")
        appendLine("gyousyaname, ○○建設株式会社")
        appendLine("zumennum, 1/1")
        shapes.getValue(shape).forEach { appendLine(it) }
    }

    private fun load(csv: String): TriangleList {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc, applyRecoverState = false)
        CsvCodec.applyListParams(doc, trilist) { }
        trilist.recoverState(PointXY(0f, 0f))
        // アプリは描画のたびに attachToTheView -> arrangePointNumbers で
        // 自動配置の番号サークルを幾何から計算し直す。手動移動されたものは
        // autoAlign が early return で保護する。テストも同じ状態で比べる。
        trilist.arrangePointNumbers()
        return trilist
    }

    private fun save(trilist: TriangleList, original: CsvCodec.CsvDoc): String =
        CsvCodec.serialize(
            CsvCodec.bake(
                trilist,
                DeductionList(),
                HeaderValues("市道○○号線 舗装打換工事", "市道○○号線", "○○建設株式会社", "1/1"),
                original,
                1f,
            )
        )

    /** 浮動小数の表示揺れを吸収 (1e-3 未満の差は同値とみなす) */
    private fun Double.r(): Long = (this * 1000.0).toLong()
    private fun Float.r(): Long = (this * 1000f).toLong()

    /** 突き合わせる状態を畳む。守りたい項目を足したらここに 1 行足す */
    private fun snapshot(t: TriangleList): String = buildString {
        appendLine("listAngle=" + t.angle.r() + " listScale=" + t.scale.r() + " size=" + t.size())
        for (i in 1..t.size()) {
            val tri = t.getBy(i)
            appendLine(
                "#" + i +
                    " name=" + tri.name +
                    " color=" + tri.mycolor +
                    " p0=" + tri.point[0].x.r() + "," + tri.point[0].y.r() +
                    " pAB=" + tri.pointAB.x.r() + "," + tri.pointAB.y.r() +
                    " pBC=" + tri.pointBC.x.r() + "," + tri.pointBC.y.r() +
                    " h=" + tri.dim.horizontal.a + tri.dim.horizontal.b +
                    tri.dim.horizontal.c + tri.dim.horizontal.s +
                    " v=" + tri.dim.vertical.a + tri.dim.vertical.b + tri.dim.vertical.c +
                    " pn=" + tri.pointnumber.x.r() + "," + tri.pointnumber.y.r() +
                    " pnMoved=" + tri.pointNumber.flag.isMovedByUser +
                    " conn=" + tri.connectionSide +
                    " parent=" + tri.parentnumber +
                    " len=" + tri.lengthA_.r() + "," + tri.lengthB_.r() + "," + tri.lengthC_.r()
            )
        }
    }

    private fun applyMutations(t: TriangleList, c: Case) {
        if (c.angle != 0f) t.rotate(PointXY(0f, 0f), c.angle, t.lastTapNumber, false)
        if (c.scale != 1f) t.setScale(PointXY(0f, 0f), c.scale)
        for (i in 1..t.size()) {
            val tri: Triangle = t.getBy(i)
            tri.name = if (c.name.isEmpty()) "" else c.name + "-" + i
            tri.setColor(3 + i)
            tri.setDimAligns(c.h.first, c.h.second, c.h.third, 1, 3, 1)
            if (c.moved) {
                tri.pointnumber = PointXY(1.5f * i, -2.5f * i)
                tri.pointNumber.flag.isMovedByUser = true
            }
        }
        // 変更後もアプリは再描画で自動配置を計算し直す
        t.arrangePointNumbers()
    }

    @Test
    fun 保存と復元で状態が落ちない() {
        val failures = mutableListOf<String>()
        val cases = allCases()
        for (c in cases) {
            val csv = csvFor(c.shape)
            val originalDoc = CsvCodec.parse(csv)
            val live = load(csv)
            applyMutations(live, c)

            val before = snapshot(live)
            val after = snapshot(load(save(live, originalDoc)))
            if (before != after) {
                val d = before.lines().zip(after.lines()).firstOrNull { (a, b) -> a != b }
                failures += c.toString() +
                    (if (d != null) "\n    保存前: " + d.first + "\n    復元後: " + d.second else "")
            }
        }
        if (failures.isNotEmpty()) fail(
            "往復で状態が落ちるケース " + failures.size + "/" + cases.size + " 件:\n" +
                failures.take(10).joinToString("\n")
        )
    }

    @Test
    fun 二度往復しても状態が動かない() {
        // 1 回目で正規化されて 2 回目以降ズレる、という形の劣化を捕まえる
        val failures = mutableListOf<String>()
        for (c in allCases()) {
            val csv = csvFor(c.shape)
            val live = load(csv)
            applyMutations(live, c)
            val once = save(live, CsvCodec.parse(csv))
            val onceLoaded = load(once)
            val twice = save(onceLoaded, CsvCodec.parse(once))
            if (snapshot(onceLoaded) != snapshot(load(twice))) failures += c.toString()
        }
        if (failures.isNotEmpty()) {
            fail("2 回目の往復で動くケース " + failures.size + " 件:\n" + failures.take(10).joinToString("\n"))
        }
    }

    @Test
    fun NaNが一切出ない() {
        val failures = mutableListOf<String>()
        for (c in allCases()) {
            val csv = csvFor(c.shape)
            val live = load(csv)
            applyMutations(live, c)
            val text = save(live, CsvCodec.parse(csv))
            if (text.contains("NaN", ignoreCase = true)) failures += c.toString() + ": 保存 CSV に NaN"
            val reloaded = load(text)
            for (i in 1..reloaded.size()) {
                val tri = reloaded.getBy(i)
                val bad = listOf(tri.point[0], tri.pointAB, tri.pointBC, tri.pointnumber)
                    .any { it.x.isNaN() || it.y.isNaN() }
                if (bad) failures += c.toString() + ": 復元後 #" + i + " が NaN"
            }
        }
        if (failures.isNotEmpty()) {
            fail("NaN が出るケース " + failures.size + " 件:\n" + failures.take(10).joinToString("\n"))
        }
    }
}
