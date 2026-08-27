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
import com.jpaver.trianglelist.dxf.SfcParser
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
    // DXF/SFCファイルパスを取得（オプション以外の引数）
    // --textmm=<紙 mm>: CSV から起こす時の寸法値サイズを紙面基準で指定する。
    // 既定 (未指定) は従来の固定表。JIS Z 8313 の寸法値は 3.5mm
    val dimTextPaperMm = args.firstOrNull { it.startsWith("--textmm=") }?.substringAfter("=")?.toFloatOrNull()
    // --escape: 番号サークルの自動退避を掛けて書き出す (CSV から起こす時のみ)
    val escapeNumbers = args.contains("--escape")
    val dxfFilePath = args.firstOrNull { !it.startsWith("-") && (it.endsWith(".dxf", ignoreCase = true) || it.endsWith(".sfc", ignoreCase = true)) }
        ?: args.firstOrNull { !it.startsWith("-") && it.endsWith(".csv", ignoreCase = true) }
            ?.let { csvToDxfForViewer(it, dimTextPaperMm, escapeNumbers) }

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

/**
 * 開発ループ高速化 (2026-08-27): viewer に CSV をそのまま渡せるようにする。
 * アプリ本番と同じ書き出し経路 (WebDrawingExport.buildDxfText → common DxfFileWriter) で
 * DXF に変換して build/viewer-dxf/ に置き、それを開く。
 *
 * これが無いと「samples の CSV を viewer で見る」たびに別タスク (gradle test 経由の
 * 書き出し probe、約 20 秒) を挟むことになる ── 実データで見た目を検証する動線が
 * 遅いと、検証そのものが省かれる。CSV は MS932 (アプリ保存出力そのまま)。
 */
private fun csvToDxfForViewer(
    csvPath: String,
    dimTextPaperMm: Float? = null,
    escapeNumbers: Boolean = false,
): String? = try {
    val ms932 = java.nio.charset.Charset.forName("MS932")
    val csvFile = File(csvPath).absoluteFile
    val dxf = com.jpaver.trianglelist.web.WebDrawingExport
        .buildDxfText(csvFile.readText(ms932), "", false, dimTextPaperMm, escapeNumbers)
    val outDir = File(System.getProperty("user.dir"), "build/viewer-dxf").apply { mkdirs() }
    val suffix = (dimTextPaperMm?.let { "_" + it.toString().replace('.', 'p') + "mm" } ?: "") +
        (if (escapeNumbers) "_esc" else "")
    val out = File(outDir, csvFile.nameWithoutExtension + suffix + ".dxf")
    out.writeText(dxf, ms932)
    println("CSV → DXF: ${csvFile.absolutePath} → ${out.absolutePath}")
    out.absolutePath
} catch (e: Exception) {
    System.err.println("CSV 変換に失敗: $csvPath (${e.message})")
    null
}

@Composable
private fun CADViewerApp(initialFilePath: String? = null, initialDebugMode: Boolean = false, useAwtViewer: Boolean = false, awtWindow: java.awt.Window? = null) {
    var parseResult by remember { mutableStateOf<DxfParseResult?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(initialDebugMode) }
    var hotReload by remember { mutableStateOf(true) }
    // LabelBox overlay (rev2): 判定が見ている box を青枠で重ね描きする。CP 「boxes on/off」で切替
    var showLabelBoxes by remember { mutableStateOf(false) }
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

    fun loadCadFile(file: File): DxfParseResult? {
        return try {
            // trianglelist 出力の DXF/SFC は MS932 固定。
            // UTF-8 default で読むと日本語が壊れる。
            val content = file.readText(java.nio.charset.Charset.forName("MS932"))
            val result = if (file.extension.equals("sfc", ignoreCase = true)) {
                SfcParser().parse(content)
            } else {
                DxfParser().parse(content)
            }

            println("=== CAD Loaded: ${file.name} (${file.absolutePath}) ===")
            println("Lines: ${result.lines.size}, Circles: ${result.circles.size}, Arcs: ${result.arcs.size}, Polylines: ${result.lwPolylines.size}, Texts: ${result.texts.size}, Hatches: ${result.hatches.size}")

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
            println("Error loading CAD file: ${e.message}")
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

                loadCadFile(file)?.let { result ->
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
                            loadCadFile(file)?.let { result ->
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

    // CP スレッド用の実測メトリクス (rev5): UI と同じ resolver/density から専用
    // TextMeasurer を作る ── フォントは描画と同一 (MS Gothic + cap 補正)、
    // cache は UI と共有しない (TextMeasurer の cache は thread-safe でないため)
    val cpFontResolver = androidx.compose.ui.platform.LocalFontFamilyResolver.current
    val cpDensity = androidx.compose.ui.platform.LocalDensity.current
    val cpLabelMetrics = remember(cpFontResolver, cpDensity) {
        com.jpaver.trianglelist.adapter.MeasuredLabelMetrics(
            androidx.compose.ui.text.TextMeasurer(
                cpFontResolver, cpDensity, androidx.compose.ui.unit.LayoutDirection.Ltr
            ),
            cpDensity
        )
    }


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
                        // 2026-08-12: プラットフォーム default charset (Windows では MS932 系) だと
                        // client 側が UTF-8 で送った日本語 filter 文字列 (dumptexts <日本語>) が化ける。
                        // CP は明示的に UTF-8 固定にする。
                        val line = socket.getInputStream().bufferedReader(Charsets.UTF_8).readLine()?.trim()
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
                                        loadCadFile(target)?.let { result ->
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
                                    // rev5 確定: 描画と同一の実測メトリクス (MS Gothic + cap 補正) で判定
                                    val report = com.jpaver.trianglelist.label.DxfOverlapAnalyzer.analyze(r, factor, cpLabelMetrics)
                                    // 深さ 0 (〜EPS) = contact (寄り添い、正常)、> EPS = intrusion (めり込み)。
                                    // 足切りはせず観測層のここで分けて報告する (rev1)
                                    val eps = com.jpaver.trianglelist.label.LabelBox.EPS
                                    val (intrusions, contacts) = report.pairs.partition { it.depthMm > eps }
                                    val top = report.pairs.sortedByDescending { it.depthMm }.take(5)
                                        .joinToString(",") { "${it.textId}x${it.otherId}@%.1f".format(it.depthMm) }
                                    out.write(
                                        ("overlap_texts=${report.overlappingTexts}/${report.totalTexts} " +
                                         "pairs=${report.pairs.size} contact=${contacts.size} intrusion=${intrusions.size} " +
                                         "circled=${report.circledNumbers.size} " +
                                         "top=$top\n").toByteArray()
                                    )
                                }
                            }
                            line == "boxes on" || line == "boxes off" -> {
                                // LabelBox overlay の表示トグル (rev2)。判定が見ている box を目で確認する
                                showLabelBoxes = line.endsWith("on")
                                out.write("ok boxes=${if (showLabelBoxes) "on" else "off"}\n".toByteArray())
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
                            line.startsWith("dumptexts") -> {
                                // タイトル文字列の目視によるセンタリング判断は主観に頼るので、
                                // parseResult.texts の実座標を客観的な数値で取る (2026-08-12)。
                                // 「dumptexts [containsフィルタ]」で部分一致する text だけ絞れる。
                                val filter = line.removePrefix("dumptexts").trim()
                                val r = parseResult
                                if (r == null) {
                                    out.write("error: no parseResult\n".toByteArray())
                                } else {
                                    val matched = if (filter.isEmpty()) r.texts else r.texts.filter { it.text.contains(filter) }
                                    if (matched.isEmpty()) {
                                        out.write("error: no texts matched '$filter'\n".toByteArray())
                                    } else {
                                        val lines = matched.joinToString("\n") { t ->
                                            "text=\"${t.text}\" x=%.4f y=%.4f height=%.4f alignH=${t.alignH} alignV=${t.alignV}".format(t.x, t.y, t.height)
                                        }
                                        out.write("$lines\n".toByteArray(Charsets.UTF_8))
                                    }
                                }
                            }
                            line.startsWith("renderbuffer") -> {
                                // Robot/OS スクリーンショットを一切使わず、AwtCadPanel.paint() で
                                // オフスクリーン BufferedImage に直接描画して PNG 保存する (2026-08-12
                                // user 指示「スクショ以外の方法でコントローラからバッファ画像を確認
                                // できるようにしろ」)。ウィンドウの可視性・重なり・前面化・DPI に
                                // 一切依存しない ── AwtCadPanelImageGoldenTest と同じ技法をライブの
                                // parseResult に適用するだけ。「renderbuffer <path> [w] [h]」。
                                val args = line.removePrefix("renderbuffer").trim().split(" ").filter { it.isNotBlank() }
                                val path = args.getOrNull(0)
                                val w = args.getOrNull(1)?.toIntOrNull() ?: 1600
                                val h = args.getOrNull(2)?.toIntOrNull() ?: 1200
                                val r = parseResult
                                if (path == null) {
                                    out.write("error: renderbuffer needs <path> [w] [h]\n".toByteArray())
                                } else if (r == null) {
                                    out.write("error: no parseResult (open a file first)\n".toByteArray())
                                } else {
                                    try {
                                        // AWT の TextLayout は空文字で例外を投げる (図枠の空欄セル由来)。
                                        // AwtCadPanelImageGoldenTest と同じ除外を適用する。
                                        val rFiltered = r.copy(texts = r.texts.filter { it.text.isNotEmpty() })
                                        val panel = com.jpaver.trianglelist.cadview.AwtCadPanel()
                                        panel.setBounds(0, 0, w, h)
                                        panel.setParseResult(rFiltered)
                                        val img = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
                                        val g = img.createGraphics()
                                        try { panel.paint(g) } finally { g.dispose() }
                                        val outFile = java.io.File(path)
                                        outFile.parentFile?.mkdirs()
                                        javax.imageio.ImageIO.write(img, "png", outFile)
                                        out.write("ok ${outFile.absolutePath} ${w}x${h}\n".toByteArray())
                                        println("CP renderbuffer: ${outFile.absolutePath} (${w}x${h}, no screenshot)")
                                    } catch (e: Exception) {
                                        out.write("error: ${e.message}\n".toByteArray())
                                        println("CP renderbuffer error: ${e.message}")
                                    }
                                }
                            }
                            line.startsWith("sendto") -> {
                                // 現在開いているファイルを外部アプリで開く (2026-08-12 user 指示
                                // 「デスクトップ操作するんじゃなくてビューワ上から dxf を外部アプリに
                                // センドできるようにしろ」「エクスプローラーのプログラムで開く相当」)。
                                // 「sendto」(引数無し) = OS 既定の関連付け (java.awt.Desktop.open、
                                // エクスプローラーでファイルをダブルクリックするのと同じ経路)。
                                // 「sendto <実行ファイルパス>」= そのプログラムを直接起動して引数に
                                // ファイルパスを渡す (エクスプローラーの「プログラムから開く」相当、
                                // 既定アプリが違っても狙ったアプリを明示できる)。UI 操作の自動化 (メニュー
                                // クリック連打) より、こちらの方が確実で再現性がある。
                                val arg = line.removePrefix("sendto").trim()
                                val file = currentFile
                                if (file == null || !file.exists()) {
                                    out.write("error: no file open (open a file first)\n".toByteArray())
                                } else {
                                    try {
                                        if (arg.isEmpty()) {
                                            java.awt.Desktop.getDesktop().open(file)
                                            out.write("ok opened via OS default association: ${file.absolutePath}\n".toByteArray())
                                            println("CP sendto: OS 既定アプリで開いた ${file.absolutePath}")
                                        } else {
                                            ProcessBuilder(arg, file.absolutePath).start()
                                            out.write("ok launched \"$arg\" ${file.absolutePath}\n".toByteArray())
                                            println("CP sendto: $arg で開いた ${file.absolutePath}")
                                        }
                                    } catch (e: Exception) {
                                        out.write("error: ${e.message}\n".toByteArray())
                                        println("CP sendto error: ${e.message}")
                                    }
                                }
                            }
                            else -> {
                                out.write("error: unknown command (open|zoom|pan|view|fit|state|capture|renderbuffer|sendto)\n".toByteArray())
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
            val dialog = FileDialog(frame, "CADファイルを選択", FileDialog.LOAD)
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

                loadCadFile(selectedFile)?.let { result ->
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
                Text("CADファイルを開く")
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

        // 衝突サマリ + 凡例。boxes on の枠の色が何を意味するかを画面上で対応づける
        // (色だけ出しても「赤が何件で何と当たっているのか」は引きの倍率では読めない)。
        if (showLabelBoxes) {
            parseResult?.let { result ->
                val kinds = remember(result) {
                    com.jpaver.trianglelist.label.DxfOverlapAnalyzer.analyze(result, metrics = cpLabelMetrics)
                        .collisionKindByText
                }
                val labelN = kinds.count { it.value == com.jpaver.trianglelist.label.ObstacleKind.LABEL }
                val circleN = kinds.count { it.value == com.jpaver.trianglelist.label.ObstacleKind.CIRCLE }
                val total = com.jpaver.trianglelist.label.DxfOverlapAnalyzer
                    .textBoxes(result, metrics = cpLabelMetrics).size
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[衝突] ${kinds.size}/$total  ",
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = "■ 文字どうし $labelN",
                        style = MaterialTheme.typography.caption,
                        color = androidx.compose.ui.graphics.Color(0xFFE53935)
                    )
                    Text(
                        text = "  ■ 番号サークル $circleN",
                        style = MaterialTheme.typography.caption,
                        color = androidx.compose.ui.graphics.Color(0xFF8E24AA)
                    )
                    Text(
                        text = "  ■ 衝突なし ${total - kinds.size}",
                        style = MaterialTheme.typography.caption,
                        color = androidx.compose.ui.graphics.Color.Blue
                    )
                }
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
                        showLabelBoxes = showLabelBoxes,
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

