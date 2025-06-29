import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "TriangleList Desktop") {
        // ここにTriLibの共通UIを配置する
    }
} 