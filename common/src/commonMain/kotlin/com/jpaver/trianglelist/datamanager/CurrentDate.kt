package com.jpaver.trianglelist.datamanager

/**
 * 図枠の作成日 "yyyy 年 M 月 d 日"。スペース位置まで全 platform 同一フォーマットで組むこと
 * (golden テストは日付行を #DATE# に正規化するが、フォーマットが崩れると正規化が効かず fail する)。
 */
expect fun currentDateStringJp(): String
