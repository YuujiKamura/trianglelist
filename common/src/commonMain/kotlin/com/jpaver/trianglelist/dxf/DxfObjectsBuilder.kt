package com.jpaver.trianglelist.dxf

/**
 * OBJECTS セクション（+ EOF）を出力する。
 * app/ObjectsBuilder.kt から移植。BufferedWriter → Appendable。
 */
class DxfObjectsBuilder(private val h: DxfHandleGen) {

    fun write(
        w: Appendable,
        paper: DxfPaper,
        printScale: DxfPrintScale,
        blkPaper: String = "32"
    ) {
        val root = "C"

        val colorDict = h.new()
        val groupDict = h.new()
        val layoutDict = h.new()
        val materialDict = h.new()
        val mlineDict = h.new()
        val plotDict = h.new()
        val plotStyleDict = h.new()
        val tableStyleDict = h.new()
        val visualStyleDict = h.new()
        val varDict = h.new()
        val dwgPropsDict = h.new()

        val plotSettings = h.new()
        // TABLES/BLOCK_RECORD(340) が固定で 35/39 を参照しているため、LAYOUT 側を合わせる
        val layout = "35"
        val modelLayout = "39"

        val materialByBlock = h.new()
        val materialByLayer = h.new()
        val materialGlobal = h.new()

        val vs2dWireframe = h.new()
        val vs3dWireframe = h.new()
        val vs3dHidden = h.new()
        val vsRealistic = h.new()
        val vsConceptual = h.new()

        fun p(gc: Int, v: Any) {
            w.appendLine("%3d".format(gc))
            w.appendLine(v.toString())
        }

        p(0, "SECTION"); p(2, "OBJECTS")

        p(0, "DICTIONARY"); p(5, root); p(330, 0); p(100, "AcDbDictionary"); p(281, 1)
        p(3, "ACAD_COLOR"); p(350, colorDict)
        p(3, "ACAD_GROUP"); p(350, groupDict)
        p(3, "ACAD_LAYOUT"); p(350, layoutDict)
        p(3, "ACAD_MATERIAL"); p(350, materialDict)
        p(3, "ACAD_MLINESTYLE"); p(350, mlineDict)
        p(3, "ACAD_PLOTSETTINGS"); p(350, plotDict)
        p(3, "ACAD_PLOTSTYLENAME"); p(350, plotStyleDict)
        p(3, "ACAD_TABLESTYLE"); p(350, tableStyleDict)
        p(3, "ACAD_VISUALSTYLE"); p(350, visualStyleDict)
        p(3, "AcDbVariableDictionary"); p(350, varDict)
        p(3, "DWGPROPS"); p(350, dwgPropsDict)

        p(0, "DICTIONARY"); p(5, colorDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)

        p(0, "DICTIONARY"); p(5, groupDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)

        p(0, "DICTIONARY"); p(5, layoutDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)
        p(3, "Model"); p(350, modelLayout)
        p(3, "Layout1"); p(350, layout)

        p(0, "DICTIONARY"); p(5, materialDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)
        p(3, "ByBlock"); p(350, materialByBlock)
        p(3, "ByLayer"); p(350, materialByLayer)
        p(3, "Global"); p(350, materialGlobal)

        p(0, "DICTIONARY"); p(5, mlineDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)

        p(0, "DICTIONARY"); p(5, plotDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)
        p(3, "${paper.name}_${printScale.model.toInt()}_${printScale.paper.toInt()}"); p(350, plotSettings)

        val normalPlotStyle = h.new()
        p(0, "ACDBDICTIONARYWDFLT"); p(5, plotStyleDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)
        p(3, "Normal"); p(350, normalPlotStyle)
        p(100, "AcDbDictionaryWithDefault")
        p(340, normalPlotStyle)

        p(0, "DICTIONARY"); p(5, tableStyleDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)

        p(0, "DICTIONARY"); p(5, visualStyleDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)
        p(3, "2dWireframe"); p(350, vs2dWireframe)
        p(3, "3D Hidden"); p(350, vs3dHidden)
        p(3, "3dWireframe"); p(350, vs3dWireframe)
        p(3, "Conceptual"); p(350, vsConceptual)
        p(3, "Realistic"); p(350, vsRealistic)

        p(0, "DICTIONARY"); p(5, varDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)

        p(0, "DICTIONARY"); p(5, dwgPropsDict)
        p(102, "{ACAD_REACTORS"); p(330, root); p(102, "}")
        p(330, root); p(100, "AcDbDictionary"); p(281, 1)

        writeMaterial(w, materialByBlock, materialDict, "ByBlock")
        writeMaterial(w, materialByLayer, materialDict, "ByLayer")
        writeMaterial(w, materialGlobal, materialDict, "Global")

        writeVisualStyle(w, vs2dWireframe, visualStyleDict, "2dWireframe", 6)
        writeVisualStyle(w, vs3dWireframe, visualStyleDict, "3dWireframe", 4)
        writeVisualStyle(w, vs3dHidden, visualStyleDict, "3D Hidden", 6)
        writeVisualStyle(w, vsRealistic, visualStyleDict, "Realistic", 8)
        writeVisualStyle(w, vsConceptual, visualStyleDict, "Conceptual", 9)

        p(0, "PLOTSTYLE"); p(5, normalPlotStyle)
        p(102, "{ACAD_REACTORS"); p(330, plotStyleDict); p(102, "}")
        p(330, plotStyleDict); p(100, "AcDbPlotStyle")
        p(1, "Normal"); p(70, 0); p(290, 1)

        val (num, den) = printScale.model to printScale.paper

        p(0, "PLOTSETTINGS"); p(5, plotSettings)
        p(102, "{ACAD_REACTORS"); p(330, plotDict); p(102, "}")
        p(330, plotDict)
        p(100, "AcDbPlotSettings")
        p(1, "${paper.name}_${num.toInt()}_${den.toInt()}")
        p(2, "none_device")
        p(4, paper.isoName)
        p(6, "")
        p(40, 0.0); p(41, 0.0); p(42, 0.0); p(43, 0.0)
        p(44, paper.height); p(45, paper.width)
        p(46, 0.0); p(47, 0.0); p(48, 0.0); p(49, 0.0)
        p(140, 0.0); p(141, 0.0)
        p(142, num); p(143, den)
        p(70, 676)
        p(72, 1); p(73, 1); p(74, 5)
        p(7, ""); p(75, 16); p(76, 0)
        p(77, 2); p(78, 300)
        p(147, 1.0); p(148, 0.0); p(149, 0.0)

        // Model LAYOUT (inherits AcDbPlotSettings → AcDbLayout)
        p(0, "LAYOUT"); p(5, modelLayout)
        p(102, "{ACAD_REACTORS"); p(330, layoutDict); p(102, "}")
        p(330, layoutDict)
        p(100, "AcDbPlotSettings")
        p(1, ""); p(2, "none_device"); p(4, ""); p(6, "")
        p(40, 0.0); p(41, 0.0); p(42, 0.0); p(43, 0.0)
        p(44, 0.0); p(45, 0.0)
        p(46, 0.0); p(47, 0.0); p(48, 0.0); p(49, 0.0)
        p(140, 0.0); p(141, 0.0); p(142, 1.0); p(143, 1.0)
        p(70, 1712); p(72, 0); p(73, 0); p(74, 0)
        p(7, ""); p(75, 0); p(76, 0); p(77, 2); p(78, 300)
        p(147, 1.0); p(148, 0.0); p(149, 0.0)
        p(100, "AcDbLayout")
        p(1, "Model"); p(70, 1); p(71, 0)
        p(10, 0.0); p(20, 0.0)
        p(11, paper.width * den); p(21, paper.height * den)
        p(12, 0.0); p(22, 0.0); p(32, 0.0)
        p(14, 0.0); p(24, 0.0); p(34, 0.0)
        p(15, paper.width * den); p(25, paper.height * den); p(35, 0.0)
        p(146, 0.0); p(13, 0.0); p(23, 0.0); p(33, 0.0)
        p(16, 1.0); p(26, 0.0); p(36, 0.0)
        p(17, 0.0); p(27, 1.0); p(37, 0.0)
        p(76, 0)
        p(330, "36")

        // Layout1 LAYOUT
        p(0, "LAYOUT"); p(5, layout)
        p(102, "{ACAD_REACTORS"); p(330, layoutDict); p(102, "}")
        p(330, layoutDict)
        p(100, "AcDbPlotSettings")
        p(1, "${paper.name}_${num.toInt()}_${den.toInt()}")
        p(2, "none_device"); p(4, paper.isoName); p(6, "")
        p(40, 0.0); p(41, 0.0); p(42, 0.0); p(43, 0.0)
        p(44, paper.height); p(45, paper.width)
        p(46, 0.0); p(47, 0.0); p(48, 0.0); p(49, 0.0)
        p(140, 0.0); p(141, 0.0); p(142, num); p(143, den)
        p(70, 676); p(72, 1); p(73, 1); p(74, 5)
        p(7, ""); p(75, 16); p(76, 0); p(77, 2); p(78, 300)
        p(147, 1.0); p(148, 0.0); p(149, 0.0)
        p(100, "AcDbLayout")
        p(1, "Layout1"); p(70, 1); p(71, 1)
        p(10, 0.0); p(20, 0.0)
        p(11, paper.width); p(21, paper.height)
        p(12, 0.0); p(22, 0.0); p(32, 0.0)
        p(14, 0.0); p(24, 0.0); p(34, 0.0)
        p(15, paper.width); p(25, paper.height); p(35, 0.0)
        p(146, 0.0); p(13, 0.0); p(23, 0.0); p(33, 0.0)
        p(16, 1.0); p(26, 0.0); p(36, 0.0)
        p(17, 0.0); p(27, 1.0); p(37, 0.0)
        p(76, 0)
        p(330, blkPaper)
        p(340, plotSettings)

        p(0, "ENDSEC")
        p(0, "EOF")
    }

    private fun writeMaterial(w: Appendable, handle: String, ownerDict: String, name: String) {
        fun p(gc: Int, v: Any) {
            w.appendLine("%3d".format(gc))
            w.appendLine(v.toString())
        }
        p(0, "MATERIAL"); p(5, handle)
        p(102, "{ACAD_REACTORS"); p(330, ownerDict); p(102, "}")
        p(330, ownerDict); p(100, "AcDbMaterial")
        p(1, name)
        p(72, 1); p(94, 127); p(282, 1)
    }

    private fun writeVisualStyle(w: Appendable, handle: String, ownerDict: String, name: String, type: Int) {
        fun p(gc: Int, v: Any) {
            w.appendLine("%3d".format(gc))
            w.appendLine(v.toString())
        }
        p(0, "VISUALSTYLE"); p(5, handle)
        p(102, "{ACAD_REACTORS"); p(330, ownerDict); p(102, "}")
        p(330, ownerDict); p(100, "AcDbVisualStyle")
        p(2, name); p(70, type)
        p(177, 2); p(291, 0); p(71, 2); p(176, 1); p(293, 1)
        p(73, 0); p(173, 0); p(290, 0); p(174, 0); p(292, 0); p(175, 1)
    }
}
