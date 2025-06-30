import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jpaver.trianglelist.cadview.CADView
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfParser
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CAD Viewer"
    ) {
        var parseResult by remember { mutableStateOf<DxfParseResult?>(null) }
        var showDialog by remember { mutableStateOf(false) }

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
                        
                        parseResult = result
                        
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
            
            parseResult?.let { result ->
                CADView(parseResult = result)
            }
        }
    }
}

