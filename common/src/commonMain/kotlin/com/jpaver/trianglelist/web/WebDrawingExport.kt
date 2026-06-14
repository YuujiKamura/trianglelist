package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.datamanager.DxfFileWriter
import com.jpaver.trianglelist.datamanager.SfcWriter
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.viewmodel.TitleParamStr

/**
 * Web 段階2b (insight #60, task #10): CSV 文字列 → DXF/SFC ファイル内容 String の組み立て。
 *
 * CSV→TriangleList は段階1 の WebCsvReader (= app CsvLoader.kt:219-234 の最小形式と同経路) を
 * 再利用し、writer の初期化列は app の golden テスト
 * (DxfDimensionLayoutGoldenTest.newWriter / SfcDimensionLayoutGoldenTest.writeSfcString) を
 * 忠実に再現する。web に view は無いので、MainActivity.saveDXF/saveSFC の view 依存値
 * (myview.textSize 由来の textscale_ 等) ではなく golden fixture を産んだ初期化列が正 ——
 * 「app と web で同じ図面ファイルが出る」ことを golden 同値テスト (desktopTest) で機械固定する。
 *
 * SJIS バイト化は platform 側 (JS は encoding-japanese、app は OutputStreamWriter) の仕事。
 * ここは String まで。
 */
object WebDrawingExport {

    data class CsvHeader(
        val koujiname: String,
        val rosenname: String,
        val gyousyaname: String,
        val zumennum: String,
    )

    /** app CsvLoader の完全形式 (readCsvHeaderLines) のラベル。この順で 4 行 */
    private val HEADER_LABELS = listOf("koujiname", "rosenname", "gyousyaname", "zumennum")

    /**
     * CSV のメタ行ラベル (CsvCodec.parse:79-87 が読む語彙)。ヘッダ 4 行が空のとき、
     * これらをヘッダ値として誤読しない — 誤読すると図面番号欄に "TextSize, 30"、
     * 施工者欄に "ListAngle, 0" が印字される (2026-06-12 図面枠の画面目視で発見)
     */
    private val META_LABELS = setOf("ListAngle", "TextSize", "Deduction")

    /**
     * CSV ヘッダ行 → 工事名/路線名/業者名/図面番号。
     * - web 最小形式: 三角形行でない先頭 4 行をこの順で読む (web/src/main.ts の headerLines と同じ判定)
     * - app 完全形式: `koujiname,<値>` のラベル付き行は 2 カラム目を値として読む
     */
    fun parseHeader(csv: String): CsvHeader {
        val values = mutableListOf<String>()
        for (line in csv.lineSequence()) {
            if (values.size >= 4) break
            if (line.isBlank()) continue
            val chunks = line.split(",").map { it.trim() }
            // 三角形行 (WebCsvReader.read と同じ判定: 4 カラム以上 + 先頭が非負整数) はヘッダではない
            val number = if (chunks.size >= 4) chunks[0].toIntOrNull() else null
            if (number != null && number >= 0) continue
            if (chunks[0] in META_LABELS) continue // ListAngle/TextSize/Deduction 行はヘッダではない
            values.add(if (chunks[0] in HEADER_LABELS) chunks.getOrNull(1) ?: "" else line.trim())
        }
        return CsvHeader(
            values.getOrElse(0) { "" },
            values.getOrElse(1) { "" },
            values.getOrElse(2) { "" },
            values.getOrElse(3) { "" },
        )
    }

    /**
     * 図枠の固定ラベル。app では string resource (rStr) 由来だが common に resource は無いので、
     * golden fixture を産んだ DxfDimensionLayoutGoldenTest と同一値を持つ。
     */
    fun defaultZumenInfo(): ZumenInfo = ZumenInfo(
        zumentitle = "面 積 展 開 図",
        rosenname = "路線1",
        koujiname = "工 事 名",
        tDtype_ = "図 面 名",
        tDname_ = "路 線 名",
        tScale_ = "縮    尺",
        tNum_ = "図面番号",
        tDateHeader_ = "作 成 日",
        tDate_ = "    年  月  日",
        tAname_ = "施 工 者",
        menseki_ = "面積",
        mTitle_ = "面 積 計 算 書",
        mCname_ = "工事名：",
        mSyoukei_ = " 小計",
        mGoukei_ = "合計",
        tCredit_ = "http://trianglelist.home.blog",
    )

    /**
     * CSV → 書き出し用 TriangleList。
     *
     * setChildsToAllParents が要る理由: 最小形式 CSV の読込 (WebCsvReader / app CsvLoader とも)
     * は connectionSide を add() の後に設定するため、add() 内の insertAndSlide → setChild →
     * setDimAlignByChild (= 子がぶら下がる辺の寸法を親の内側 INNER に置く) が発火しない。
     * 対話的に組んだリスト (golden fixture の経路) はコンストラクタが先に parent.nodeC を
     * 設定済みで alreadyHaveChild → setChild が走り INNER になる。この経路差を、既存 API
     * setChildsToAllParents (app テスト曰く「これやらないと認知されない」) で埋める。
     * 埋めないと親の共有辺の寸法値が子三角形側 (外) に出て golden と 1 行 (DXF code 73 /
     * SFC 垂直 align) 食い違う — WebDrawingExportGoldenTest が機械固定している。
     */
    private fun readForExport(csv: String) =
        WebCsvReader.read(csv).apply { setChildsToAllParents() }

    /**
     * CSV の Deduction 行 → 書き出し用 DeductionList (CsvCodec.buildDeductions、viewscale=1)。
     * アプリは saveDXF/saveSFC で myDeductionList.clone() を渡す (MainActivity.kt:2573/2588) —
     * web も同じ口に CSV 由来の dedlist を渡す。座標はビュー空間 (y 下向き) で、writer 側が
     * Y 反転する (DxfFileWriter.writeEntities:299 / SfcWriter:35)。viewscale=1 なので
     * DxfFileWriter の viewscale_ (既定 47.6 = アプリのビュー倍率) は 1f に明示する
     */
    private fun dedsForExport(csv: String) = CsvCodec.buildDeductions(CsvCodec.parse(csv))

    /** CSV → DXF 全文 (SJIS エンコード前の String)。初期化列は DxfDimensionLayoutGoldenTest.newWriter と同一 */
    fun buildDxfText(csv: String): String = buildDxfText(csv, "")

    /**
     * 段階2e (task #15): overrides 付き経路。readForExport 後に WebOverrides を適用して
     * から writer を組む — W/H フリップ・番号移動が DXF にも乗る (ADR 0003 Decision C
     * 「描画と書き出しは同じ式⊕override の結果を消費する」)。空 overrides なら従来と同一出力。
     */
    fun buildDxfText(csv: String, overridesJson: String): String =
        buildDxfText(csv, overridesJson, false)

    /**
     * 番号逆順付き経路 (アプリ保存ダイアログの NumReverse ボタン、MainActivity.kt:2293 →
     * writer.isReverse_ = isNumberReverse:2578)。効き方は DxfFileWriter.writeEntities:319-323
     * (番号の振り直し resetNumReverse + 控除リスト reverse) + :361-363 (面積計算書の行順) が正。
     * CSV 保存には影響しない (ファイル仕様不変)。numReverse=false なら従来と同一出力
     */
    fun buildDxfText(csv: String, overridesJson: String, numReverse: Boolean): String {
        val header = parseHeader(csv)
        val trilist = readForExport(csv)
        WebOverrides.applyJson(trilist, overridesJson)
        val writer = DxfFileWriter(trilist).apply {
            dedlist_ = dedsForExport(csv)
            viewscale_ = 1f // web の ded はビュー倍率 1 (アプリ既定 47.6 のままだと座標が 1/47.6 に縮む)
            startTriNumber_ = 1
            isReverse_ = numReverse
        }
        writer.titleTri_ = TitleParamStr()
        writer.titleDed_ = TitleParamStr()
        writer.zumeninfo = defaultZumenInfo()
        writer.setNames(header.koujiname, header.rosenname, header.gyousyaname, header.zumennum)
        // 台形 (混在リスト) を実寸 (scale=1) で組む。三角形 / 台形 / 台形子三角形を
        // buildMixed で 1 引数取得 (= CsvCodec の SoT)。trilist は overrides 適用済を明示渡し、
        // 台形の親解決にユーザー手動配置を反映させる。三角形のみ CSV なら traps/trapTris が空。
        val mixed = CsvCodec.buildMixed(CsvCodec.parse(csv), 1f, trilist)
        writer.traps_ = mixed.traps
        writer.trapTris_ = numberTrapTris(mixed.trapTris, trilist.size(), mixed.traps.size)
        val sb = StringBuilder()
        writer.writer = sb
        writer.save()
        return sb.toString()
    }

    /** 台形を親に持つ三角形に mynumber / pointnumber を WebPrimitiveRenderer と同じ仕様で振る。
     *  mynumber = 三角形数 + 台形数 + (1-based 連番)、pointnumber は重心 (= pointcenter)。
     *  writer は writeTriangle 経由で mynumber を採番済み前提で扱う (TriangleList の中ではないため
     *  arrangePointNumbers が回らない、ここで明示的にセット)。 */
    private fun numberTrapTris(tris: List<Triangle>, triCount: Int, trapCount: Int): List<Triangle> {
        val base = triCount + trapCount
        for ((j, t) in tris.withIndex()) {
            t.mynumber = base + j + 1
            t.pointnumber = t.pointcenter
        }
        return tris
    }

    /**
     * CSV → 完全形式 28 列 CSV (ADR 0008 bake)。overrides (W/H フリップ・番号移動) を model に
     * 適用してから書くので、web での手動配置が保存 CSV に乗る — これまで overrides は
     * localStorage 止まりで、書き出した CSV をアプリで開くと手動配置が消えていた (ユーザー損失)。
     * 列順・値はアプリの保存 (MainActivity.writeCSV) と同一
     */
    fun buildCsvText(csv: String, overridesJson: String): String {
        val doc = CsvCodec.parse(csv)
        val trilist = readForExport(csv)
        WebOverrides.applyJson(trilist, overridesJson)
        return CsvCodec.serialize(CsvCodec.bake(trilist, doc))
    }

    /** CSV → SFC 全文 (SJIS エンコード前の String)。初期化列は SfcDimensionLayoutGoldenTest.writeSfcString と同一 */
    fun buildSfcText(csv: String, filename: String): String = buildSfcText(csv, filename, "")

    /** 段階2e (task #15): overrides 付き経路 (buildDxfText の overrides 版と同じ理屈) */
    fun buildSfcText(csv: String, filename: String, overridesJson: String): String =
        buildSfcText(csv, filename, overridesJson, false)

    /** 番号逆順付き経路 (SfcWriter.kt:49-53 = DXF と同じ resetNumReverse + 控除 reverse) */
    fun buildSfcText(csv: String, filename: String, overridesJson: String, numReverse: Boolean): String {
        val header = parseHeader(csv)
        val trilist = readForExport(csv)
        WebOverrides.applyJson(trilist, overridesJson)
        val writer = SfcWriter(trilist, dedsForExport(csv), filename, 1, 1f)
        writer.titleTri_ = TitleParamStr()
        writer.titleDed_ = TitleParamStr()
        writer.zumeninfo = defaultZumenInfo()
        // textscale は内部値 (getPrintTextScale = DXF と同一)。単位モデル統一 (段4) 後は
        // 500 直値上書きは primitive の ×unitscale と二重になるため外す。
        writer.setNames(header.koujiname, header.rosenname, header.gyousyaname, header.zumennum)
        writer.setStartNumber(1)
        writer.isReverse_ = numReverse
        // 台形 (混在リスト) を実寸 (scale=1) で組む。DXF 側と同形で buildMixed 経由で 1 引数取得。
        // trilist は overrides 適用済を明示渡し。三角形のみ CSV なら traps/trapTris が空 = SFC golden 不変。
        val mixed = CsvCodec.buildMixed(CsvCodec.parse(csv), 1f, trilist)
        writer.traps_ = mixed.traps
        writer.trapTris_ = numberTrapTris(mixed.trapTris, trilist.size(), mixed.traps.size)
        return writer.buildSfcString()
    }
}
