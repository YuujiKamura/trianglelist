package com.jpaver.trianglelist.cadview

import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.web.WebDrawingExport
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** 測点名テキストが原点に落ちる様子を、実ビューワー (AwtCadPanel) の絵で残す診断用プローブ。 */
class SokutenOriginRenderProbe {

    init { System.setProperty("java.awt.headless", "true") }

    private val csv = """
        市道○○号線 舗装打換工事
        市道○○号線
        ○○建設株式会社
        1/1
        1,6.0,5.0,4.0,-1,-1,No.1
        2,5.0,4.0,3.0,1,1,No.2
        3,4.0,3.5,3.0,1,2,No.3
    """.trimIndent() + "\n"

    @Test
    fun render() {
        val dxf = WebDrawingExport.buildDxfText(csv)
        val parsed = DxfParser().parse(dxf).let { r ->
            r.copy(texts = r.texts.filter { it.text.isNotEmpty() })
        }
        println("sokuten texts: " + parsed.texts.filter { it.text.startsWith("No.") })
        dump(parsed, "sokuten-origin-asis.png")
        val cleaned = parsed.copy(texts = parsed.texts.filter { !it.x.isNaN() && !it.y.isNaN() })
        dump(cleaned, "sokuten-origin-nan-dropped.png")
        val zeroed = parsed.copy(texts = parsed.texts.map {
            if (it.x.isNaN() || it.y.isNaN()) it.copy(x = 0.0, y = 0.0) else it
        })
        dump(zeroed, "sokuten-origin-as-zero.png")
    }

    private fun dump(r: com.jpaver.trianglelist.dxf.DxfParseResult, name: String) {
        val panel = AwtCadPanel()
        panel.setBounds(0, 0, 1600, 1100)
        panel.setParseResult(r)
        val image = BufferedImage(1600, 1100, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try { panel.paint(g) } finally { g.dispose() }
        val out = File("build/probe/" + name)
        out.parentFile.mkdirs()
        ImageIO.write(image, "png", out)
        println("PROBE_PNG=" + out.absolutePath)
    }
}
