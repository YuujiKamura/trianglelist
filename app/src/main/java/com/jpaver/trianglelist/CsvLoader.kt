package com.jpaver.trianglelist

import android.widget.EditText
import com.jpaver.trianglelist.util.InputParameter
import java.io.BufferedReader

class CsvLoader {

    fun parseCSV(
        reader: BufferedReader,
        showToast: (String) -> Unit,
        addTriangle: (TriangleList, List<String?>, PointXY, Float) -> Unit,
        setAllTextSize: (Float) -> Unit,
        typeToInt: (String) -> Int,
        viewscale: Float,
        rosennameEditText: EditText
    ): ReturnValues? {
        val trilist = TriangleList()
        val dedlist = DeductionList()
        var headerRead = false
        var headerValues: HeaderValues? = null
        var firstLine = true

        while (true) {
            val line = reader.readLine() ?: break
            val chunks = line.split(",").map { it.trim() }

            if (!headerRead) {
                if (chunks[0] != "koujiname") {
                    showToast("It's not supported file.")
                    return null
                }
                headerValues = readCsvHeaderLines(chunks, reader)
                rosennameEditText.setText(headerValues.rosenname)
                headerRead = true
                continue
            }

            if (firstLine) {
                buildFirstTriangle(addTriangle, trilist, chunks)
                firstLine = false
                continue
            }

            // リストの回転とかテキストサイズなどの状態
            if (readListParameter(chunks, trilist, setAllTextSize)) continue

            // 控除
            if (buildDeductions(chunks, dedlist, typeToInt, viewscale)) continue

            // 三角形
            buildTriangle2(addTriangle, trilist, chunks)
        }

        return if (headerValues != null) ReturnValues(trilist, dedlist, headerValues) else null
    }

    fun buildTriangle2(
        addTriangle: (TriangleList, List<String?>, PointXY, Float) -> Unit,
        trilist: TriangleList,
        chunks: List<String>
    ){
        val connectiontype = chunks[5].toInt()

        //非接続
        if( connectiontype == 0 ){
            val pt = PointXY(0f, 0f)
            var angle = 0f

            if( chunks.size > 22 ){
                pt.set( -chunks[23].toFloat(), -chunks[24].toFloat() )
                angle = chunks[22].toFloat()
            }

            addTriangle(trilist, chunks, pt, angle - 180f)
        }
        //接続
        else {
            val ptri = trilist.getBy(chunks[4].toInt())
            val cp = readCParam(chunks)
            trilist.add(
                Triangle(
                    ptri, cp,
                    chunks[2].toFloat(),
                    chunks[3].toFloat()
                ),
                true
            )
        }

        finalizeBuildTriangle(chunks, trilist.getBy(trilist.size()) )
    }

    fun buildFirstTriangle(
        addTriangle:(TriangleList,List<String?>,PointXY,Float) -> Unit,
        trilist: TriangleList,
        chunks: List<String?>,
    ){
        val pointfirst = PointXY(0f, 0f)
        val anglefirst = 180f

        addTriangle(trilist, chunks, pointfirst, anglefirst)
        finalizeBuildTriangle(chunks, trilist.getBy(trilist.size()) )
    }

    fun finalizeBuildTriangle(chunks: List<String?>, mt:Triangle){
        mt.connectionType = chunks[5]!!.toInt()
        mt.name = chunks[6]!!.toString()
        if( chunks[9]!! == "true" ) readPointNumber( chunks, mt)
        if( chunks.size > 10 ) mt.setColor(chunks[10]!!.toInt())
        if( chunks.size > 11 ) readDimAligns(chunks, mt)
        if( chunks.size > 17 ) mt.cParam_ = readCParam(chunks)
        if( chunks.size > 20 ) mt.dim.flag = readDimFlag(chunks)
    }

    fun readPointNumber(chunks: List<String?>, mt:Triangle){
        mt.setPointNumber(
            PointXY(
                chunks[7]!!.toFloat(),
                chunks[8]!!.toFloat()
            )
        )
    }

    fun readDimAligns(chunks: List<String?>, triangle:Triangle ){
            triangle.setDimAligns(
                chunks[11]!!.toInt(), chunks[12]!!.toInt(), chunks[13]!!.toInt(),
                chunks[14]!!.toInt(), chunks[15]!!.toInt(), chunks[16]!!.toInt()
            )
    }

    fun readDimFlag( chunks: List<String?>):Array<Flags>{

        val flags = arrayOf(Flags(), Flags(), Flags())
        flags[1].isMovedByUser = chunks[20]!!.toBoolean()
        flags[2].isMovedByUser = chunks[21]!!.toBoolean()
        return flags

    }

    fun readCParam( chunks:List<String?> ):ConnParam{
            return ConnParam(
            chunks[17]!!.toInt(),
            chunks[18]!!.toInt(),
            chunks[19]!!.toInt(),
            chunks[1]!!.toFloat()
            )
    }

    private fun readCsvHeaderLines(
        chunks: List<String?>,
        reader: BufferedReader,
    ): HeaderValues {
        val headerValues = HeaderValues(
        koujiname = parseLine(chunks,"koujiname"),
        rosenname = parseLine(readChunks(reader),"rosenname"),
        gyousyaname = parseLine(readChunks(reader),"gyousyaname"),
        zumennum = parseLine(readChunks(reader),"zumennum")
        )
        return headerValues
    }

    private fun parseLine(chunks: List<String?>, key: String): String {
        // Check if the first element matches the key and ensure there's a second element
        return if (chunks.firstOrNull() == key && chunks.size > 1) {
            chunks[1] ?: ""
        } else {
            ""
        }
    }

    fun buildDeductions(chunks: List<String>, dedlist: DeductionList, typeToInt: (String) -> Int, viewscale:Float):Boolean{
        if(chunks[0] == "Deduction"){
            dedlist.add(
                Deduction(
                    InputParameter(
                        chunks[2], chunks[6], chunks[1].toInt(),
                        chunks[3].toFloat(), chunks[4].toFloat(), 0f,
                        chunks[5].toInt(), typeToInt(chunks[6]),
                        PointXY(
                            chunks[8].toFloat(),
                            -chunks[9].toFloat()
                        ).scale(viewscale),
                        PointXY(
                            chunks[10].toFloat(),
                            -chunks[11].toFloat()
                        ).scale(viewscale)
                    )
                )
            )
            if(chunks[12].isNotEmpty()) dedlist.get(dedlist.size()).shapeAngle = chunks[12].toFloat()
            return true
        }
        return false
    }

    fun readListParameter(chunks: List<String>, trilist: TriangleList,setAllTextSize: (Float) -> Unit ):Boolean{
        when (chunks[0]) {
            "ListAngle" -> {
                trilist.angle = chunks[1].toFloat()
                return true
            }
            "ListScale" -> {
                trilist.setScale(PointXY(0f, 0f), chunks[1].toFloat())
                return true
            }
            "TextSize"  -> {
                setAllTextSize(chunks[1].toFloat())
                return true
            }
        }
        return false
    }

    private fun readChunks(reader: BufferedReader): List<String> {
        val line = reader.readLine()
        return line.split(",").map { it.trim() }
    }

}

data class ReturnValues(
    var trilist: TriangleList = TriangleList(),
    var dedlist: DeductionList = DeductionList(),
    var headerValues: HeaderValues = HeaderValues()
)
data class HeaderValues(
    var koujiname: String = "",
    var rosenname: String = "",
    var gyousyaname: String = "",
    var zumennum: String = ""
)
