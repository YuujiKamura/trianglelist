package com.jpaver.trianglelist.regression.eightmillion

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 【エイトミリオンウェイストゥダイ：高負荷・耐久ストレステスト (Endurance Tier)】
 *
 * 通常の短時間ビルドを妨げないよう分離・整理された高深度ストレステスト。
 * 500段の深層チェーン、500ステップの過激ファジングなど、
 * 極限の負荷でメモリリークやスタックオーバーフロー、累積破綻が起きないことを保証する。
 *
 * 実行方法:
 *   ./gradlew :common:desktopTest --tests "com.jpaver.trianglelist.regression.eightmillion.EightMillionEnduranceTest"
 */
class EightMillionEnduranceTest {

    @Test
    fun `Endurance_01_深層チェーン_500個の連続接続でもスタックオーバーフローや破綻が起きない`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))

        // 500個の三角形を連続チェーン
        for (i in 1..500) {
            val last = list.getBy(list.size())
            val b = last.lengthB_
            // 安定した相似比で接続
            val child = Triangle(b, b * 0.9f, b * 0.8f)
            child.parentnumber = last.mynumber
            child.connectionSide = 1
            list.add(child, true)
        }

        assertEquals(501, list.size())

        // 末尾の座標が NaN でないこと
        val lastTri = list.getBy(501)
        assertFalse(lastTri.pointBC.x.isNaN() || lastTri.pointBC.y.isNaN(), "Depth 500 coordinate must not be NaN")
        assertFalse(lastTri.pointBC.x.isInfinite() || lastTri.pointBC.y.isInfinite(), "Depth 500 coordinate must not be Infinite")

        // 500個のツリーをシリアライズ＆デシリアライズ
        val emptyDoc = CsvCodec.CsvDoc(emptyList(), null, emptyList())
        val baked = CsvCodec.bake(list, emptyDoc)
        val text = CsvCodec.serialize(baked)
        val parsed = CsvCodec.parse(text)
        val reloaded = CsvCodec.build(parsed, applyRecoverState = false)

        assertEquals(501, reloaded.size())
    }

    @Test
    fun `Endurance_02_過激ファジング_200ステップのデタラメ破壊操作シーケンス`() {
        val random = Random(999)
        var list = TriangleList(Triangle(6f, 5f, 4f))

        for (step in 1..200) {
            when (random.nextInt(6)) {
                0, 1 -> {
                    // 追加
                    val parent = list.getBy(list.size())
                    val a = parent.lengthB_.coerceIn(0.1f, 1000f)
                    val b = (a * 0.8f).coerceIn(0.1f, 1000f)
                    val c = (a * 0.7f).coerceIn(0.1f, 1000f)
                    val child = Triangle(a, b, c)
                    child.parentnumber = parent.mynumber
                    child.connectionSide = if (random.nextBoolean()) 1 else 2
                    list.add(child, true)
                }
                2 -> {
                    // 削除
                    if (list.size() > 1) {
                        list.remove(list.size())
                    }
                }
                3 -> {
                    // 微小回転
                    list.rotate(PointXY(0.0, 0.0), random.nextFloat() * 10f - 5f, 0, false)
                }
                4 -> {
                    // 保存と再構築
                    val emptyDoc = CsvCodec.CsvDoc(emptyList(), null, emptyList())
                    val baked = CsvCodec.bake(list, emptyDoc)
                    val text = CsvCodec.serialize(baked)
                    val parsed = CsvCodec.parse(text)
                    list = CsvCodec.build(parsed, applyRecoverState = false)
                }
                5 -> {
                    // ランダムな中間要素の辺長読み取り
                    val randIdx = random.nextInt(1, list.size() + 1)
                    val t = list.getBy(randIdx)
                    assertFalse(t.pointBC.x.isNaN())
                }
            }
        }

        assertTrue(list.size() >= 1)
        assertFalse(list.getBy(1).getArea().isNaN())
    }
}
