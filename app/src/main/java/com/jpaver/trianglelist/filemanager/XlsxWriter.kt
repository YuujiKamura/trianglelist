package com.jpaver.trianglelist.filemanager

import com.jpaver.trianglelist.DeductionList
import com.jpaver.trianglelist.EditList
import com.jpaver.trianglelist.TriangleList
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFHyperlink
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.io.OutputStream


class XlsxWriter() {
    val wb = XSSFWorkbook()
    val sheet = wb.createSheet()
    val rowStart = 2
    val rowsizer = 1.25f
    val format1 = wb.createDataFormat()
    val styleC = wb.createCellStyle()
    val styleTitle = wb.createCellStyle()
    val styleDigit = wb.createCellStyle()
    val styleCRed = wb.createCellStyle()
    val styleDigitRed = wb.createCellStyle()


    //val rowTitle = sheet.createRow( rowStart - 1 )
    //val rowRosenmei = sheet.createRow( rowStart )
    val rowHeader = sheet.createRow( rowStart + 1 )
    val fontRed = wb.createFont()
    val fontAuto = wb.createFont()

    init {
        val border = BorderStyle.THIN
        val borderless = BorderStyle.NONE
        val alignC = HorizontalAlignment.CENTER

        styleDigit.setDataFormat(format1.getFormat("0.00"))
        setStyle( styleDigit, alignC, border )
        setStyle( styleC, alignC, border )
        setStyle( styleTitle, alignC, borderless )

        fontRed.color = IndexedColors.RED.index
        fontAuto.color = IndexedColors.BLACK.index

        styleDigitRed.setDataFormat(format1.getFormat("0.00"))
        setStyle( styleDigitRed, alignC, border )
        setStyle( styleCRed, alignC, border )
        styleCRed.setFont(fontRed)
        styleDigitRed.setFont(fontRed)

        // 複数の列の幅を設定する関数を呼び出す
        setColumnWidths(sheet, intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf( 2, 12, 8, 8, 8, 12 ))

    }

    // 複数の列の幅を設定する関数
    fun setColumnWidths(sheet: Sheet, columnIndexes: IntArray, columnWidths: IntArray) {
        for (i in columnIndexes.indices) {
            sheet.setColumnWidth(columnIndexes[i], 256 * columnWidths[i])
        }
    }

    fun setColumnHeights(sheet: Sheet, rowIndexes: IntArray, rowWidths: IntArray) {
        for (i in rowIndexes.indices) {
            sheet.setColumnWidth(rowIndexes[i], 256 * rowWidths[i])
        }
    }

    fun setStyle( style: CellStyle, align: HorizontalAlignment, border: BorderStyle ){
        style.alignment = align
        style.borderBottom = border
    }

    fun writeRow(rownum: Int, string: String, style: CellStyle, link: XSSFHyperlink? = null ){
        val row = sheet.createRow( rownum )
        row.heightInPoints  = rowHeader.heightInPoints * rowsizer

        val Cell = row.createCell(3)
        Cell.setCellValue( string )

        Cell.setCellStyle(style)

        if(link != null ) Cell.hyperlink = link

    }

    fun writeRow(  rownum: Int, style: CellStyle, vararg strings: String ){

        val row = sheet.createRow( rownum )
        row.heightInPoints  = rowHeader.heightInPoints * rowsizer

        strings.forEachIndexed() { index, _ ->
            val cells = row.createCell( index + 1 )
            cells.setCellValue(strings[index])

            cells.setCellStyle(style)
        }

    }

    fun writeBody(rownum: Int, list: TriangleList){
        for( i in 0 until list.size() ){
            val rowBody = sheet.createRow(rownum + i )
            rowBody.heightInPoints  = rowHeader.heightInPoints * rowsizer
            val rn = rownum + 1 + i
            val lsum = "(0.5*(C" + rn + "+D" + rn + "+E" + rn + "))"
            val lenAround = Math.round( list.get(i+1).lengthA.toDouble() * 100 ) * 0.01
            val lenBround = Math.round( list.get(i+1).lengthB.toDouble() * 100 ) * 0.01
            val lenCround = Math.round( list.get(i+1).lengthC.toDouble() * 100 ) * 0.01
            val cells = arrayOf(
                rowBody.createCell(1),
                rowBody.createCell(2),
                rowBody.createCell(3),
                rowBody.createCell(4),
                rowBody.createCell(5)
            )
            cells[0].setCellValue( list.get(i+1).myNumber_.toDouble() )
            cells[1].setCellValue( lenAround )
            cells[2].setCellValue( lenBround )
            cells[3].setCellValue( lenCround )
            cells[4].setCellFormula( "ROUND((("+ lsum + "*(" + lsum + "-C" + rn + ")*(" + lsum + "-D" + rn + ")*(" + lsum + "-E" + rn + "))^0.5),2)" )

            cells[0].cellType = CellType.NUMERIC
            cells[0].setCellStyle(styleC)
            cells[1].setCellStyle(styleDigit)
            cells[2].setCellStyle(styleDigit)
            cells[3].setCellStyle(styleDigit)
            cells[4].setCellStyle(styleDigit)
        }
    }

    fun writeBody(rownum: Int, list: DeductionList){
        for( i in 0 until list.size() ){
            val rowBody = sheet.createRow(rownum + i )
            rowBody.heightInPoints  = rowHeader.heightInPoints * rowsizer
            val rowcounter = rownum + 1 + i
            val element = list.get(i+1)
            val lenXround = Math.round( element.lengthX.toDouble() * 100 ) * 0.01
            val lenYround = Math.round( element.lengthY.toDouble() * 100 ) * 0.01
            val formula = arrayOf(
                "-C" + rowcounter + "*D" + rowcounter,
                "-(C" + rowcounter + "*0.5)*(C" + rowcounter + "*0.5)*3.14"
            )

            val cells = arrayOf(
                rowBody.createCell(1),
                rowBody.createCell(2),
                rowBody.createCell(3),
                rowBody.createCell(4),
                rowBody.createCell(5)
            )
            cells[0].setCellValue( element.getNumNameCount() )
            cells[1].setCellValue( lenXround )
            cells[2].setCellValue( lenYround )
            cells[3].setCellValue( element.typestring )
            cells[4].setCellFormula( "ROUND(" + formula[element.typenum] + ",2)" )


            cells[0].setCellStyle(styleCRed)
            cells[1].setCellStyle(styleDigitRed)
            cells[2].setCellStyle(styleDigitRed)
            cells[3].setCellStyle(styleCRed)
            cells[4].setCellStyle(styleDigitRed)
        }
    }

    fun writeGoukei(rownum: Int, list: EditList, string: String, formulaid: Int, style: CellStyle ){
        val sumstart = rownum + 1
        val sumend = rownum + list.size()
        val beforesyoukei = sumstart-4
        val nextsyoukei = sumend
        val formula = arrayOf(
            "sum(" + "F" + sumstart + ":F" + sumend + ")",
            "F" + beforesyoukei + "+F" + nextsyoukei
        )

        val rowSum = sheet.createRow( sumend )
        rowSum.heightInPoints = rowHeader.heightInPoints * rowsizer

        // 複数のセルを一度に生成する
        createCellValues(rowSum, intArrayOf(1,2,3,4), arrayOf(string,"","",""), style)

        val GCell5 = rowSum.createCell(5)
        GCell5.setCellFormula( formula[formulaid]  )
        GCell5.setCellStyle(style)
    }

    private fun createCellValues(
        rowSum: XSSFRow,
        columnIndexes: IntArray,
        stringArray: Array<String>,
        style: CellStyle
    ) {
        for (i in columnIndexes.indices) {
            val GCell1 = rowSum.createCell(columnIndexes[i])
            GCell1.setCellValue(stringArray[i])
            GCell1.setCellStyle(style)
        }
    }

    fun writeDedList( rowStartD: Int, dedlist: DeductionList){

        writeRow( rowStartD, "面積控除", styleCRed )
        writeRow( rowStartD+1, styleCRed, "名称","寸法１","寸法２","形状","面積" )
        writeBody( rowStartD+2, dedlist )
        writeGoukei( rowStartD+2, dedlist, "小計(2)", 0, styleCRed )
        writeGoukei( rowStartD+3, dedlist, "合計(1)+(2)", 1, styleC )

    }

    fun write(content: OutputStream, trilist: TriangleList, dedlist: DeductionList, rosenmei: String ){
        try {
            writeRow( 1, "面 積 計 算 書", styleTitle )
            writeRow( 2, rosenmei, styleTitle )
            writeRow( 3, styleC, "番号","辺長A","辺長B","辺長C","面積" )

            writeBody( 4, trilist )
            writeGoukei( 4, trilist, "小計(1)", 0, styleC )

            if(dedlist.size()>0) writeDedList(4 + trilist.size() + 1, dedlist )


            val ch = wb.getCreationHelper()
            val link = ch.createHyperlink(HyperlinkType.URL)
            val endrow = 4 + trilist.size() + 1 + dedlist.size() + 4
            link.setAddress("http://poi.apache.org")
            writeRow( endrow + 1,"( This .xlsx file was exported by Apache POI )", styleTitle )
            writeRow( endrow + 2,"( http://poi.apache.org )", styleTitle, link )


            // セル範囲の外枠を描く関数を呼び出す
            //
            // drawCellBorders(sheet, 3, 1, endrow-1, 5, BorderStyle.THIN)

            wb.write( content )
            wb.close()
            content.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * 指定されたセル範囲の外枠を指定された罫線スタイルで描画します。
     *
     * @param sheet 外枠を描画するシートオブジェクト
     * @param startRow 範囲の開始行番号（0ベース）
     * @param startColumn 範囲の開始列番号（0ベース）
     * @param endRow 範囲の終了行番号（0ベース）
     * @param endColumn 範囲の終了列番号（0ベース）
     * @param borderStyle 外枠の罫線スタイル
     *
     * @throws IllegalArgumentException 無効な引数が指定された場合にスローされます。
     *                                   無効な引数としては、
     *                                   ・シートオブジェクトがnullの場合
     *                                   ・開始行番号または開始列番号が負数の場合
     *                                   ・終了行番号または終了列番号が開始行番号または開始列番号より小さい場合
     * @throws IllegalStateException    シートオブジェクトの設定が無効な場合にスローされます。
     *                                   例えば、シートオブジェクトの作成に失敗した場合など。
     */
    fun drawCellBorders(
        sheet: Sheet,
        startRow: Int,
        startColumn: Int,
        endRow: Int,
        endColumn: Int,
        borderStyle: BorderStyle
    ) {
        require(sheet != null) { "sheet must not be null" }
        require(startRow >= 0) { "startRow must be greater than or equal to 0" }
        require(startColumn >= 0) { "startColumn must be greater than or equal to 0" }
        require(endRow >= startRow) { "endRow must be greater than or equal to startRow" }
        require(endColumn >= startColumn) { "endColumn must be greater than or equal to startColumn" }

        // セル範囲のCellStyleを取得する
        val cellStyle: CellStyle = sheet.getRow(startRow).getCell(startColumn).cellStyle

        // セル範囲の上部の外枠を描く
        for (col in startColumn..endColumn) {
            val row = sheet.getRow(startRow)
            val cell: Cell = row.getCell(col)?: row.createCell(col)
            val currentStyle: CellStyle = if (cell.cellStyle != null) cell.cellStyle else cellStyle
            currentStyle.borderTop = borderStyle
            cell.cellStyle = currentStyle
        }

        // セル範囲の下部の外枠を描く
        for (col in startColumn..endColumn) {
            val row = sheet.getRow(endRow)
            val cell: Cell = row.getCell(col)?: row.createCell(col)
            val currentStyle: CellStyle = if (cell.cellStyle != null) cell.cellStyle else cellStyle
            currentStyle.borderBottom = borderStyle
            cell.cellStyle = currentStyle
        }

        // セル範囲の左部の外枠を描く
        for (rowLeft in startRow..endRow) {
            val row = sheet.getRow(rowLeft)
            val cell: Cell = row.getCell(startColumn)?: row.createCell(startColumn)
            val currentStyle: CellStyle = if (cell.cellStyle != null) cell.cellStyle else cellStyle
            currentStyle.borderLeft = borderStyle
            cell.cellStyle = currentStyle
        }

        // セル範囲の右部の外枠を描く
        for (rowRight in startRow..endRow) {
            val row = sheet.getRow(rowRight)
            val cell: Cell = row.getCell(endColumn)?: row.createCell(endColumn)
            val currentStyle: CellStyle = if (cell.cellStyle != null) cell.cellStyle else cellStyle
            currentStyle.borderRight = borderStyle
            cell.cellStyle = currentStyle
        }
    }
}