import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jpaver.trianglelist.cadview.CADView
import com.jpaver.trianglelist.cadview.ColoredShape
import com.jpaver.trianglelist.cadview.aciToColor
import com.jpaver.trianglelist.parser.DxfHeader
import com.jpaver.trianglelist.parser.DxfParseResult
import com.jpaver.trianglelist.parser.DxfParser
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CAD Viewer"
    ) {
        var shapes by remember { mutableStateOf<List<ColoredShape>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }
        var dxfHeader by remember { mutableStateOf<DxfHeader?>(null) }
        val scope = rememberCoroutineScope()

        if (showDialog) {
            LaunchedEffect(Unit) {
                val frame = Frame()
                val dialog = FileDialog(frame, "DXFファイルを選択", FileDialog.LOAD)
                dialog.isVisible = true

                dialog.file?.let { fileName ->
                    val selectedFile = File(dialog.directory, fileName)
                    try {
                        val dxfContent = selectedFile.readText()
                        val parser = DxfParser()
                        val result = parser.parse(dxfContent)
                        
                        println("Lines found: ${result.lines.size}")
                        println("Circles found: ${result.circles.size}")
                        println("Polylines found: ${result.lwPolylines.size}")
                        println("Texts found: ${result.texts.size}")
                        
                        // ヘッダー情報をログ出力
                        result.header?.let { header ->
                            println("--- Header Information ---")
                            println("ACADVER: ${header.acadVer}")
                            println("INSUNITS: ${header.insUnits}")
                            println("EXTMIN: ${header.extMin}")
                            println("EXTMAX: ${header.extMax}")
                            println("LIMMIN: ${header.limMin}")
                            println("LIMMAX: ${header.limMax}")
                            println("DIMSCALE: ${header.dimScale}")
                            println("-------------------------")
                        }
                        
                        val newShapes = mutableListOf<ColoredShape>()
                        
                        // LWPOLYLINEを追加
                        result.lwPolylines.forEach { 
                            newShapes.add(ColoredShape.Polyline(it.vertices, it.isClosed, aciToColor(it.color)))
                        }
                        
                        // LINEを追加
                        result.lines.forEach { 
                            newShapes.add(ColoredShape.Line(it.x1, it.y1, it.x2, it.y2, aciToColor(it.color)))
                        }
                        
                        // CIRCLEを追加
                        result.circles.forEach { circle ->
                            newShapes.add(ColoredShape.Circle(circle.centerX, circle.centerY, circle.radius, aciToColor(circle.color)))
                        }

                        shapes = newShapes
                        dxfHeader = result.header
                        
                    } catch (e: Exception) {
                        println("Error loading DXF file: ${e.message}")
                        e.printStackTrace()
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
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("DXFファイルを開く")
            }
            
            if (shapes.isNotEmpty()) {
                CADView(shapes = shapes, header = dxfHeader)
            }
        }
    }
}

// DXFファイルを解析してCADViewに表示する図形に変換
private fun convertToColoredShapes(parseResult: DxfParseResult): List<ColoredShape> {
    val shapes = mutableListOf<ColoredShape>()
    
    // 線分を変換
    parseResult.lines.forEach { line ->
        shapes.add(
            ColoredShape.Line(
                x1 = line.x1,
                y1 = line.y1,
                x2 = line.x2,
                y2 = line.y2,
                color = aciToColor(line.color)
            )
        )
    }
    
    // 円を変換
    parseResult.circles.forEach { circle ->
        shapes.add(
            ColoredShape.Circle(
                centerX = circle.centerX,
                centerY = circle.centerY,
                radius = circle.radius,
                color = aciToColor(circle.color)
            )
        )
    }
    
    // ポリラインを変換
    parseResult.lwPolylines.forEach { polyline ->
        shapes.add(
            ColoredShape.Polyline(
                vertices = polyline.vertices,
                isClosed = polyline.isClosed,
                color = aciToColor(polyline.color)
            )
        )
    }
    
    return shapes
} 