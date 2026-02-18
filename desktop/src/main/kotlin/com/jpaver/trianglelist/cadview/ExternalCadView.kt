package com.jpaver.trianglelist.cadview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * External CAD engine viewer: delegates DXF rendering to Python ezdxf.
 *
 * Flow:
 * 1. Receives a DXF file path
 * 2. Calls Python ezdxf script to convert DXF -> high-res PNG
 * 3. Displays the PNG in Compose Canvas with pan/zoom support
 *
 * Advantages:
 * - Uses ezdxf's production-quality CAD rendering engine
 * - Correct handling of all DXF entity types, layers, line styles, colors
 * - Text rendering with SHX font support
 */

/** Result from the Python conversion process */
data class ConversionResult(
    val pngPath: String,
    val svgPath: String?,
    val imageWidth: Int,
    val imageHeight: Int,
    val entityCount: Int
)

/** State of the external rendering process */
sealed class RenderState {
    data object Idle : RenderState()
    data object Converting : RenderState()
    data class Ready(val image: ImageBitmap, val result: ConversionResult) : RenderState()
    data class Error(val message: String) : RenderState()
}

/**
 * Convert DXF to PNG/SVG using the Python ezdxf script.
 */
suspend fun convertDxfWithEzdxf(
    dxfPath: String,
    outputDir: String? = null,
    dpi: Int = 300,
    background: String = "white"
): ConversionResult = withContext(Dispatchers.IO) {
    val dir = outputDir ?: System.getProperty("java.io.tmpdir")
    val baseName = File(dxfPath).nameWithoutExtension
    val outputBase = File(dir, "ezdxf_${baseName}_${System.currentTimeMillis()}").absolutePath

    val scriptPath = findPythonScript()
    val pythonCmd = findPythonCommand()

    val process = ProcessBuilder(
        pythonCmd,
        scriptPath,
        dxfPath,
        outputBase,
        "--format", "both",
        "--dpi", dpi.toString(),
        "--bg", background
    ).apply {
        redirectErrorStream(false)
        environment()["PYTHONIOENCODING"] = "utf-8"
    }.start()

    val stdout = process.inputStream.bufferedReader().readText()
    val stderr = process.errorStream.bufferedReader().readText()
    val exitCode = process.waitFor()

    if (exitCode != 0) {
        val errorMsg = if (stderr.isNotBlank()) stderr else "Python process exited with code $exitCode"
        throw RuntimeException("DXF conversion failed: $errorMsg")
    }

    parseConversionResult(stdout, outputBase)
}

/** Find the dxf_to_image.py script */
private fun findPythonScript(): String {
    val projectRoot = System.getProperty("user.dir")
    val candidates = listOf(
        File(projectRoot, "desktop/scripts/dxf_to_image.py"),
        File(projectRoot, "scripts/dxf_to_image.py"),
        File(projectRoot, "../desktop/scripts/dxf_to_image.py")
    )

    for (candidate in candidates) {
        if (candidate.exists()) return candidate.absolutePath
    }

    throw RuntimeException(
        "Python script dxf_to_image.py not found. Searched:\n" +
            candidates.joinToString("\n") { "  - ${it.absolutePath}" }
    )
}

/** Find the Python 3 command */
private fun findPythonCommand(): String {
    val candidates = if (System.getProperty("os.name").lowercase().contains("win")) {
        listOf("python", "python3", "py")
    } else {
        listOf("python3", "python")
    }

    for (cmd in candidates) {
        try {
            val proc = ProcessBuilder(cmd, "--version")
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            if (proc.waitFor() == 0 && output.contains("Python 3")) return cmd
        } catch (_: Exception) { }
    }
    throw RuntimeException("Python 3 not found. Install Python 3 and ensure it's on PATH.")
}

/** Parse JSON output from the Python script */
private fun parseConversionResult(jsonOutput: String, outputBase: String): ConversionResult {
    val trimmed = jsonOutput.trim()
    val entityCount = extractJsonInt(trimmed, "entity_count") ?: 0
    val width = extractJsonInt(trimmed, "width") ?: 0
    val height = extractJsonInt(trimmed, "height") ?: 0

    val pngPath = "$outputBase.png"
    val svgPath = "$outputBase.svg"

    if (!File(pngPath).exists()) {
        throw RuntimeException("PNG output not generated at: $pngPath")
    }

    return ConversionResult(
        pngPath = pngPath,
        svgPath = if (File(svgPath).exists()) svgPath else null,
        imageWidth = width,
        imageHeight = height,
        entityCount = entityCount
    )
}

private fun extractJsonInt(json: String, key: String): Int? {
    val regex = Regex(""""$key"\s*:\s*(\d+)""")
    return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
}

/**
 * Composable: displays a DXF file rendered by the external ezdxf engine.
 * Supports pan (drag) and zoom (scroll wheel).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ExternalCadView(
    dxfFilePath: String,
    modifier: Modifier = Modifier,
    dpi: Int = 300,
    background: String = "white"
) {
    var renderState by remember { mutableStateOf<RenderState>(RenderState.Idle) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isInitialized by remember { mutableStateOf(false) }

    // Trigger conversion when file path changes
    LaunchedEffect(dxfFilePath) {
        renderState = RenderState.Converting
        isInitialized = false
        try {
            val result = convertDxfWithEzdxf(dxfFilePath, dpi = dpi, background = background)
            val imageBytes = File(result.pngPath).readBytes()
            val skiaImage = Image.makeFromEncoded(imageBytes)
            val bitmap = skiaImage.toComposeImageBitmap()
            renderState = RenderState.Ready(bitmap, result)
        } catch (e: Exception) {
            renderState = RenderState.Error(e.message ?: "Unknown error")
            println("ExternalCadView error: ${e.message}")
            e.printStackTrace()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = renderState) {
            is RenderState.Idle -> {
                Text("DXFファイルを選択してください", modifier = Modifier.align(Alignment.Center))
            }

            is RenderState.Converting -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ezdxf で変換中...")
                }
            }

            is RenderState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("変換エラー", color = Color.Red, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            is RenderState.Ready -> {
                val image = state.image
                val result = state.result

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current

                    // Fit image to viewport on first load
                    LaunchedEffect(image, maxWidth, maxHeight) {
                        if (isInitialized) return@LaunchedEffect
                        val canvasW = with(density) { maxWidth.toPx() }
                        val canvasH = with(density) { maxHeight.toPx() }
                        val imgW = image.width.toFloat()
                        val imgH = image.height.toFloat()
                        if (imgW <= 0f || imgH <= 0f) return@LaunchedEffect

                        val fitScale = min(canvasW / imgW, canvasH / imgH) * 0.95f
                        scale = fitScale
                        offset = Offset(
                            (canvasW - imgW * fitScale) / 2f,
                            (canvasH - imgH * fitScale) / 2f
                        )
                        isInitialized = true
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offset += dragAmount
                                }
                            }
                            .onPointerEvent(PointerEventType.Scroll) {
                                val zoomFactor = if (it.changes.first().scrollDelta.y > 0) 0.9f else 1.1f
                                val newScale = (scale * zoomFactor).coerceIn(0.01f, 50f)

                                // Zoom toward pointer position
                                val pointerPos = it.changes.first().position
                                val worldX = (pointerPos.x - offset.x) / scale
                                val worldY = (pointerPos.y - offset.y) / scale

                                offset = Offset(
                                    pointerPos.x - worldX * newScale,
                                    pointerPos.y - worldY * newScale
                                )
                                scale = newScale
                            }
                    ) {
                        drawImage(
                            image = image,
                            dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                            dstSize = IntSize(
                                (image.width * scale).roundToInt(),
                                (image.height * scale).roundToInt()
                            )
                        )
                    }
                }

                // Info overlay
                Text(
                    text = "ezdxf render | ${result.entityCount} entities | ${result.imageWidth}x${result.imageHeight}px",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(4.dp),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}
