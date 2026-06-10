package com.jpaver.trianglelist.dxf

/**
 * DxfIndex / DxfAnalyzer のファイル I/O 部 (java.io.File / Properties / currentTimeMillis 依存)。
 * commonMain への wasmJs ターゲット追加 (Web 段階1) に伴い、commonMain の本体から
 * extension としてここへ移設した。呼び出し形 (DxfIndex.fromFile(...) 等) は移設前と同名。
 */

/**
 * インデックスをキャッシュファイルに保存
 */
fun DxfIndex.saveToCache(cacheFile: java.io.File) {
    try {
        val props = java.util.Properties()

        // 測点情報を保存
        getAllStations().forEach { (name, coord) ->
            props.setProperty("station.$name", "${coord.first},${coord.second}")
        }

        // レイヤー情報を保存（レイヤー名のみ）
        props.setProperty("layers", getAllLayers().joinToString("|"))

        // 統計情報
        props.setProperty("lineCount", parseResult.lines.size.toString())
        props.setProperty("textCount", parseResult.texts.size.toString())
        props.setProperty("polylineCount", parseResult.lwPolylines.size.toString())

        cacheFile.outputStream().use { props.store(it, "DxfIndex Cache") }
        println("Index cache saved: ${cacheFile.absolutePath}")
    } catch (e: Exception) {
        println("Failed to save index cache: ${e.message}")
    }
}

/**
 * DXFファイルからインデックスを作成（キャッシュ対応）
 */
fun DxfIndex.Companion.fromFile(filePath: String, useCache: Boolean = true): DxfIndex? {
    val dxfFile = java.io.File(filePath)
    val cacheFile = java.io.File(filePath + ".idx")

    // キャッシュが有効かチェック
    if (useCache && cacheFile.exists() && cacheFile.lastModified() >= dxfFile.lastModified()) {
        println("Using cached index: ${cacheFile.absolutePath}")
        return fromCache(filePath, cacheFile)
    }

    // フルパース
    val startTime = System.currentTimeMillis()
    return try {
        val content = dxfFile.readText()
        val parser = DxfParser()
        val result = parser.parse(content)
        val index = DxfIndex(result)

        val elapsed = System.currentTimeMillis() - startTime
        println("DXF parsed in ${elapsed}ms: $filePath")

        // キャッシュを保存
        if (useCache) {
            index.saveToCache(cacheFile)
        }

        index
    } catch (e: Exception) {
        println("Error creating DxfIndex: ${e.message}")
        null
    }
}

/**
 * キャッシュファイルから測点情報のみ高速読み込み
 */
fun DxfIndex.Companion.fromCache(dxfPath: String, cacheFile: java.io.File): DxfIndex? {
    val startTime = System.currentTimeMillis()
    return try {
        val props = java.util.Properties()
        cacheFile.inputStream().use { props.load(it) }

        // 測点情報を復元
        val stationMap = mutableMapOf<String, Pair<Double, Double>>()
        props.stringPropertyNames()
            .filter { it.startsWith("station.") }
            .forEach { key ->
                val name = key.removePrefix("station.")
                val value = props.getProperty(key)
                val parts = value.split(",")
                if (parts.size == 2) {
                    stationMap[name] = Pair(parts[0].toDouble(), parts[1].toDouble())
                }
            }

        val elapsed = System.currentTimeMillis() - startTime
        println("Index cache loaded in ${elapsed}ms (${stationMap.size} stations)")

        // 軽量なDxfIndexを返す（フルデータなし）
        CachedDxfIndex(stationMap)
    } catch (e: Exception) {
        println("Failed to load index cache: ${e.message}")
        null
    }
}

/**
 * DXFファイルのサイズ分析（ファイル読み込み付き）
 */
fun DxfAnalyzer.analyzeFile(filePath: String): DxfAnalyzer.AnalysisResult? {
    return try {
        val content = java.io.File(filePath).readText()
        analyze(content)
    } catch (e: Exception) {
        println("Error analyzing DXF: ${e.message}")
        null
    }
}
