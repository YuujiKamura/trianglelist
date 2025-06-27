import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CADView - KMP Desktop",
        state = WindowState(
            position = WindowPosition(300.dp, 200.dp),
            width = 800.dp,
            height = 600.dp
        )
    ) {
        CADViewWidget()
    }
} 