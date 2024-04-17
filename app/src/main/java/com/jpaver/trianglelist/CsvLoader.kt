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
        val headerValues: HeaderValues?
        val line1 = reader.readLine() ?: return null
        val chunks1 = line1.split(",").map { it.trim() }

        if (chunks1[0] != "koujiname") {
            showToast("It's not supported file.")
            return null
        }
        headerValues = readCsvHeaderLines(chunks1, reader)
        rosennameEditText.setText(headerValues.rosenname)

        while (true) {
            val line = reader.readLine() ?: break
            val chunks = line.split(",").map { it.trim() }

            // リストの回転とかテキストサイズなどの状態
            if (readListParameter(chunks, trilist, setAllTextSize)) continue

            // 控除
            if (buildDeductions(chunks, dedlist, typeToInt, viewscale)) continue

            // 三角形
            buildTriangle2(addTriangle, trilist, chunks)
        }

        return ReturnValues(trilist, dedlist, headerValues)
    }

    fun buildTriangle2(
        addTriangle: (TriangleList, List<String?>, PointXY, Float) -> Unit,
        trilist: TriangleList,
        chunks: List<String>
    ){
        val connectiontype = chunks[5].toInt()

        //非接続というか１番目
        if( connectiontype == -1 ){
            val pt = PointXY(0f, 0f)
            addTriangle(trilist, chunks, pt, 180f )
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

    //i  0,1,2,3, 4, 5,6,7    ,8     ,9    ,10,11,12,13,14,15,16,17,18,19,20   ,21   ,22     ,23 ,24 ,25
    //ex 1,6,1,1,-1,-1, ,4.060,-2.358,false,4 ,0 ,0 ,0 , 1, 1, 3, 0, 0, 0,false,false,-268.70,0.0,0.0,-448.70
    // 0-3 ${mt.mynumber}, ${mt.lengthA_}, ${mt.lengthB_}, ${mt.lengthC_},
    // 4-5 ${mt.parentnumber}, ${mt.connectionType},
    // 6-9 ${mt.name}, ${pointnumber.x}, ${pointnumber.y}, ${mt.pointNumber.flag.isMovedByUser},
    // 10  ${mt.color_},
    // 11-13 ${mt.dim.horizontal.a}, ${mt.dim.horizontal.b}, ${mt.dim.horizontal.c},
    // 14-16 ${mt.dim.vertical.a}, ${mt.dim.vertical.b}, ${mt.dim.vertical.c},
    // 17-19 ${cp.side}, ${cp.type}, ${cp.lcr},
    // 20-21 ${mt.dim.flag[1].isMovedByUser}, ${mt.dim.flag[2].isMovedByUser},
    // 22-25 ${mt.angle}, ${mt.pointCA.x}, ${mt.pointCA.y}, ${mt.angleInLocal_}

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
