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
import com.jpaver.trianglelist.cadview.CADViewAwt
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
    val useAwtViewer = args.contains("--viewer=awt")
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
            // window (= ComposeWindow / extends JFrame) を CP の capture コマンドに渡す。
            // contentPane を BufferedImage に paintAll するための AWT 参照。
            CADViewerApp(initialFilePath = dxfFilePath, initialDebugMode = isDebugMode, useAwtViewer = useAwtViewer, awtWindow = window)
        }
    }
}

@Composable
private fun CADViewerApp(initialFilePath: String? = null, initialDebugMode: Boolean = false, useAwtViewer: Boolean = false, awtWindow: java.awt.Window? = null) {
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
            // trianglelist 出力の DXF は $DWGCODEPAGE=ANSI_932 (DxfHeader) 固定。
            // UTF-8 default で読むと日本語が壊れる。
            val dxfContent = file.readText(java.nio.charset.Charset.forName("MS932"))
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

    // CP (control plane): localhost:9876 で TCP listen し、
    // 「open <path>」の 1 行を受信したら viewer 再起動なしで DXF を差し替える。
    // CLI 側は desktop/scripts/cad-open.ps1 で送信する。
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val server = java.net.ServerSocket(9876, 50, java.net.InetAddress.getByName("127.0.0.1"))
                println("CP listening on 127.0.0.1:9876")
                while (true) {
                    val socket = server.accept()
                    try {
                        val line = socket.getInputStream().bufferedReader().readLine()?.trim()
                        if (line == null) {
                            socket.close()
                            continue
                        }
                        println("CP recv: $line")
                        val out = socket.getOutputStream()
                        when {
                            line.startsWith("open ") -> {
                                val path = line.removePrefix("open ").trim()
                                val target = File(path)
                                if (target.exists()) {
                                    run {
                                        loadDxfFile(target)?.let { result ->
                                            parseResult = result
                                            currentFile = target
                                            lastModified = target.lastModified()
                                            // 別ファイルを開く ── 保存された view state を復元、
                                            // 無ければ null にして CADView の fit を再計算させる。
                                            // これをしないと前ファイルの scale/offset が残って枠が画面外にはみ出る。
                                            val saved = viewStateManager.loadViewState(target.absolutePath)
                                            initialScale = saved?.first
                                            initialOffset = saved?.second
                                            currentScale = saved?.first
                                            currentOffset = saved?.second
                                            viewStateManager.saveLastOpenedFile(target.absolutePath)
                                        }
                                    }
                                    out.write("ok\n".toByteArray())
                                } else {
                                    out.write("error: file not found\n".toByteArray())
                                }
                            }
                            line.startsWith("zoom ") -> {
                                // 「zoom <factor>」 ── 現在 scale に factor を乗算する
                                val factor = line.removePrefix("zoom ").trim().toFloatOrNull()
                                if (factor != null) {
                                    run {
                                        val base = currentScale ?: initialScale ?: 1f
                                        val ns = base * factor
                                        initialScale = ns
                                        currentScale = ns
                                    }
                                    out.write("ok scale=${currentScale}\n".toByteArray())
                                } else {
                                    out.write("error: invalid factor\n".toByteArray())
                                }
                            }
                            line.startsWith("pan ") -> {
                                // 「pan <dx> <dy>」 ── current offset に (dx, dy) を加算
                                val parts = line.removePrefix("pan ").split(" ").mapNotNull { it.toFloatOrNull() }
                                if (parts.size >= 2) {
                                    run {
                                        val base = currentOffset ?: initialOffset ?: Offset.Zero
                                        val no = Offset(base.x + parts[0], base.y + parts[1])
                                        initialOffset = no
                                        currentOffset = no
                                    }
                                    out.write("ok offset=${currentOffset}\n".toByteArray())
                                } else {
                                    out.write("error: pan needs <dx> <dy>\n".toByteArray())
                                }
                            }
                            line.startsWith("view ") -> {
                                // 「view <scale> <ox> <oy>」 ── 絶対値で set
                                val parts = line.removePrefix("view ").split(" ").mapNotNull { it.toFloatOrNull() }
                                if (parts.size >= 3) {
                                    run {
                                        initialScale = parts[0]
                                        initialOffset = Offset(parts[1], parts[2])
                                        currentScale = parts[0]
                                        currentOffset = Offset(parts[1], parts[2])
                                    }
                                    out.write("ok scale=${parts[0]} offset=(${parts[1]},${parts[2]})\n".toByteArray())
                                } else {
                                    out.write("error: view needs <scale> <ox> <oy>\n".toByteArray())
                                }
                            }
                            line == "fit" -> {
                                // 全体フィットに戻す (CADView が initial が null の時に再計算する)
                                run {
                                    initialScale = null
                                    initialOffset = null
                                    currentScale = null
                                    currentOffset = null
                                }
                                out.write("ok fit\n".toByteArray())
                            }
                            line == "state" -> {
                                out.write("scale=${currentScale ?: initialScale} offset=${currentOffset ?: initialOffset}\n".toByteArray())
                            }
                            line == "inspector" -> {
                                // 画面に頼らず Inspector 数値を CP 越しに text で取る。
                                // capture (Robot screenshot) が他 window に覆われる環境でも
                                // viewer 内部状態から数値で観測できる経路。
                                val r = parseResult
                                if (r == null || r.texts.isEmpty()) {
                                    out.write("error: no parseResult or no texts\n".toByteArray())
                                } else {
                                    val heights = r.texts.map { it.height }
                                    val minH = heights.min().toFloat()
                                    val avgH = heights.average().toFloat()
                                    val maxH = heights.max().toFloat()
                                    val ds = r.drawingScaleDenominator
                                    val scaleStr = ds?.let { "1/${it.toInt()}" } ?: "unknown"
                                    val paperAvg = ds?.let {
                                        com.jpaver.trianglelist.scale.TextSizePolicy.modelToPaper(avgH, it)
                                    }
                                    val paperAvgStr = paperAvg?.let { "%.4f".format(it) } ?: "null"
                                    val jisGap = if (paperAvg != null && paperAvg > 0f) {
                                        "%.2f".format(com.jpaver.trianglelist.scale.TextSizePolicy.DIMENSION_PAPER_MM / paperAvg)
                                    } else "null"
                                    out.write(
                                        ("texts_count=${r.texts.size} " +
                                         "height_min=%.2f height_avg=%.2f height_max=%.2f ".format(minH, avgH, maxH) +
                                         "drawing_scale=$scaleStr " +
                                         "paper_avg_mm=$paperAvgStr " +
                                         "jis_gap_factor=$jisGap\n").toByteArray()
                                    )
                                }
                            }
                            line == "overlaps" || line.startsWith("overlaps ") -> {
                                // 「overlaps [<textWidthFactor>]」 ── 現状図面の重なりを数値で観測する
                                // (ADR 0002 段階 2: まず測る、直すのは後)。配置は一切変更しない。
                                val argStr = line.removePrefix("overlaps").trim()
                                val factor = if (argStr.isEmpty()) 1.0f else argStr.toFloatOrNull()
                                val r = parseResult
                                if (factor == null) {
                                    out.write("error: invalid factor\n".toByteArray())
                                } else if (r == null || r.texts.isEmpty()) {
                                    out.write("error: no parseResult or no texts\n".toByteArray())
                                } else {
                                    val report = com.jpaver.trianglelist.label.DxfOverlapAnalyzer.analyze(r, factor)
                                    val top = report.pairs.take(5)
                                        .joinToString(",") { "${it.textId}x${it.otherId}" }
                                    out.write(
                                        ("overlap_texts=${report.overlappingTexts}/${report.totalTexts} " +
                                         "pairs=${report.pairs.size} top=$top\n").toByteArray()
                                    )
                                }
                            }
                            line.startsWith("capture ") -> {
                                // viewer 窓を AlwaysOnTop で一瞬前面に出して Robot で撮る。
                                // toFront だけでは Windows 11 の focus-steal 抑止で前面に出ない、
                                // AlwaysOnTop なら確実 (撮ったあとすぐ解除して user 作業に戻す)。
                                val path = line.removePrefix("capture ").trim()
                                val win = awtWindow
                                if (win == null) {
                                    out.write("error: no awt window\n".toByteArray())
                                } else {
                                    try {
                                        javax.swing.SwingUtilities.invokeAndWait {
                                            win.isAlwaysOnTop = true
                                            win.toFront()
                                        }
                                        Thread.sleep(200)  // 前面化 + 再描画待ち
                                        val bounds = win.bounds
                                        val robot = java.awt.Robot()
                                        val img = robot.createScreenCapture(bounds)
                                        javax.swing.SwingUtilities.invokeAndWait {
                                            win.isAlwaysOnTop = false
                                        }
                                        val outFile = java.io.File(path)
                                        outFile.parentFile?.mkdirs()
                                        javax.imageio.ImageIO.write(img, "png", outFile)
                                        out.write("ok ${outFile.absolutePath}\n".toByteArray())
                                        println("CP capture: ${outFile.absolutePath} (${bounds.width}x${bounds.height} at ${bounds.x},${bounds.y})")
                                    } catch (e: Exception) {
                                        out.write("error: ${e.message}\n".toByteArray())
                                        println("CP capture error: ${e.message}")
                                    }
                                }
                            }
                            else -> {
                                out.write("error: unknown command (open|zoom|pan|view|fit|state|capture)\n".toByteArray())
                            }
                        }
                    } catch (e: Exception) {
                        println("CP handler error: ${e.message}")
                    } finally {
                        try { socket.close() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                println("CP fatal: ${e.message}")
            }
        }
    }

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

        // Inspector: DXF の TEXT 群を paper-mm に逆算して JIS との乖離を表示。
        // drawingScale は DxfParser が TEXT 内の「1/N (A3)」表記から確実に抽出する。
        // ファイル名 hint なし、DIMSCALE 等の壊れた variable にも依存しない。
        parseResult?.let { result ->
            if (result.texts.isNotEmpty()) {
                val heights = result.texts.map { it.height }
                val avgModelMm = heights.average().toFloat()
                val minModelMm = heights.min().toFloat()
                val maxModelMm = heights.max().toFloat()
                val drawingScaleDenominator = result.drawingScaleDenominator
                val scaleLabel = drawingScaleDenominator?.let { "1/${it.toInt()}" } ?: "?"
                val avgPaperMm = drawingScaleDenominator?.let {
                    com.jpaver.trianglelist.scale.TextSizePolicy.modelToPaper(avgModelMm, it)
                }
                val jisDimensionMm = com.jpaver.trianglelist.scale.TextSizePolicy.DIMENSION_PAPER_MM
                val paperAvgLabel = avgPaperMm?.let { "${"%.4f".format(it)} mm" } ?: "縮尺不明"
                val gapLabel = if (avgPaperMm != null && avgPaperMm > 0f) {
                    "${"%.1f".format(jisDimensionMm / avgPaperMm)} 倍小"
                } else "─"
                Text(
                    text = "[Inspector] DXF TEXT 個数=${result.texts.size}, " +
                        "model height min=${"%.2f".format(minModelMm)} / avg=${"%.2f".format(avgModelMm)} / max=${"%.2f".format(maxModelMm)}  |  " +
                        "drawingScale=$scaleLabel → paper avg=$paperAvgLabel  |  " +
                        "JIS 寸法値想定 ${jisDimensionMm} mm との乖離 $gapLabel",
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.caption,
                    color = androidx.compose.ui.graphics.Color(0xFFFF6600)
                )
            }
        }

        parseResult?.let { result ->
            // ファイル名を key にして CADView を再生成。
            // CADView 内部の isInitialized が remember に抱えられているため、
            // 別ファイルを CP 越しに open しても fit が再計算されないバグへの対処。
            androidx.compose.runtime.key(currentFile?.absolutePath) {
                if (useAwtViewer) {
                    CADViewAwt(
                        parseResult = result,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
    }
}

