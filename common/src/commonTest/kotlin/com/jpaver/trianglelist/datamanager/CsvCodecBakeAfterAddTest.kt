package com.jpaver.trianglelist.datamanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 2026-08-13 発見の再現テスト: 三角形を追加した直後の autosave が、その追加分の行を
 * CSV から欠落させるバグ。MainActivity の実運用シーケンスをそのまま模す:
 *
 *   resumeCSV (起動/復帰時ロード) → FABリプレースで三角形追加 (trilist だけ更新) →
 *   autosave (writeCSV:2759-2769 = CsvCodec.bake(trilist, original=currentDoc)) →
 *   share等でバックグラウンド遷移→復帰 → onResume→resumeCSV (直前に書いたCSVを読み直す)
 *
 * CsvCodec.bake(trilist, original) (CsvCodec.kt:581-601) は
 * `original.figureRows.map { ... }` で original 側の行数分しか出力しないため、
 * trilist が original より行数が多い (= 追加された) 場合、追加分は例外もトーストも無く
 * 静かに出力から欠落する。isSaved は true を返すため、autosave 呼び出し側からは
 * 失敗が一切見えない。
 */
class CsvCodecBakeAfterAddTest {

    @Test
    fun bake_after_add_drops_the_newly_added_triangle_row() {
        // 直前の autosave で保存済みの状態 (三角形1個) = resumeCSV 直後の currentDoc
        val savedDoc = CsvCodec.parse("1,6.0,5.0,4.0,-1,-1\n")
        val trilist = CsvCodec.build(savedDoc)
        assertEquals(1, trilist.size())

        // ユーザー操作: 三角形1のB辺(side=1)をタップしてシャドー表示→数字入力→FABリプレースで追加
        val added = trilist.add(1, 1, 3.0f, 3.0f)
        assertTrue(added, "trilist.add がバリデーションで弾かれていないことの前提確認")
        assertEquals(2, trilist.size(), "画面(メモリ上のtrilist)には追加が正しく反映されている")

        // MainActivity.writeCSV と同じ呼び出し: original には「追加前」の savedDoc を渡す
        val baked = CsvCodec.bake(trilist, savedDoc)

        // 期待: bake後のfigureRowsはtrilistと同じ2件になるべき
        // 実際: savedDoc.figureRows (1件) をmapしているだけなので1件のまま → 追加分が消える
        assertEquals(
            trilist.size(),
            baked.figureRows.size,
            "追加した三角形の行が bake() の出力から欠落している" +
                " (original.figureRows を map しているだけで新規行を append していない)",
        )
    }

    @Test
    fun autosave_then_resume_cycle_reverts_screen_to_pre_add_state() {
        // resumeCSV 直後: currentDoc と trilist が一致した状態からスタート
        var currentDoc = CsvCodec.parse("1,6.0,5.0,4.0,-1,-1\n")
        val trilist = CsvCodec.build(currentDoc)
        assertEquals(1, trilist.size())

        // ユーザー操作: 辺タップ→シャドー→数字入力→FABリプレースで三角形を1つ追加
        trilist.add(1, 1, 3.0f, 3.0f)
        assertEquals(2, trilist.size(), "追加直後、画面上は2個の三角形が見えている")

        // autosave: setCommonFabListener → autosave() → saveCSVtoPrivate() → writeCSV()
        val bakedDoc = CsvCodec.bake(trilist, currentDoc)
        currentDoc = bakedDoc // writeCSV: this.currentDoc = updatedDoc
        val savedCsvText = CsvCodec.serialize(bakedDoc) // writer.write(...) で privateTrilist.csv に書かれる内容

        // シェアFAB操作でバックグラウンド遷移→復帰: onResume() → resumeCSV() → parseCSV() が
        // 直前に書いたCSVを読み直して画面(trianglelist)を丸ごと差し替える
        val reloadedTrilist = CsvCodec.build(CsvCodec.parse(savedCsvText))

        // バグ再現: 追加したはずの三角形が画面から消えている (1個に戻ってしまう)
        assertEquals(
            2,
            reloadedTrilist.size(),
            "share操作からの復帰後、直前にFABリプレースで追加した三角形が画面から消えてしまう" +
                " (autosave が bake() の行欠落バグにより追加分を書き損じているため)",
        )
    }
}
