// XSS 防御: window.open に渡す前に url の scheme を http/https に限定する。
// 2026-06-18 security review (automated) 指摘 ── tCredit (= 図面枠の url 表示) を canvas click で
// 別タブ open する経路で、 url が javascript:/data:/vbscript:/file: の時 XSS 可能。 現状 url 値は
// wasm 内 zumeninfo.tCredit_ 固定で user 入力経路はないが、 将来 user 編集可能になった時の
// exploit を防ぐため scheme を限定する多層防御。
//
// 戻り値: 開いてよい url 文字列 (= sanitized) or null (= 開かない)。 null 時の理由は呼出側で
// 区別しない (silent ignore、 user 側に harm はなく log も不要)。

export function safeOpenUrl(raw: string | null | undefined): string | null {
  if (!raw) return null;
  try {
    const u = new URL(raw);
    if (u.protocol === 'http:' || u.protocol === 'https:') {
      return u.toString();
    }
    return null;
  } catch {
    return null;
  }
}
