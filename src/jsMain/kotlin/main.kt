import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    var count: Int by mutableStateOf(0)

    renderComposable(rootElementId = "root") {
        Div({
            style {
                padding(25.px)
                fontFamily("Arial, sans-serif")
            }
        }) {
            H1 { Text("CADView - Web版") }
            
            P { Text("KMP Compose for Webで作成されたCADプレビューウィジェット") }
            
            Div({
                style {
                    border(1.px, LineStyle.Solid, Color.blue)
                    padding(20.px)
                    margin(10.px)
                    borderRadius(8.px)
                }
            }) {
                H3 { Text("図面プレビューエリア") }
                P { Text("ここに三角形の描画が表示されます") }
                
                // SVGで簡単な三角形を描画
                Svg({
                    attr("width", "300")
                    attr("height", "200")
                    style {
                        border(1.px, LineStyle.Solid, Color.gray)
                    }
                }) {
                    // 三角形1
                    Polygon({
                        attr("points", "100,100 200,100 150,50")
                        attr("fill", "none")
                        attr("stroke", "blue")
                        attr("stroke-width", "2")
                    })
                    
                    // 三角形2
                    Polygon({
                        attr("points", "200,100 300,100 250,150")
                        attr("fill", "none")
                        attr("stroke", "blue")
                        attr("stroke-width", "2")
                    })
                    
                    // 頂点
                    Circle({
                        attr("cx", "100")
                        attr("cy", "100")
                        attr("r", "4")
                        attr("fill", "red")
                    })
                    Circle({
                        attr("cx", "200")
                        attr("cy", "100")
                        attr("r", "4")
                        attr("fill", "red")
                    })
                    Circle({
                        attr("cx", "150")
                        attr("cy", "50")
                        attr("r", "4")
                        attr("fill", "red")
                    })
                }
            }
            
            Div({
                style {
                    border(1.px, LineStyle.Solid, Color.green)
                    padding(20.px)
                    margin(10.px)
                    borderRadius(8.px)
                }
            }) {
                H3 { Text("三角形情報") }
                P { Text("三角形 1") }
                P { Text("辺A: 100.0") }
                P { Text("辺B: 70.7") }
                P { Text("辺C: 70.7") }
                
                P { Text("三角形 2") }
                P { Text("辺A: 100.0") }
                P { Text("辺B: 111.8") }
                P { Text("辺C: 111.8") }
            }
            
            Button({
                onClick { count += 1 }
                style {
                    padding(10.px)
                    marginTop(20.px)
                    backgroundColor(Color.lightblue)
                    border(1.px, LineStyle.Solid, Color.blue)
                    borderRadius(4.px)
                    cursor("pointer")
                }
            }) {
                Text("クリック回数: $count")
            }
        }
    }
} 