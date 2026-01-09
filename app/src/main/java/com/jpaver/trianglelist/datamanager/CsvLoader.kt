package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Flags
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.setColor
import com.jpaver.trianglelist.setDimAligns
import com.jpaver.trianglelist.setPointNumber
import com.jpaver.trianglelist.viewmodel.InputParameter
import java.io.BufferedReader

/**
 * 三角形CSVのカラム定義
 *
 * 最小形式: NUMBER, LENGTH_A, LENGTH_B, LENGTH_C の4カラム
 * 接続形式: + PARENT_NUMBER, CONNECTION_TYPE で6カラム
 * 完全形式: 28カラム（レイアウト情報含む）
 *
 * ## 接続の仕様（重要）
 *
 * 三角形の辺には役割がある:
 * - A辺: 親との共有辺（接続辺）。子の生成時に親の辺と一致する
 * - B辺/C辺: 自由辺。他の三角形が接続可能
 *
 * CONNECTION_TYPEの値:
 * - -1: 独立（接続なし）
 * -  1: 親のB辺に接続
 * -  2: 親のC辺に接続
 *
 * A辺への接続は設計上存在しない（親のA辺は既にその親と共有済みのため）
 */
object TriangleColumn {
    // === 必須（図形生成に必要） ===
    /** 三角形の通し番号 */
    const val NUMBER = 0
    /** 辺Aの長さ（底辺、接続時は共有辺） */
    const val LENGTH_A = 1
    /** 辺Bの長さ */
    const val LENGTH_B = 2
    /** 辺Cの長さ */
    const val LENGTH_C = 3

    // === 接続情報 ===
    /** 接続先の三角形番号（-1 = 非接続） */
    const val PARENT_NUMBER = 4
    /** 接続タイプ（1=親のB辺, 2=親のC辺, -1=非接続）※A辺接続は存在しない */
    const val CONNECTION_TYPE = 5

    // === 表示情報 ===
    /** 三角形の名前（T1, T2など） */
    const val NAME = 6
    /** 点番号のX座標（表示位置） */
    const val POINT_NUMBER_X = 7
    /** 点番号のY座標（表示位置） */
    const val POINT_NUMBER_Y = 8
    /** 点番号をユーザーが手動移動したか */
    const val POINT_NUMBER_MOVED = 9
    /** 表示色（1-8のカラーパレット番号） */
    const val COLOR = 10

    // === 寸法線の配置 ===
    /** 水平寸法のA辺配置 */
    const val DIM_HORIZONTAL_A = 11
    /** 水平寸法のB辺配置 */
    const val DIM_HORIZONTAL_B = 12
    /** 水平寸法のC辺配置 */
    const val DIM_HORIZONTAL_C = 13
    /** 垂直寸法のA辺配置 */
    const val DIM_VERTICAL_A = 14
    /** 垂直寸法のB辺配置 */
    const val DIM_VERTICAL_B = 15
    /** 垂直寸法のC辺配置 */
    const val DIM_VERTICAL_C = 16

    // === 接続パラメータ詳細 ===
    /** 接続辺（どの辺で繋がるか） */
    const val CONN_PARAM_SIDE = 17
    /** 接続タイプ詳細 */
    const val CONN_PARAM_TYPE = 18
    /** 接続位置（Left/Center/Right） */
    const val CONN_PARAM_LCR = 19

    // === 寸法フラグ ===
    /** 寸法1がユーザー移動済みか */
    const val DIM_FLAG_1_MOVED = 20
    /** 寸法2がユーザー移動済みか */
    const val DIM_FLAG_2_MOVED = 21

    // === 角度・座標 ===
    /** 三角形の角度 */
    const val ANGLE = 22
    /** CA点のX座標 */
    const val POINT_CA_X = 23
    /** CA点のY座標 */
    const val POINT_CA_Y = 24
    /** ローカル角度 */
    const val ANGLE_LOCAL = 25

    // === 測点 ===
    /** 水平寸法の測点 */
    const val DIM_HORIZONTAL_S = 26
    /** 測点フラグがユーザー移動済みか */
    const val DIM_FLAG_S_MOVED = 27

    // === カラム数の定義 ===
    /** 独立三角形の最小カラム数 */
    const val MIN_REQUIRED = 4
    /** 接続情報付きのカラム数 */
    const val WITH_CONNECTION = 6
    /** 完全形式のカラム数 */
    const val FULL = 28
}

// === List<String>用の拡張関数 ===

/** 指定インデックスのInt値を取得（なければdefault） */
fun List<String>.intAt(index: Int, default: Int = 0): Int =
    getOrNull(index)?.toIntOrNull() ?: default

/** 指定インデックスのFloat値を取得（なければdefault） */
fun List<String>.floatAt(index: Int, default: Float = 0f): Float =
    getOrNull(index)?.toFloatOrNull() ?: default

/** 指定インデックスのBoolean値を取得（なければdefault） */
fun List<String>.boolAt(index: Int, default: Boolean = false): Boolean =
    getOrNull(index)?.lowercase()?.toBooleanStrictOrNull() ?: default

/** 指定インデックスのString値を取得（なければdefault） */
fun List<String>.stringAt(index: Int, default: String = ""): String =
    getOrNull(index)?.takeIf { it.isNotBlank() } ?: default

/** 指定インデックスが存在するか */
fun List<String>.has(index: Int): Boolean = size > index && getOrNull(index)?.isNotBlank() == true

class CsvLoader {

    fun parseCSV(
        reader: BufferedReader,
        showToast: (String) -> Unit,
        setAllTextSize: (Float) -> Unit,
        typeToInt: (String) -> Int,
        viewscale: Float,
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

        while (true) {
            val line = reader.readLine() ?: break
            val chunks = line.split(",").map { it.trim() }

            // チェック関数を使ってchunksが適切か確認
            if (!checkChunks(chunks)) {
                // 不適切なデータに対する処理、ログ記録、エラーメッセージ表示など
                println("Invalid input or insufficient data")
                continue
            }

            // リストの回転とかテキストサイズなどの状態
            if (readListParameter(chunks, trilist, setAllTextSize)) continue

            // 控除
            if (buildDeductions(chunks, dedlist, typeToInt, viewscale)) continue

            // 三角形
            buildTriangle(trilist, chunks)
        }

        return ReturnValues(trilist, dedlist, headerValues)
    }

    fun checkChunks(chunks: List<String?>): Boolean {

        chunks.forEach() { if (it == null) return false }

        return true
    }

    fun buildTriangle(
        trilist: TriangleList,
        chunks: List<String>
    ){
        // 最低4カラム必要（番号, 辺A, 辺B, 辺C）
        if (chunks.size < TriangleColumn.MIN_REQUIRED) return

        // 数値でない行（ListAngle等）はスキップ
        val number = chunks.intAt(TriangleColumn.NUMBER, -1)
        if (number < 0) return

        val lengthA = chunks.floatAt(TriangleColumn.LENGTH_A)
        val lengthB = chunks.floatAt(TriangleColumn.LENGTH_B)
        val lengthC = chunks.floatAt(TriangleColumn.LENGTH_C)
        val parent = chunks.intAt(TriangleColumn.PARENT_NUMBER, -1)
        val connectionType = chunks.intAt(TriangleColumn.CONNECTION_TYPE, -1)

        // 非接続（独立三角形）
        if (connectionType < 1) {
            trilist.add(
                Triangle(lengthA, lengthB, lengthC, PointXY(0f, 0f), 180f),
                true
            )
        }
        // 接続
        else {
            val ptri = trilist.getBy(parent)
            // 6カラム形式ではCONNECTION_TYPEから接続パラメータを生成
            val cp = if (chunks.has(TriangleColumn.CONN_PARAM_LCR)) {
                readCParamSafe(chunks)  // 完全形式
            } else {
                // 最小形式: CONNECTION_TYPE(1=B辺, 2=C辺)をそのまま使用
                // getParentPointByLCRはpbc=1(B辺), pbc=2(C辺)のみ対応
                ConnParam(
                    side = connectionType,      // 1→B辺, 2→C辺
                    type = 0,
                    lcr = 2,                    // Center（デフォルト）
                    lenA = lengthA              // 共有辺の長さ
                )
            }
            trilist.add(
                Triangle(ptri, cp, lengthB, lengthC),
                true
            )
        }

        finalizeBuildTriangle(chunks, trilist.getBy(trilist.size()))
    }

    // 0-3 ${mt.mynumber}, ${mt.lengthA_}, ${mt.lengthB_}, ${mt.lengthC_},
    // 4-5 ${mt.parentnumber}, ${mt.connectionType},
    // 6-9 ${mt.name}, ${pointnumber.x}, ${pointnumber.y}, ${mt.pointNumber.flag.isMovedByUser},
    // 10  ${mt.color_},
    // 11-13 ${mt.dim.horizontal.a}, ${mt.dim.horizontal.b}, ${mt.dim.horizontal.c},
    // 14-16 ${mt.dim.vertical.a}, ${mt.dim.vertical.b}, ${mt.dim.vertical.c},
    // 17-19 ${cp.side}, ${cp.type}, ${cp.lcr},
    // 20-21 ${mt.dim.flag[1].isMovedByUser}, ${mt.dim.flag[2].isMovedByUser},
    // 22-25 ${mt.angle}, ${mt.pointCA.x}, ${mt.pointCA.y}, ${mt.angleInLocal_}
    // 26-27 "${mt.dim.horizontal.s},${mt.dim.flagS.isMovedByUser}"

    //i  0,1,2,3, 4, 5,6,7    ,8     ,9    ,10,11,12,13,14,15,16,17,18,19,20   ,21   ,22     ,23 ,24 ,25
    //ex 1,6,1,1,-1,-1, ,4.060,-2.358,false,4 ,0 ,0 ,0 , 1, 1, 3, 0, 0, 0,false,false,-268.70,0.0,0.0,-448.70


    fun finalizeBuildTriangle(chunks: List<String>, mt: Triangle){
        // 接続タイプ
        if (chunks.has(TriangleColumn.CONNECTION_TYPE)) {
            mt.connectionSide = chunks.intAt(TriangleColumn.CONNECTION_TYPE, -1)
        }

        // 名前
        if (chunks.has(TriangleColumn.NAME)) {
            mt.name = chunks.stringAt(TriangleColumn.NAME)
        }

        // 点番号位置（ユーザー移動時のみ）
        if (chunks.boolAt(TriangleColumn.POINT_NUMBER_MOVED)) {
            mt.setPointNumber(
                PointXY(
                    chunks.floatAt(TriangleColumn.POINT_NUMBER_X),
                    chunks.floatAt(TriangleColumn.POINT_NUMBER_Y)
                ),
                true
            )
        }

        // 色
        if (chunks.has(TriangleColumn.COLOR)) {
            mt.setColor(chunks.intAt(TriangleColumn.COLOR, 4))
        }

        // 寸法配置
        if (chunks.has(TriangleColumn.DIM_VERTICAL_C)) {
            mt.setDimAligns(
                chunks.intAt(TriangleColumn.DIM_HORIZONTAL_A),
                chunks.intAt(TriangleColumn.DIM_HORIZONTAL_B),
                chunks.intAt(TriangleColumn.DIM_HORIZONTAL_C),
                chunks.intAt(TriangleColumn.DIM_VERTICAL_A),
                chunks.intAt(TriangleColumn.DIM_VERTICAL_B),
                chunks.intAt(TriangleColumn.DIM_VERTICAL_C)
            )
        }

        // 接続パラメータ
        if (chunks.has(TriangleColumn.CONN_PARAM_LCR)) {
            mt.cParam_ = readCParamSafe(chunks)
        }

        // 寸法フラグ
        if (chunks.has(TriangleColumn.DIM_FLAG_2_MOVED)) {
            mt.dim.flag = readDimFlagSafe(chunks)
        }

        // 測点
        if (chunks.has(TriangleColumn.DIM_FLAG_S_MOVED)) {
            mt.dim.horizontal.s = chunks.intAt(TriangleColumn.DIM_HORIZONTAL_S)
            mt.dim.flagS.isMovedByUser = chunks.boolAt(TriangleColumn.DIM_FLAG_S_MOVED)
        }
    }

    fun readDimSokuten(chunks: List<String>, mt: Triangle){
        mt.dim.horizontal.s = chunks[26].toInt()
        mt.dim.flagS.isMovedByUser = chunks[27].toBoolean()
    }

    fun readPointNumber(chunks: List<String>, mt: Triangle){
        mt.setPointNumber(
            PointXY(
                chunks[7].toFloat(),
                chunks[8].toFloat()
            ),
            chunks[9].toBoolean()
        )
    }

    fun readDimAligns(chunks: List<String>, triangle: Triangle){
            triangle.setDimAligns(
                chunks[11].toInt(), chunks[12].toInt(), chunks[13].toInt(),
                chunks[14].toInt(), chunks[15].toInt(), chunks[16].toInt()
            )
    }

    fun readDimFlagSafe(chunks: List<String>): Array<Flags> {
        val flags = arrayOf(Flags(), Flags(), Flags())
        flags[1].isMovedByUser = chunks.boolAt(TriangleColumn.DIM_FLAG_1_MOVED)
        flags[2].isMovedByUser = chunks.boolAt(TriangleColumn.DIM_FLAG_2_MOVED)
        return flags
    }

    fun readCParamSafe(chunks: List<String>): ConnParam {
        return ConnParam(
            chunks.intAt(TriangleColumn.CONN_PARAM_SIDE),
            chunks.intAt(TriangleColumn.CONN_PARAM_TYPE),
            chunks.intAt(TriangleColumn.CONN_PARAM_LCR),
            chunks.floatAt(TriangleColumn.LENGTH_A)
        )
    }

    private fun readCsvHeaderLines(
        chunks: List<String>,
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

    fun readListParameter(chunks: List<String>, trilist: TriangleList, setAllTextSize: (Float) -> Unit ):Boolean{
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
