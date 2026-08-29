package com.jpaver.trianglelist.agent

/**
 * エージェント作図機構 (2026-08-29)
 *
 * 複数のエージェントが CAD Viewer 上で同時に作図するための機構。
 *
 * 設計の要点:
 *  - **1 エージェント = 1 レイヤ**。書き込みは自分のレイヤに限定される。
 *    peer 同士がお互いの成果物を壊す事故を、構造で防ぐ (共有 mutable state を持たない)。
 *  - エージェントは生の線分ではなく **部品 (part)** を置く。
 *    コーン間隔・テーパー角などの基準値は部品側に埋め込む。
 *    エージェントは「どこに何を置くか」だけを決める = 判断の層と作図の層を分離する。
 *  - 部品単位なので後から検査 (DRC) が書ける。
 *
 * 単位はモデル空間 = 紙面 mm (A3 420x297, 1:1) を想定。
 */
import com.jpaver.trianglelist.dxf.DxfCircle
import com.jpaver.trianglelist.dxf.DxfLine
import com.jpaver.trianglelist.dxf.DxfLwPolyline
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfText

/** エージェントが書き込んだ図形を、レイヤ名をキーに保持する。 */
object AgentLayers {
    private val store = LinkedHashMap<String, MutableList<Any>>()
    private val owners = LinkedHashMap<String, String>()

    @Synchronized
    fun claim(layer: String, agent: String): Boolean {
        val cur = owners[layer]
        if (cur != null && cur != agent) return false
        owners[layer] = agent
        return true
    }

    @Synchronized
    fun add(layer: String, ents: List<Any>) {
        store.getOrPut(layer) { mutableListOf() }.addAll(ents)
    }

    @Synchronized
    fun clear(layer: String) { store.remove(layer) }

    @Synchronized
    fun clearAll() { store.clear(); owners.clear() }

    @Synchronized
    fun listing(): String =
        if (store.isEmpty()) "(empty)"
        else store.entries.joinToString("; ") { (k, v) ->
            "$k owner=${owners[k] ?: "-"} n=${v.size}"
        }

    /** ベースの解析結果に、全エージェントレイヤを重ねた結果を返す。 */
    @Synchronized
    fun merge(base: DxfParseResult?): DxfParseResult {
        val b = base ?: DxfParseResult()
        val all = store.values.flatten()
        return b.copy(
            lines = b.lines + all.filterIsInstance<DxfLine>(),
            circles = b.circles + all.filterIsInstance<DxfCircle>(),
            lwPolylines = b.lwPolylines + all.filterIsInstance<DxfLwPolyline>(),
            texts = b.texts + all.filterIsInstance<DxfText>()
        )
    }

    @Synchronized
    fun isEmpty(): Boolean = store.isEmpty()

    /** 全レイヤの図形を (レイヤ名, 図形) の平坦な列として返す。index が編集の識別子になる。 */
    @Synchronized
    fun flat(): List<Pair<String, Any>> =
        store.entries.flatMap { (lay, list) -> list.map { lay to it } }

    /** flat() の index で図形を差し替える。 */
    @Synchronized
    fun replaceAt(index: Int, f: (Any) -> Any) {
        var i = 0
        for ((_, list) in store) {
            if (index < i + list.size) { list[index - i] = f(list[index - i]); return }
            i += list.size
        }
    }

    /** flat() の index で図形を削除する。 */
    @Synchronized
    fun deleteAt(index: Int) {
        var i = 0
        for ((_, list) in store) {
            if (index < i + list.size) { list.removeAt(index - i); return }
            i += list.size
        }
    }
}

/** `k=v k=v` 形式の引数をパースする。値に空白を含めたい場合は `k="a b"`。 */
fun parseArgs(s: String): Map<String, String> {
    val m = LinkedHashMap<String, String>()
    var i = 0
    while (i < s.length) {
        while (i < s.length && s[i] == ' ') i++
        val eq = s.indexOf('=', i); if (eq < 0) break
        val key = s.substring(i, eq).trim()
        var j = eq + 1
        val v: String
        if (j < s.length && s[j] == '"') {
            val end = s.indexOf('"', j + 1)
            v = if (end < 0) s.substring(j + 1) else s.substring(j + 1, end)
            j = if (end < 0) s.length else end + 1
        } else {
            var end = s.indexOf(' ', j); if (end < 0) end = s.length
            v = s.substring(j, end); j = end
        }
        m[key] = v; i = j
    }
    return m
}

private fun Map<String, String>.d(k: String, def: Double = 0.0) =
    this[k]?.toDoubleOrNull() ?: def
private fun Map<String, String>.i(k: String, def: Int) =
    this[k]?.toIntOrNull() ?: def

/** 部品を生成する。未知の kind は null を返す。 */
object Parts {
    /** 保安施設設置基準(案) 標準図の値。部品側に基準を埋め込む。 */
    const val CONE_PITCH_MIN_M = 3.0
    const val CONE_PITCH_MAX_M = 5.0
    const val TAPER_ANGLE_MIN_DEG = 15.0
    const val TAPER_ANGLE_MAX_DEG = 30.0

    fun build(kind: String, a: Map<String, String>, layer: String): List<Any>? = when (kind) {
        "line" -> listOf(DxfLine(a.d("x1"), a.d("y1"), a.d("x2"), a.d("y2"), a.i("color", 7), layer))
        "rect" -> {
            val x = a.d("x"); val y = a.d("y"); val w = a.d("w"); val h = a.d("h")
            listOf(DxfLwPolyline(listOf(x to y, x + w to y, x + w to y + h, x to y + h), true, a.i("color", 7), layer))
        }
        "text" -> listOf(DxfText(a.d("x"), a.d("y"), a["t"] ?: "", a.d("h", 3.5), a.d("rot"), a.i("color", 7), layer = layer))
        "circle" -> listOf(DxfCircle(a.d("x"), a.d("y"), a.d("r", 2.0), a.i("color", 7), layer))
        // --- 保安施設の部品 ---
        "cone_line" -> {
            // x1,y1 -> x2,y2 を n 個のコーンで埋める。n 未指定なら pitch(紙mm) から求める。
            val x1 = a.d("x1"); val y1 = a.d("y1"); val x2 = a.d("x2"); val y2 = a.d("y2")
            val len = Math.hypot(x2 - x1, y2 - y1)
            val n = a["n"]?.toIntOrNull() ?: maxOf(2, (len / a.d("pitch", 7.0)).toInt() + 1)
            // n=1 は「その位置に1個だけ」の意味 (凡例など)。t の除算で 0 割りにならないよう分岐する。
            val out = mutableListOf<Any>()
            for (k in 0 until n) {
                val t = if (n == 1) 0.0 else k.toDouble() / (n - 1)
                val cx = x1 + (x2 - x1) * t; val cy = y1 + (y2 - y1) * t
                out += DxfLwPolyline(
                    listOf(cx - 1.0 to cy - 1.0, cx + 1.0 to cy - 1.0, cx to cy + 1.4),
                    true, a.i("color", 7), layer)
            }
            out
        }
        "guard" -> {
            // 交通誘導警備員。kind=A(検定合格者)/B
            val x = a.d("x"); val y = a.d("y"); val k = a["k"] ?: "B"
            listOf(
                DxfCircle(x, y, 2.2, a.i("color", 5), layer),
                DxfText(x - 0.9, y - 1.2, k, 2.5, 0.0, a.i("color", 5), layer = layer)
            )
        }
        "sign" -> {
            // 標識・表示板。id は基準の記号番号 (①〜⑲)。
            val x = a.d("x"); val y = a.d("y"); val w = a.d("w", 14.0); val h = a.d("h", 8.0)
            val out = mutableListOf<Any>(
                DxfLwPolyline(listOf(x to y, x + w to y, x + w to y + h, x to y + h), true, a.i("color", 7), layer),
                DxfLine(x + w / 2, y, x + w / 2, y - 4.0, a.i("color", 7), layer)
            )
            (a["t"] ?: "").split("|").forEachIndexed { idx, ln ->
                out += DxfText(x + 1.0, y + h - 3.0 - idx * 3.0, ln, 2.5, 0.0, a.i("color", 7), layer = layer)
            }
            out
        }
        "dim" -> {
            // 寸法線 (水平)
            val x1 = a.d("x1"); val x2 = a.d("x2"); val y = a.d("y")
            val c = a.i("color", 7)
            listOf(
                DxfLine(x1, y, x2, y, c, layer),
                DxfLine(x1, y - 1.5, x1, y + 1.5, c, layer),
                DxfLine(x2, y - 1.5, x2, y + 1.5, c, layer),
                DxfText((x1 + x2) / 2 - (a["t"] ?: "").length * 0.9, y + 1.2, a["t"] ?: "", 3.5, 0.0, c, layer = layer)
            )
        }
        "frame" -> {
            // A3 図郭
            val m = a.d("m", 10.0); val w = a.d("w", 420.0); val h = a.d("h", 297.0)
            listOf(DxfLwPolyline(listOf(m to m, w - m to m, w - m to h - m, m to h - m), true, a.i("color", 7), layer))
        }
        else -> null
    }

    val kinds = listOf("line", "rect", "text", "circle", "cone_line", "guard", "sign", "dim", "frame")
}

/**
 * DxfParseResult を DXF テキストへ書き出す (最小構成)。
 * viewer は MS932 固定で読むので、呼び出し側で MS932 で書くこと。
 */
fun exportDxf(r: DxfParseResult): String {
    val sb = StringBuilder()
    fun g(code: Int, v: Any) { sb.append(code).append('\n').append(v).append('\n') }
    g(0, "SECTION"); g(2, "HEADER")
    g(9, "\$INSUNITS"); g(70, 4)
    g(9, "\$DWGCODEPAGE"); g(3, "ANSI_932")
    g(0, "ENDSEC")
    g(0, "SECTION"); g(2, "ENTITIES")
    for (e in r.lines) {
        g(0, "LINE"); g(8, e.layer); g(62, e.color)
        g(10, e.x1); g(20, e.y1); g(30, 0.0); g(11, e.x2); g(21, e.y2); g(31, 0.0)
    }
    for (e in r.circles) {
        g(0, "CIRCLE"); g(8, e.layer); g(62, e.color)
        g(10, e.centerX); g(20, e.centerY); g(30, 0.0); g(40, e.radius)
    }
    for (e in r.lwPolylines) {
        g(0, "LWPOLYLINE"); g(8, e.layer); g(62, e.color)
        g(90, e.vertices.size); g(70, if (e.isClosed) 1 else 0)
        for (v in e.vertices) { g(10, v.first); g(20, v.second) }
    }
    for (e in r.texts) {
        g(0, "TEXT"); g(8, e.layer); g(62, e.color)
        g(10, e.x); g(20, e.y); g(30, 0.0); g(40, e.height); g(1, e.text); g(50, e.rotation)
    }
    g(0, "ENDSEC"); g(0, "EOF")
    return sb.toString()
}
