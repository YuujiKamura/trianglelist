import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// 三角形データクラス
data class TriangleData(
    val pointA: Offset,
    val pointB: Offset,
    val pointC: Offset,
    val lengthA: Float,
    val lengthB: Float,
    val lengthC: Float,
    val number: Int = 1
)

// サンプル三角形データ
fun createSampleTriangles(): List<TriangleData> {
    return listOf(
        TriangleData(
            pointA = Offset(100f, 100f),
            pointB = Offset(200f, 100f),
            pointC = Offset(150f, 50f),
            lengthA = 100f,
            lengthB = 70.7f,
            lengthC = 70.7f,
            number = 1
        ),
        TriangleData(
            pointA = Offset(200f, 100f),
            pointB = Offset(300f, 100f),
            pointC = Offset(250f, 150f),
            lengthA = 100f,
            lengthB = 111.8f,
            lengthC = 111.8f,
            number = 2
        )
    )
}

// 三角形描画関数
fun DrawScope.drawTriangle(triangle: TriangleData) {
    val path = Path().apply {
        moveTo(triangle.pointA.x, triangle.pointA.y)
        lineTo(triangle.pointB.x, triangle.pointB.y)
        lineTo(triangle.pointC.x, triangle.pointC.y)
        close()
    }
    
    // 三角形の輪郭を描画
    drawPath(
        path = path,
        color = Color.Blue,
        style = Stroke(width = 2.dp.toPx())
    )
    
    // 頂点を描画
    drawCircle(
        color = Color.Red,
        radius = 4.dp.toPx(),
        center = triangle.pointA
    )
    drawCircle(
        color = Color.Red,
        radius = 4.dp.toPx(),
        center = triangle.pointB
    )
    drawCircle(
        color = Color.Red,
        radius = 4.dp.toPx(),
        center = triangle.pointC
    )
    
    // 三角形番号を描画
    val center = Offset(
        (triangle.pointA.x + triangle.pointB.x + triangle.pointC.x) / 3,
        (triangle.pointA.y + triangle.pointB.y + triangle.pointC.y) / 3
    )
    
    drawCircle(
        color = Color.White,
        radius = 12.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.Black,
        radius = 12.dp.toPx(),
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

@Composable
fun CADViewWidget() {
    val triangles = remember { createSampleTriangles() }
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "CADView",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // 左側：図面プレビュー
                Card(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    elevation = 4.dp
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // 背景
                        drawRect(
                            color = Color.White,
                            size = size
                        )
                        
                        // 三角形を描画
                        triangles.forEach { triangle ->
                            drawTriangle(triangle)
                        }
                    }
                }
                
                // 右側：情報パネル
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "三角形情報",
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        triangles.forEach { triangle ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text("三角形 ${triangle.number}")
                                    Text("辺A: ${String.format("%.1f", triangle.lengthA)}")
                                    Text("辺B: ${String.format("%.1f", triangle.lengthB)}")
                                    Text("辺C: ${String.format("%.1f", triangle.lengthC)}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 