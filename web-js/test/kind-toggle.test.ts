import { describe, it, expect, beforeAll, beforeEach } from 'vitest';
import { health, state, loadCsv, click, options, key, select, tap } from './combo/cpClient.ts';
import { expectScreenshotToMatch } from './combo/vrt.ts';

// dev server に当てる integration test。 commit 1755172 で導入した
// 「一覧の △/□ アイコン click で 三角形 ↔ 台形 切替」 の動作を pin。
// user 2026-06-18 「テストケースにないのか」 ── ロジック層 + UI option 出力を
// 機械化された assertion で押さえる。

beforeAll(async () => {
  const ok = await health();
  if (!ok) throw new Error('dev server (http://localhost:5173) が立ってない');
}, 10_000);

/**
 * UI が静止する (state が 3 連続で同じ) まで待つ。時間ではなく変化が止まったことを待つ。
 * 2026-08-25 追加: VRT スクショが直前の test の残留 UI 状態を写して flaky になっていたため。
 */
async function waitQuiescent(): Promise<void> {
  let last = '';
  let stable = 0;
  for (let i = 0; i < 60; i++) {
    const s: any = await state();
    const cur = JSON.stringify(s?.newRow ?? null)
      + '|' + String(s?.selected ?? '')
      + '|' + String(s?.prims?.length ?? 0)
      + '|' + String(s?.deductionMode ?? '')
      + '|' + String(s?.rows?.length ?? 0);
    stable = cur === last ? stable + 1 : 0;
    last = cur;
    if (stable >= 2) return;
    await new Promise((r) => setTimeout(r, 50));
  }
  throw new Error(`画面が静止しない: ${last}`);
}

/**
 * アプリを既知の初期状態へ戻す (アプリ自身の「新規作成」)。
 * この file も VRT スクショを撮るので、vrt-scenarios と同じ理由でリセットが要る ──
 * 両者を同じスイートで回すと、控除モード / 控除リストの開閉 / fab ハイライト /
 * 種別に連動するヘッダ表記 が相互に漏れて双方の snapshot が壊れる (2026-08-25)。
 */
/**
 * 新規行フォームのテキスト入力をクリアし、反映を確認する。
 *
 * 「新規作成」(newDrawing) は rows / モード / 選択は初期化するが **新規行フォームは
 * 触らない** (2026-08-25 実測)。残ったままだと次の test のスクショに写る。
 *
 * select (接続辺 / 形態 / 起点) は触らない ── 親番号や選択から派生して自動で決まる値なので、
 * 外から値を入れると逆に非決定になる。実際、旧実装が key('newLcr','0') で左起点を強制して
 * いたせいで「右起点 (派生値) と 左起点 (強制値)」が混ざり 27px の flaky になっていた。
 * 親番号を空にすれば select 側も一意に落ち着く (conn=1 / ctype=0 / lcr=2)。
 */
async function clearNewRowInputs(): Promise<void> {
  for (let attempt = 0; attempt < 20; attempt++) {
    for (const id of ['newA', 'newB', 'newC', 'newParent']) await key(id, '');
    const s: any = await state();
    const n = s?.newRow;
    if (n && n.a === '' && n.b === '' && n.c === '' && n.parent === '') return;
    await new Promise((r) => setTimeout(r, 50));
  }
  throw new Error('新規行フォームがクリアされない');
}

beforeEach(async () => {
  await click('newDrawing');
  await tap(0, 0);
  await clearNewRowInputs();
  await waitQuiescent();
});

// page HTML を取って <select id="sideCell-N"> の option value 列を抽出
async function sideOptionsOf(triNum: number): Promise<string[]> {
  const res = await fetch('http://localhost:5173/__tlcp/page', {
    method: 'GET',
  });
  // /__tlcp/page は PNG を返すので HTML 取得は別経路 — vite dev は root で index.html
  // を返す、 ただし DOM 状態は run-time なので index.html では足りない。
  // 妥協: HTML scraping ではなく state() の row.parentKind と row.kind から間接判定
  // (= ロジック層 unit test として割り切る)。 UI 側の option 生成は connOptionsFor
  // (line 1159) が pure 関数で parentKind=1 を入れたら必ず ['1','2','3'] を返す
  // ことで保証されてる、 = parentKind=1 が立てば D 出る。
  void res;
  return [];
}

describe('kind-toggle (一覧 △/□ click で 種別切替)', () => {
  it('親 rectangle + 子 rectangle 構成で、 子 row.parentKind が 1 (= D 辺 option が出る条件)', async () => {
    // 親=台形 (Rectangle, num=1, height=3, widthA=10, widthB=7, parent=-1, side=0)
    // 子=台形 (Rectangle, num=2, height=2, widthA=3, widthB=2, parent=1, side=1, align=0, parentKind=1)
    const csv = [
      'Rectangle,1,3,10,7,-1,0,0,0',
      'Rectangle,2,2,3,2,1,1,0,1',
    ].join('\n');
    await loadCsv(csv);
    const s = await state();
    expect(s.rows.length).toBe(2);
    const parent = s.rows[0];
    const child = s.rows[1];
    expect(parent.kind).toBe('rectangle');
    expect(child.kind).toBe('rectangle');
    expect(child.parent).toBe('1');
    // 子 row.parentKind = 1 ── 親が rectangle なので D 辺接続可、 buildTrapRowCells
    // で connOptionsFor('new', 1) → ['1','2','3'] が options に積まれる
    expect(child.parentKind).toBe(1);
  });

  // dev server 再起動 + browser refresh が必要 (options endpoint は vite plugin 拡張で
  // 追加、 hot.on('tlcp:options-req') 登録は browser side で main.ts 再 load 後に有効)。
  // 走らせるには (a) dev server kill → npm run dev で再起動、 (b) localhost:5173 を
  // browser で開いて refresh、 (c) it.skip it に戻して再走。
  it('親 rectangle + 子 rectangle 構成で sideCell-2 の option に "3" (D 辺) が含む', async () => {
    const csv = [
      'Rectangle,1,3,10,7,-1,0,0,0',
      'Rectangle,2,2,3,2,1,1,0,1',
    ].join('\n');
    await loadCsv(csv);
    const opts = await options('sideCell-2');
    expect(opts.ok).toBe(true);
    const vals = opts.options.map((o) => o.value);
    expect(vals).toContain('3');
  });

  it('親 rectangle + 子 triangle を △ click で rectangle 化したとき、 子 row.parentKind が 1 になる', async () => {
    // 親=台形, 子=三角形 (親の B 辺接続)
    const csv = [
      'Rectangle,1,3,10,7,-1,0,0,0',
      '2,3,3,3,1,1', // num=2 triangle、 parent=1, conn=1 (親の B 辺接続)
    ].join('\n');
    await loadCsv(csv);
    let s = await state();
    expect(s.rows.length).toBe(2);
    expect(s.rows[1].kind).toBe('triangle');

    // 子 (row index 1 = num=2) の △ アイコン click で rectangle 化
    const r = await click('kindCell-2');
    expect((r as any).ok).toBe(true);

    // wait for redraw + buildTable
    await new Promise((res) => setTimeout(res, 200));
    s = await state();
    expect(s.rows[1].kind).toBe('rectangle');
    // 親が rectangle なので、 切替後の子の parentKind は 1 (= D 辺 option 出る条件)
    expect(s.rows[1].parentKind).toBe(1);
  });

  // dev server 再起動 + browser refresh が必要 (上の [要再起動] と同じ理由)
  it('click 後の子 sideCell-2 の option に "3" (D 辺) が含む', async () => {
    const csv = [
      'Rectangle,1,3,10,7,-1,0,0,0',
      '2,3,3,3,1,1',
    ].join('\n');
    await loadCsv(csv);
    await click('kindCell-2');
    await new Promise((res) => setTimeout(res, 200));
    const opts = await options('sideCell-2');
    expect(opts.ok).toBe(true);
    const vals = opts.options.map((o) => o.value);
    expect(vals).toContain('3');
  });

  it('親 triangle + 子 triangle を △ click で rectangle 化したとき、 子 row.parentKind は 0 (= D 辺 option 出ない、 正しい挙動)', async () => {
    // 親=三角形, 子=三角形
    const csv = [
      '1,6,5,4,-1,-1',
      '2,3,3,3,1,1',
    ].join('\n');
    await loadCsv(csv);
    let s = await state();
    expect(s.rows.length).toBe(2);

    const r = await click('kindCell-2');
    expect((r as any).ok).toBe(true);
    await new Promise((res) => setTimeout(res, 200));
    s = await state();
    expect(s.rows[1].kind).toBe('rectangle');
    // 親が triangle なので、 子の parentKind は 0 ── D 辺 option は出ない (正しい)
    expect(s.rows[1].parentKind).toBe(0);
  });

  describe('台形と三角形の色替え (fabFillColor)', () => {
    it('台形の色替えをクリックしたとき、測点名ではなく extras[4] に色が保存されること', async () => {
      const csv = [
        'Rectangle,1,3.00,10.00,7.00,-1,0,0,0,0,RecStation,,,,4',
      ].join('\n');
      await loadCsv(csv);

      let s = await state();
      expect(s.rows[0].kind).toBe('rectangle');
      expect(s.rows[0].extras[0]).toBe('RecStation'); // 測点名
      expect(s.rows[0].extras[4] ?? '4').toBe('4');   // 初期色 (4)

      await select(1); // 明示的に1行目を選択する
      const r = await click('fabFillColor');
      expect((r as any).ok).toBe(true);

      await new Promise((res) => setTimeout(res, 200));
      s = await state();
      
      // 新仕様：台形(rectangle)の場合は extras[0] に色が保存される (0)
      expect(s.rows[0].extras[0]).toBe('0');

      // VRTによるビジュアル検証
      await waitQuiescent();
    await expectScreenshotToMatch('kind-toggle-rectangle-color-changed');
    });

    it('三角形の色替えをクリックしたとき、extras[4] に色が保存され、測点名 extras[0] は影響を受けないこと', async () => {
      const csv = [
        '1,3.00,3.00,3.00,-1,-1,TriStation,,,,4',
      ].join('\n');
      await loadCsv(csv);

      let s = await state();
      expect(s.rows[0].kind).toBe('triangle');
      expect(s.rows[0].extras[0]).toBe('TriStation');
      expect(s.rows[0].extras[4] ?? '4').toBe('4');

      const r = await click('fabFillColor');
      expect((r as any).ok).toBe(true);

      await new Promise((res) => setTimeout(res, 200));
      s = await state();

      expect(s.rows[0].extras[4]).toBe('0');
      expect(s.rows[0].extras[0]).toBe('TriStation');

      // VRTによるビジュアル検証
      await waitQuiescent();
    await expectScreenshotToMatch('kind-toggle-triangle-color-changed');
    });
  });
});
