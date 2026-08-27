package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer
import com.jpaver.trianglelist.label.ObstacleKind
import com.example.trilib.PointXY
import org.junit.Test
import java.io.File

/** アプリ画面と同じ経路 (MyView.setTriangleList → attachToTheView) を再現して効果を測る。 */
class AppViewPathProbe {
    @Test fun probe() {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        val csv = File(dir, "samples/8.25_bad.csv").readText(java.nio.charset.Charset.forName("MS932"))

        for (scale in listOf(1f, 5f, 10f, 47.6f)) {
            for (ts in listOf(30f, 12f, 5f)) {
                val list = CsvCodec.build(CsvCodec.parse(csv))
                // MyView.setTriangleList と同じ: clone → attachToTheView(0,0, scale, ts)
                val before = run {
                    val l2 = CsvCodec.build(CsvCodec.parse(csv))
                    l2.attachToTheView(PointXY(0f, 0f), scale, ts, isArrangePointNumbers = false)
                    ModelOverlapAnalyzer.analyze(l2, textSize = ts).collisionKindByText
                }
                list.attachToTheView(PointXY(0f, 0f), scale, ts)
                val after = ModelOverlapAnalyzer.analyze(list, textSize = ts).collisionKindByText
                println("[app] scale=$scale ts=$ts 前=%d(文字%d/円%d) 後=%d(文字%d/円%d) dimHeight=%.2f".format(
                    before.size, before.count { it.value == ObstacleKind.LABEL }, before.count { it.value == ObstacleKind.CIRCLE },
                    after.size, after.count { it.value == ObstacleKind.LABEL }, after.count { it.value == ObstacleKind.CIRCLE },
                    list.getBy(1).dimHeight))
            }
        }
    }
}
