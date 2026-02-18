package com.jpaver.trianglelist.cadview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.jpaver.trianglelist.dxf.DxfParseResult

/**
 * AWT Graphics2D ベースの DXF ビューワー (Compose SwingPanel ラッパー)
 */
@Composable
fun CADViewAwt(parseResult: DxfParseResult?, modifier: Modifier = Modifier) {
    SwingPanel(
        modifier = modifier,
        factory = { AwtCadPanel().apply { setParseResult(parseResult) } },
        update = { panel ->
            panel.setParseResult(parseResult)
            panel.repaint()
        }
    )
}
