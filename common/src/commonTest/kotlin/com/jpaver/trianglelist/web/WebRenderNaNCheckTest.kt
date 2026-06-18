package com.jpaver.trianglelist.web

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * web の dev server 上で出ていた "5967,"x2":NaN,"y2":N" の NaN を common 側 (= 同じ
 * Kotlin path) で再現するか確認する。 ロジック側で NaN が無ければ wasm キャッシュ疑い。
 */
class WebRenderNaNCheckTest {
    private val csvs = listOf(
        "tri-rect-only" to "1,6.0,5.0,4.0,-1,-1\nRectangle,2,2.0,4.0,4.0,1,2,0\n",
        "tri-tri-rect"  to "1,6.0,5.0,4.0,-1,-1\n2,5.0,4.0,3.0,1,1\nRectangle,3,2.0,4.0,4.0,1,2,0\n",
        "rect-tri"      to "Rectangle,1,5,10,7,-1,0\n2,7,4,4,1,2\n",
        "rect-only"     to "Rectangle,1,5,10,7,-1,0\n",
    )

    @Test
    fun render_emits_no_NaN() {
        for ((name, csv) in csvs) {
            val json = WebPrimitiveRenderer.renderCsv(csv, 1f)
            assertFalse(json.contains("NaN"), "construct '$name' renderCsv NaN: ${json.take(400)}")
        }
    }

    @Test
    fun render_with_overrides_emits_no_NaN() {
        for ((name, csv) in csvs) {
            val json = WebPrimitiveRenderer.renderCsv(csv, 1f, "")
            assertFalse(json.contains("NaN"), "construct '$name' renderCsv(overrides=\"\") NaN: ${json.take(400)}")
        }
    }

    @Test
    fun frame_emits_no_NaN() {
        for ((name, csv) in csvs) {
            val json = WebFrame.renderFrame(csv)
            assertFalse(json.contains("NaN"), "construct '$name' renderFrame NaN: ${json.take(400)}")
        }
    }
}
