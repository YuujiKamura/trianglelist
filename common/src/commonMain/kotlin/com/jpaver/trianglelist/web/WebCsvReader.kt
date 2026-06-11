package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.TriangleList

/**
 * Web 用 CSV リーダー — 実体は CsvCodec (ADR 0008) への委譲。
 *
 * 段階1 ではここに parse+構築が直書きされていたが、文書モデル化 (CsvCodec.parse → CsvDoc →
 * CsvCodec.build) に置き換えた。手動配置の復元・ListAngle・named phases の説明は
 * CsvCodec.kt 側に集約。この object は既存の呼び出し面 (WebPrimitiveRenderer /
 * WebHitTest / WebDrawingExport / テスト群) の互換のために残す。
 */
object WebCsvReader {

    fun read(csv: String): TriangleList = CsvCodec.build(CsvCodec.parse(csv))
}
