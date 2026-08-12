package com.jpaver.trianglelist.datamanager

/**
 * 図面枠 (paper 固定位置) に付随するテキストの役割。TextSizePolicy.resolve の key。
 *
 * 図形本体の寸法値・測点名・番号等 (drawingScale に依存し、密集メッシュでの接触回避が本質制約、
 * ADR 0001 で 2 path 構造として明文化済み) はこの enum の対象外 ── TriangleList.getPrintTextScale
 * / TextScaleCalculator が既に単一の入口として機能しており、ここに合流させる理由がない。
 *
 * このロールは「用紙の固定位置に置く枠側テキスト」のみを指す。DrawingFileWriter.writeTopTitle /
 * writeDrawingFrame 以外の場所からサイズを取得する経路を作らないことが、このファイルを分離した目的。
 */
enum class TextRole {
    /** 用紙上中央「面積展開図」+ 路線名 + 面積合計。 */
    TopTitle,

    /** 用紙右下の表題欄 cell (工事名/図面名/路線名/作成日/縮尺/図面番号/施工者)。 */
    BottomTitleFrame,

    /** 用紙左下の url 表記。 */
    BottomCredit,
}
