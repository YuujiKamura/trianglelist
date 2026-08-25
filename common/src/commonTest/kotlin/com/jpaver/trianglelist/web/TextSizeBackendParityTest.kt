package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.TextRole
import com.jpaver.trianglelist.datamanager.TextSizePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 「同じ CSV を食わせたら、どの backend でも文字は同じ実寸で出る」を pin する gate。
 *
 * 2026-08-25 user 指摘「補正とか掛けてる時点で抽象化が足りてない」を物理層に降ろしたもの。
 *
 * 経緯: 文字サイズは policy (paper mm) から 4 つの境界を渡って各 backend に届くが、
 * 渡っているのは `size: Float` という名前だけの裸の数値で、単位も「その数が何の高さか」も
 * 型に乗っていない。結果、境界ごとに受け手が意味を思い出して手で変換する構造になり、
 * 実際に 2 種類の事故が出た:
 *
 *   (1) 単位を思い出し損ねた   … WebFrame が mm→cm の ÷10 を二重に掛け、web の枠テキスト
 *                                だけ 1/10 になった (2026-08-25 修正、cc21adfb)
 *   (2) 意味を手で補正している … DXF height はキャップハイト、TTF の fontSize は em なので
 *                                renderer 側が ÷capHeightRatio する。これ自体は必要な変換
 *                                だが、leaf に散っていて backend ごとに有無が違う
 *
 * (1) が 2 週間気付かれなかったのは「backend 間の実寸を突き合わせるテストが無い」から。
 * 型を入れても、この gate が無ければ次の形でまた漏れる ── なので先にここを置く。
 *
 * 本 test が見るのは emit 層 (model → DXF / web prim) の一致。renderer 層 (prim → 画面 px、
 * = キャップハイト → em の変換) は各プラットフォームの font 実測が要るので、desktop / web
 * それぞれの test に分けて置く (TextRenderer / web-js の cap-height test)。
 *
 * 単位の対応:
 *   DXF (DxfFileWriter)  … model mm      例: paper 3.5mm × 1/50 → 175.0
 *   web (WebFrame)       … model m       例: 同上 → 0.175
 * なので比は常に 1000。ここを直接 pin すると「片方だけ単位変換が増減した」瞬間に落ちる。
 */
class TextSizeBackendParityTest {

    /** DXF の 1 単位 = 1 model mm、web prim の 1 単位 = 1 model m。 */
    private val dxfPerWeb = 1000.0
    private val tol = 1e-3

    private val csv = """
        市道○○号線 舗装打換工事
        市道○○号線
        ○○建設株式会社
        1/1
        1,10,2,10,-1,-1
        2,10,1,10,1,2
        ListAngle, 180
    """.trimIndent() + "\n"

    /** DXF TEXT の (文字列, group code 40 = 文字高さ) を layer 指定で全部拾う。
     *  group code 40 は仕様上キャップハイト (ezdxf doc / AutoCAD DXF reference)。 */
    private fun dxfTexts(dxf: String, layer: String): List<Pair<String, Double>> {
        val lines = dxf.split("\n").map { it.trim() }
        val out = mutableListOf<Pair<String, Double>>()
        var i = 0
        while (i < lines.size - 1) {
            if (lines[i] == "0" && lines[i + 1] == "TEXT") {
                var h: Double? = null
                var t: String? = null
                var lay: String? = null
                var j = i + 2
                while (j < lines.size - 1 && lines[j] != "0") {
                    when (lines[j]) {
                        "40" -> h = lines[j + 1].toDoubleOrNull()
                        "1" -> t = lines[j + 1]
                        "8" -> lay = lines[j + 1]
                    }
                    j += 2
                }
                if (h != null && t != null && lay == layer) out.add(t to h)
                i = j
            } else i++
        }
        return out
    }

    /** web prim JSON から layer="frame" の (文字列, size) を全部拾う。 */
    private fun webFrameTexts(json: String): List<Pair<String, Double>> =
        Regex(""""type":"text","layer":"frame","text":"((?:[^"\\]|\\.)*)".*?"size":([-0-9.E]+)""")
            .findAll(json)
            .map { it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\") to it.groupValues[2].toDouble() }
            .toList()

    private fun webFrameTextSizes(json: String): List<Double> = webFrameTexts(json).map { it.second }

    @Test
    fun `同じ文字は DXF と web で同じ実寸になる`() {
        // これが今回の ÷10 二重掛けを落とす gate。片方の経路にだけ単位変換が増えた瞬間に red。
        //
        // 突き合わせは「文字列が同じもの同士」で行う。DXF の C-TTL-FRAM には web が出さない
        // 寸法表 (writeTitleTri、web は HTML 表で代替) も乗るので、集合として比べると
        // その scope 差で落ちてしまう ── 見たいのは「同じものが同じ大きさか」だけ。
        val dxf = WebDrawingExport.buildDxfText(csv)
        val web = WebFrame.renderFrame(csv)

        val dxfByText = dxfTexts(dxf, "C-TTL-FRAM")
            .filter { it.first.isNotBlank() }
            .groupBy({ it.first }, { it.second })
        val webByText = webFrameTexts(web)
            .filter { it.first.isNotBlank() }
            .groupBy({ it.first }, { it.second })

        val shared = dxfByText.keys.intersect(webByText.keys)
        // 空集合で vacuous pass しないよう、突き合わせ対象の本数自体も pin する
        // (表題欄のラベル 6 + 内容 + タイトル + url で 10 は下らない)
        assertTrue(
            shared.size >= 10,
            "DXF と web で共通の枠テキストが $shared 件しかない ── 比較が成立していない。" +
                "dxf=${dxfByText.keys} web=${webByText.keys}"
        )

        // 比較は DXF 側の単位 (model mm) で行い、許容はその量子化幅そのものにする ──
        // DxfEntity は group code 40 を formattedString(0) = 整数 model mm で書くので、
        // web の連続値との差は最大 0.5 出る。それ以上ズレたら単位変換の増減であって丸めではない
        // (今回の ÷10 なら 10 倍 = 数百 mm のズレになるので余裕で捕まる)。
        val quantum = 0.5 + 1e-6
        val mismatches = shared.mapNotNull { t ->
            val d = dxfByText.getValue(t).sorted()
            val w = webByText.getValue(t).map { it * dxfPerWeb }.sorted()
            val same = d.size == w.size && d.indices.all { i -> kotlin.math.abs(d[i] - w[i]) <= quantum }
            if (same) null else "  \"$t\": DXF(model mm)=$d web(×1000)=$w"
        }
        assertTrue(
            mismatches.isEmpty(),
            "同じ文字の実寸が backend 間でズレている (どちらかの経路に単位変換が増減している):\n" +
                mismatches.joinToString("\n")
        )
    }

    @Test
    fun `枠テキストの実寸が policy の paper mm と一致する`() {
        // backend 同士が一致していても、両方まとめて policy からズレていたら意味が無い。
        // paper mm → model への換算は printscale (ps) 1 本だけであるべき、を pin する。
        val web = WebFrame.renderFrame(csv)
        val ps = WebCsvReader.read(csv).getPrintScale(1f).toDouble()

        // paper 外周 (A3 = 42cm 幅) の実寸から ps を逆算し、引数の ps と一致することを確認
        // (= 「座標に効いている ps」と「サイズに効いている ps」が同じ 1 本であることの確認)
        val paperXs = Regex(""""type":"line","layer":"paper","x1":([-0-9.E]+),"y1":[-0-9.E]+,"x2":([-0-9.E]+)""")
            .findAll(web).flatMap { sequenceOf(it.groupValues[1].toDouble(), it.groupValues[2].toDouble()) }
            .toList()
        assertTrue(paperXs.isNotEmpty(), "paper 外周 line が無い")
        val psFromGeometry = (paperXs.max() - paperXs.min()) / 42.0
        assertEquals(ps, psFromGeometry, tol, "座標に効いている ps と getPrintScale が不一致")

        val sizes = webFrameTextSizes(web)
        val titlePaperMm = TextSizePolicy.resolve(TextRole.TopTitle).toDouble() * 10.0
        val creditPaperMm = TextSizePolicy.resolve(TextRole.BottomCredit).toDouble() * 10.0
        val framePaperMm = TextSizePolicy.resolve(TextRole.BottomTitleFrame).toDouble() * 10.0

        // 錨には縮小のかからないものを選ぶ。表題欄のセル内テキストは TextFit.fitSize が
        // 箱に合わせて縮めるので「最小 = policy の base」は成り立たない (縮小は仕様)。
        // url (BottomCredit) だけは fitted() を通らず base サイズのまま出るので、
        // 「policy → prim の間に ps 以外の係数が挟まっていない」の検査はこれで行う。
        val urlSize = Regex(""""type":"text","layer":"frame","text":"http[^"]*".*?"size":([-0-9.E]+)""")
            .find(web)?.groupValues?.get(1)?.toDouble()
        assertTrue(urlSize != null, "url (BottomCredit) の prim が見つからない")
        assertEquals(creditPaperMm, (urlSize!! / ps) * 10.0, tol, "url (BottomCredit) が policy の paper mm と不一致")

        // model → paper cm は ÷ps、cm → mm は ×10。この 2 段以外の係数が挟まっていないこと
        val maxPaperMm = (sizes.max() / ps) * 10.0
        assertEquals(titlePaperMm, maxPaperMm, tol, "最大の枠テキスト (TopTitle) が policy の paper mm と不一致")
        // 縮小は base 以下にしかならない (拡大方向に動いたら単位が壊れている)
        val overBase = sizes.filter { (it / ps) * 10.0 > titlePaperMm + tol }
        assertTrue(overBase.isEmpty(), "TopTitle の base を超える枠テキストがある: $overBase")
        assertTrue(
            (sizes.min() / ps) * 10.0 <= framePaperMm + tol,
            "最小の枠テキストが表題欄 base を超えている ── 縮小方向のはずが拡大している"
        )
    }

    @Test
    fun `寸法値より表題欄が小さく タイトルが大きい強弱が保たれる`() {
        // 2026-08-12 に表題欄 (2.5mm) が寸法値 (3.5mm) を下回る逆転が入り、web で判読不能に
        // なった (2026-08-25 user 指摘)。数値そのものは policy 側の test が pin するので、
        // ここは「実際に emit された prim の大小関係」として押さえる (= 描画が真実)。
        val web = WebFrame.renderFrame(csv)
        val figures = WebPrimitiveRenderer.renderCsv(csv, 1f)
        val dimSizes = Regex(""""type":"text","layer":"dim".*?"size":([-0-9.E]+)""")
            .findAll(figures).map { it.groupValues[1].toDouble() }.toList()
        assertTrue(dimSizes.isNotEmpty(), "dim text が無い")

        val frameSizes = webFrameTextSizes(web)
        assertTrue(
            frameSizes.max() > dimSizes.max(),
            "図面タイトルは寸法値より大きいべき (title=${frameSizes.max()} dim=${dimSizes.max()})"
        )
    }

    private fun List<Double>.distinctRounded(): List<Double> =
        map { kotlin.math.round(it * 1e4) / 1e4 }.distinct().sorted()
}
