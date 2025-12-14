import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jpaver.trianglelist.cadview.CADView
import com.jpaver.trianglelist.cadview.ViewStateManager
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.dxf.CrosswalkGenerator
import com.jpaver.trianglelist.dxf.DxfText
import com.jpaver.trianglelist.dxf.MarkingCommand
import com.jpaver.trianglelist.dxf.MarkingCommandExecutor
import com.jpaver.trianglelist.test.TextGeometryTestWidget
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.io.File
import kotlinx.coroutines.delay

fun main(args: Array<String>) = application {
    // コマンドライン引数を処理
    val isTestMode = args.contains("--test") || args.contains("-t")
    val isDebugMode = args.contains("--debug") || args.contains("-d")
    // DXFファイルパスを取得（オプション以外の引数）
    val dxfFilePath = args.firstOrNull { !it.startsWith("-") && it.endsWith(".dxf", ignoreCase = true) }

    // デスクトップサイズを取得して左下1/4に配置
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowWidth = screenSize.width / 2
    val windowHeight = screenSize.height / 2
    val windowX = 0
    val windowY = screenSize.height / 2

    val windowState = rememberWindowState(
        size = DpSize(windowWidth.dp, windowHeight.dp),
        position = WindowPosition(windowX.dp, windowY.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = if (isTestMode) "テキストジオメトリテスト" else "CAD Viewer",
        state = windowState
    ) {
        if (isTestMode) {
            // テストモード
            TextGeometryTestWidget()
        } else {
            // 通常のCADビューアモード
            CADViewerApp(initialFilePath = dxfFilePath, initialDebugMode = isDebugMode)
        }
    }
}

@Composable
private fun CADViewerApp(initialFilePath: String? = null, initialDebugMode: Boolean = false) {
    var parseResult by remember { mutableStateOf<DxfParseResult?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(initialDebugMode) }
    var hotReload by remember { mutableStateOf(true) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var lastModified by remember { mutableStateOf(0L) }

    // ビューステート管理
    val viewStateManager = remember { ViewStateManager() }
    var initialScale by remember { mutableStateOf<Float?>(null) }
    var initialOffset by remember { mutableStateOf<Offset?>(null) }

    // 現在のビューステート（保存用）
    var currentScale by remember { mutableStateOf<Float?>(null) }
    var currentOffset by remember { mutableStateOf<Offset?>(null) }

    // 現在のステートを保存する関数
    fun saveCurrentViewState() {
        currentFile?.let { file ->
            currentScale?.let { scale ->
                currentOffset?.let { offset ->
                    viewStateManager.saveViewState(file.absolutePath, scale, offset)
                }
            }
        }
    }

    // コンポーネント破棄時に保存
    DisposableEffect(Unit) {
        onDispose {
            saveCurrentViewState()
        }
    }

    fun loadDxfFile(file: File): DxfParseResult? {
        return try {
            val dxfContent = file.readText()
            val parser = DxfParser()
            val result = parser.parse(dxfContent)

            println("=== DXF Loaded: ${file.name} (${file.absolutePath}) ===")
            println("Lines: ${result.lines.size}, Circles: ${result.circles.size}, Arcs: ${result.arcs.size}, Polylines: ${result.lwPolylines.size}, Texts: ${result.texts.size}")

            result.lwPolylines.forEachIndexed { idx, poly ->
                val xs = poly.vertices.map { it.first }
                val ys = poly.vertices.map { it.second }
                val minX = xs.minOrNull() ?: 0.0
                val maxX = xs.maxOrNull() ?: 0.0
                val minY = ys.minOrNull() ?: 0.0
                val maxY = ys.maxOrNull() ?: 0.0
                println("  Polyline[$idx]: vertices=${poly.vertices.size}, bounds=(${minX.toInt()},${minY.toInt()})-(${maxX.toInt()},${maxY.toInt()}), size=${(maxX-minX).toInt()}x${(maxY-minY).toInt()}")
            }

            result
        } catch (e: Exception) {
            println("Error loading DXF file: ${e.message}")
            null
        }
    }

    LaunchedEffect(initialFilePath) {
        // コマンドライン引数 or 最後に開いたファイル
        val pathToOpen = initialFilePath ?: viewStateManager.getLastOpenedFile()

        pathToOpen?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // 保存されたビューステートを読み込む
                viewStateManager.loadViewState(path)?.let { (scale, offset) ->
                    initialScale = scale
                    initialOffset = offset
                }

                loadDxfFile(file)?.let { result ->
                    parseResult = result
                    currentFile = file
                    lastModified = file.lastModified()
                    // 最後に開いたファイルを保存
                    viewStateManager.saveLastOpenedFile(file.absolutePath)
                }
            } else {
                println("File not found: $path")
            }
        }
    }

    LaunchedEffect(currentFile, hotReload) {
        if (currentFile != null && hotReload) {
            while (true) {
                delay(1000)
                currentFile?.let { file ->
                    if (file.exists()) {
                        val newModified = file.lastModified()
                        if (newModified > lastModified) {
                            println("File changed, reloading...")
                            loadDxfFile(file)?.let { result ->
                                parseResult = result
                                lastModified = newModified
                            }
                        }
                    }
                }
            }
        }
    }

    // コマンドファイル監視（エージェントからの区画線追加用）
    val commandExecutor = remember { MarkingCommandExecutor() }
    var commandFileModified by remember { mutableStateOf(0L) }

    LaunchedEffect(currentFile) {
        if (currentFile != null) {
            val commandFile = File(currentFile!!.parent, ".cadview_commands.json")
            println("Watching command file: ${commandFile.absolutePath}")

            while (true) {
                delay(500)  // 0.5秒間隔で監視
                if (commandFile.exists()) {
                    val newModified = commandFile.lastModified()
                    if (newModified > commandFileModified) {
                        commandFileModified = newModified
                        try {
                            val content = commandFile.readText()
                            if (content.isNotBlank()) {
                                println("Command file detected: $content")
                                val commands = MarkingCommand.listFromJson(content)

                                parseResult?.let { currentResult ->
                                    var updatedResult = currentResult
                                    for (cmd in commands) {
                                        val result = commandExecutor.execute(cmd, updatedResult, currentFile?.absolutePath)
                                        println("Command executed: ${result.message}")
                                        updatedResult = updatedResult.copy(
                                            lines = updatedResult.lines + result.lines,
                                            texts = updatedResult.texts + result.texts
                                        )
                                    }
                                    parseResult = updatedResult
                                }

                                // コマンド実行後、ファイルを削除
                                commandFile.delete()
                                println("Command file processed and deleted")
                            }
                        } catch (e: Exception) {
                            println("Error processing command file: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        LaunchedEffect(Unit) {
            val frame = Frame()
            val dialog = FileDialog(frame, "DXFファイルを選択", FileDialog.LOAD)
            dialog.isVisible = true

            dialog.file?.let { fileName ->
                val selectedFile = File(dialog.directory, fileName)

                // 現在のファイルのステートを保存
                saveCurrentViewState()

                // 保存されたビューステートを読み込む
                viewStateManager.loadViewState(selectedFile.absolutePath)?.let { (scale, offset) ->
                    initialScale = scale
                    initialOffset = offset
                } ?: run {
                    // 保存されていなければリセット
                    initialScale = null
                    initialOffset = null
                }

                loadDxfFile(selectedFile)?.let { result ->
                    parseResult = result
                    currentFile = selectedFile
                    lastModified = selectedFile.lastModified()
                    // 最後に開いたファイルを保存
                    viewStateManager.saveLastOpenedFile(selectedFile.absolutePath)
                }
            }
            dialog.dispose()
            frame.dispose()
            showDialog = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { showDialog = true }) {
                Text("DXFファイルを開く")
            }

            Button(
                onClick = { debugMode = !debugMode },
                colors = if (debugMode) {
                    ButtonDefaults.buttonColors(backgroundColor = androidx.compose.ui.graphics.Color.Red)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (debugMode) "デバッグOFF" else "デバッグON")
            }

            Button(
                onClick = { hotReload = !hotReload },
                colors = if (hotReload) {
                    ButtonDefaults.buttonColors(backgroundColor = androidx.compose.ui.graphics.Color.Green)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (hotReload) "自動更新ON" else "自動更新OFF")
            }
        }

        currentFile?.let { file ->
            Text(
                text = file.name,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.caption
            )
        }

        parseResult?.let { result ->
            CADView(
                parseResult = result,
                debugMode = debugMode,
                initialScale = initialScale,
                initialOffset = initialOffset,
                onViewStateChanged = { scale, offset ->
                    currentScale = scale
                    currentOffset = offset
                }
            )
        }
    }
}
