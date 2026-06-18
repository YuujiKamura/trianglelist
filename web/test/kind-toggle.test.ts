import { describe, it, expect, beforeAll } from 'vitest';
import { health, state, loadCsv, click, options, key } from './combo/cpClient.ts';

// dev server に当てる integration test。 commit 1755172 で導入した
// 「一覧の △/□ アイコン click で 三角形 ↔ 台形 切替」 の動作を pin。
// user 2026-06-18 「テストケースにないのか」 ── ロジック層 + UI option 出力を
// 機械化された assertion で押さえる。

beforeAll(async () => {
  const ok = await health();
  if (!ok) throw new Error('dev server (http://localhost:5173) が立ってない');
}, 10_000);

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
  // browser で開いて refresh、 (c) it.skip を it に戻して再走。
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
});
