import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.awt.FileDialog
import java.awt.Frame
import com.jpaver.cadview.CADViewWidget

fun main() = application {
    var dxfText by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var textScale by remember { mutableStateOf(1f) } // WebApp版のtextscale_機能を移植
    var isDebugMode by remember { mutableStateOf(false) } // WebApp版から移植：デバッグモード

    Window(
        onCloseRequest = ::exitApplication,
        title = "CADView - KMP Desktop",
        state = WindowState(
            position = WindowPosition(300.dp, 200.dp),
            width = 1200.dp,
            height = 1000.dp
        )
    ) {
        CADViewWidget(
            dxfText = dxfText,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            textScale = textScale, // テキストスケール追加
            isDebugMode = isDebugMode, // WebApp版から移植：デバッグモード
            onOpenFile = {
                val dialog = FileDialog(null as Frame?, "DXFファイルを選択", FileDialog.LOAD)
                dialog.isVisible = true
                val file = dialog.files.firstOrNull()
                if (file != null) {
                    dxfText = file.readText()
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                }
            },
            onZoomIn = { scale *= 1.2f },
            onZoomOut = { scale /= 1.2f },
            onReset = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
                textScale = 1f // テキストスケールもリセット
            },
            onTextSizeUp = { textScale *= 1.2f }, // WebApp版のテキストサイズ変更機能を移植
            onTextSizeDown = { textScale /= 1.2f },
            onToggleDebug = { isDebugMode = !isDebugMode } // WebApp版から移植：デバッグモード切り替え
            }
        )
    }
} 