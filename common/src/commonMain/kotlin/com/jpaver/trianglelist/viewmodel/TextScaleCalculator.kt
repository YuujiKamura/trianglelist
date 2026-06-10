package com.jpaver.trianglelist.viewmodel

class TextScaleCalculator {
    companion object {
        // スケール定数
        private const val S_500 = 50f
        private const val S_400 = 40f
        private const val S_300 = 25f
        private const val S_250 = 25f
        private const val S_200 = 20f
        private const val S_150 = 15f
        private const val S_100 = 10f
        private const val S_50 = 5f

        private val fileTypeMap: Map<String, Map<Float, Float>> = initializeFileTypeMap()
        
        private fun initializeFileTypeMap(): Map<String, Map<Float, Float>> {
            val textScaleMapPDF = mapOf(
                S_500 to 3f,
                S_400 to 4f,
                S_300 to 5f,
                S_250 to 6f,
                S_200 to 8f,
                S_150 to 8f
            )
            val textScaleMapCAD = mapOf(
                S_500 to 0.5f,
                S_400 to 0.4f,
                S_300 to 0.35f,
                S_250 to 0.35f,
                S_200 to 0.35f,
                S_150 to 0.25f,
                S_100 to 0.25f,
                S_50 to 0.25f
            )
            return mapOf(
                "dxf" to textScaleMapCAD,
                "sfc" to textScaleMapCAD,
                "pdf" to textScaleMapPDF
            )
        }

        private val defaultScaleMap: Map<String, Float> = mapOf(
            "dxf" to 0.25f,
            "sfc" to 0.25f,
            "pdf" to 8f
        )
    }

    fun getTextScale(printScale: Float, exportFileType: String): Float {
        val defaultValue = defaultScaleMap.getOrDefault(exportFileType, 5f)
        val selectedMap = fileTypeMap.getOrDefault(exportFileType, mapOf())
        return selectedMap.getOrDefault(printScale, defaultValue)
    }
} 