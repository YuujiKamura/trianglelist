package com.jpaver.trianglelist.viewmodel

import androidx.lifecycle.ViewModel
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.TriangleList

/**
 * 編集中の図面の所有者。
 *
 * これまでモデルは MainActivity の lateinit フィールドにあり、CSV からの復元は
 * onAttachedToWindow / onResume という **View の都合のコールバック**に紐付いていた。
 * View が付いた・画面が前に出た、はどちらも「データを読み直すべき瞬間」ではなく、
 * 何回でも起きる。その結果 resume のたびに生きたモデルがディスクの内容で上書きされ、
 * 未保存の操作 (ピンチ回転など) が巻き戻っていた (2026-08-27 実機報告)。
 *
 * ViewModel は「構成変更や resume では死なず、プロセスと一緒に死ぬ」寿命を持つ。
 * これは「CSV から復元すべき唯一の瞬間 = メモリ上にモデルが無いとき」と正確に一致する。
 * したがってモデルの所有をここに移し、復元は isRestored で 1 プロセス 1 回に限定する。
 * Activity / View 側は保持しているものを描画するだけになり、コールバックが何回走っても壊れない。
 */
class DrawingSessionViewModel : ViewModel() {

    var trilist: TriangleList = TriangleList()
    var dedlist: DeductionList = DeductionList()

    /**
     * このプロセスで CSV からの復元を済ませたか。
     * 端末回転などで Activity が作り直されても ViewModel は生き残るので false に戻らず、
     * 復元は走らない (= メモリ上のモデルがそのまま使われる)。
     */
    var isRestored: Boolean = false

    /**
     * private CSV へ最後に書いた本文。自動保存の「内容が変わったか」判定の基準。
     *
     * 「どの操作が保存を要するか」を操作ごとに配線する方式 (setCommonFabListener の
     * isSaveCSV 引数) は、人間が全ての変更経路を正しく判断し続ける前提で、実際
     * ピンチ回転が漏れていた。焼いた本文をここと比べれば、変更経路を知らなくても
     * 「変わっていれば書く / 同じなら書かない」が成立する。
     * Activity より寿命が長いので端末回転をまたいでも基準が失われない。
     */
    var lastSavedCsv: String? = null
}
