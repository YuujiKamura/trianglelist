package com.jpaver.trianglelist.filemanager

import com.jpaver.trianglelist.DeductionList
import com.jpaver.trianglelist.EditList
import com.jpaver.trianglelist.TriangleList
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFHyperlink
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
    val styleDigit = wb.createCellStyle()
    val styleCRed = wb.createCellStyle()
    val styleDigitRed = wb.createCellStyle()


    val rowTitle = sheet.createRow( rowStart - 1 )
    val rowRosenmei = sheet.createRow( rowStart )
    val rowHeader = sheet.createRow( rowStart + 1 )
    val fontRed = wb.createFont()
    val fontAuto = wb.createFont()

    init {
        val border = BorderStyle.THIN

        styleDigit.setDataFormat(format1.getFormat("0.00"))
        setStyle( styleDigit, HorizontalAlignment.CENTER, border )
        setStyle( styleC, HorizontalAlignment.CENTER, border )

        fontRed.color = IndexedColors.RED.index
        fontAuto.color = IndexedColors.BLACK.index

        styleDigitRed.setDataFormat(format1.getFormat("0.00"))
        setStyle( styleDigitRed, HorizontalAlignment.CENTER, border )
        setStyle( styleCRed, HorizontalAlignment.CENTER, border )
        styleCRed.setFont(fontRed)
        styleDigitRed.setFont(fontRed)

    }

    fun setStyle( style: CellStyle, align: HorizontalAlignment, border: BorderStyle ){
        style.alignment = HorizontalAlignment.CENTER
        style.borderBottom = BorderStyle.THIN
    }

    fun writeRow(rownum: Int, string: String, style: CellStyle, link: XSSFHyperlink? = null ){
        val row = sheet.createRow( rownum )
        row.heightInPoints *= rowsizer

        val Cell = row.createCell(3)
        Cell.setCellValue( string )

        Cell.setCellStyle(style)

        if(link != null ) Cell.hyperlink = link

    }

    fun writeRow(  rownum: Int, style: CellStyle, vararg strings: String ){

        val row = sheet.createRow( rownum )
        row.heightInPoints *= rowsizer

        strings.forEachIndexed() { index, _ ->
            val cells = row.createCell( index + 1 )
            cells.setCellValue(strings[index])

            cells.setCellStyle(style)
        }

    }

    fun writeBody(rownum: Int, list: TriangleList ){
        for( i in 0 until list.size() ){
            val rowBody = sheet.createRow(rownum + i )
            rowBody.heightInPoints *= rowsizer
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
            rowBody.heightInPoints *= rowsizer
            val rowcounter = rownum + 1 + i
            val element = list.get(i+1)
            val lenXround = Math.round( element.lengthX.toDouble() * 100 ) * 0.01
            var lenYround = Math.round( element.lengthY.toDouble() * 100 ) * 0.01
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
            cells[0].setCellValue( element.name )
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
        val GCell1 = rowSum.createCell(1)
        val GCell5 = rowSum.createCell(5)

        GCell1.setCellValue( string )
        GCell1.setCellStyle(style)
        GCell5.setCellFormula( formula[formulaid]  )
        GCell5.setCellStyle(style)
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
            writeRow( 1, "面 積 計 算 書", styleC )
            writeRow( 2, rosenmei, styleC )
            writeRow( 3, styleC, "番号","辺長A","辺長B","辺長C","面積" )

            writeBody( 4, trilist )
            writeGoukei( 4, trilist, "小計(1)", 0, styleC )

            if(dedlist.size()>0) writeDedList(4 + trilist.size() + 1, dedlist )


            val ch = wb.getCreationHelper()
            val link = ch.createHyperlink(HyperlinkType.URL);
            link.setAddress("http://poi.apache.org")
            writeRow( 4 + trilist.size() + 1 + dedlist.size() + 5,"( This .xlsx file was exported by Apache POI )", styleC )
            writeRow( 4 + trilist.size() + 1 + dedlist.size() + 6,"( http://poi.apache.org )", styleC, link )


            wb.write( content )
            wb.close()
            content.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}