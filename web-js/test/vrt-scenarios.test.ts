import { describe, it, expect, beforeAll, beforeEach } from 'vitest';
import { health, state, loadCsv, click, tap, key } from './combo/cpClient.ts';
import { expectScreenshotToMatch } from './combo/vrt.ts';

beforeAll(async () => {
  const ok = await health();
  if (!ok) throw new Error('dev server (http://localhost:5173) が立ってない');
}, 10_000);

/**
 * アプリを既知の初期状態へ戻す。
 *
 * 2026-08-25 修正。VRT のスクショは「その瞬間の画面全部」を写すので、**前の test が
 * 残した UI 状態がそのまま差分になる**。実際に踏んだもの:
 *  - 新規行フォームの残留 (親番号 / 起点) … loadCsv はフォームを触らないので次の
 *    scenario まで生き残る → 27px 差で単独実行は通りスイート実行で落ちる flaky
 *  - 控除リスト <details> の開閉、控除モード、fab のハイライト、種別 (台形/三角形) に
 *    連動するヘッダ表記 … kind-toggle と VRT を同じスイートで回すと双方が壊れる
 *
 * 個々のフィールドを狙って消す方式は、値の到達可能性に依存して脆い (例: newConn の
 * option は 1/2 しか無く "0" は存在しないので、旧実装の key('newConn','0') は無言で
 * 無視されていた)。アプリ自身の「新規作成」を叩いて丸ごと初期化する方が確実。
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

async function resetApp(): Promise<void> {
  await click('newDrawing'); // rows / 控除モード / 選択 を初期化
  await tap(0, 0);
  await clearNewRowInputs();  // 新規作成はフォームを触らないので明示クリア
  await waitQuiescent();
}

/** UI が静止する (state が 3 連続で同じ) まで待つ。時間ではなく変化が止まったことを待つ。 */
async function waitQuiescent(): Promise<string> {
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
    if (stable >= 2) return cur;
    await new Promise((r) => setTimeout(r, 50));
  }
  throw new Error(`画面が静止しない: ${last}`);
}

/**
 * 画面が静止する (= 新規行フォームが 3 連続で同じ値を返す) まで待ってからスクショを撮る。
 *
 * 2026-08-25 修正: loadCsv の後、アプリは新規行フォームを**非同期で再設定**する
 * (末尾の図形に接続する前提で 親番号=1 / 起点=右起点 を自動で入れる)。旧実装は固定
 * 200ms 待ちだったので、この再設定に勝ったり負けたりして 27px 差の flaky になっていた
 * (親番号 "" vs "1"、起点 左 vs 右)。マシン負荷が変わるだけで結果が変わる種類の不安定さで、
 * 実際 test を 1 本足しただけで再現するようになった。
 *
 * 時間ではなく「変化が止まったこと」を待つ。
 */
async function screenshotWhenSettled(name: string): Promise<void> {
  await waitQuiescent();
  await expectScreenshotToMatch(name);
}

beforeEach(async () => {
  // テスト間の状態汚染を防ぐため、アプリごと初期状態へ戻す
  await resetApp();
});

describe('VRT Scenarios (主要UI状態のビジュアル回帰テスト)', () => {
  it('1. 初期状態 (標準UIレイアウトと基準三角形1つ)', async () => {
    // 基準の三角形1つだけの状態をロードして初期表示を検証
    await loadCsv('1,3.00,3.00,3.00,-1,-1');
    await new Promise(r => setTimeout(r, 200)); // レンダリング待ち
    
    await screenshotWhenSettled('vrt-scenario-1-initial-state');
  });

  it('2. 新規追加時のシャドープレビュー (入力中の仮図形表示)', async () => {
    // 初期化
    await loadCsv('1,3.00,3.00,3.00,-1,-1');
    await new Promise(r => setTimeout(r, 200));

    // 1. 基準三角形(tri=1) の 辺2 (side=1) の中心をタップして、接続先に設定
    const s = await state();
    const line = s.prims?.find(
      (p: any) => p.type === 'line' && p.layer === 'tri' && p.tri === 1 && p.side === 1
    );
    if (!line) throw new Error('基準三角形の辺B(side=1)が見つかりません');

    const tx = (line.x1 + line.x2) / 2;
    const ty = (line.y1 + line.y2) / 2;
    await tap(tx, ty);
    await new Promise(r => setTimeout(r, 100));

    // 2. フォームに寸法を入力し、確定前のシャドープレビューを発生させる
    await key('newB', '4.00');
    await key('newC', '4.00');
    await new Promise(r => setTimeout(r, 250)); // プレビュー描画待ち

    // シャドープレビューが表示されている状態でビジュアル検証
    await screenshotWhenSettled('vrt-scenario-2-shadow-preview');
  });

  it('3. 複雑な混成チェーンの描画 (台形 + 三角形接続レイアウト)', async () => {
    // 台形に三角形を接続し、さらに台形を接続した3段構成
    const csv = [
      'Rectangle,1,3.00,10.00,7.00,-1,0,0,0,0,RecStation,,,,4', // 1段目: 台形
      '2,4.00,4.00,4.00,1,1,,,,4',                             // 2段目: 三角形 (1段目の辺Bに接続)
      'Rectangle,3,2.00,3.00,2.00,2,2,0,1,0,SubStation,,,,4'      // 3段目: 台形 (2段目の辺Cに接続)
    ].join('\n');
    
    await loadCsv(csv);
    await new Promise(r => setTimeout(r, 250)); // レンダリング待ち
    
    await screenshotWhenSettled('vrt-scenario-3-complex-chain');
  });

  it('4. 控除モードへの切替 (控除編集モードのUIスタイル)', async () => {
    // 3段の複雑なチェーンがある状態から控除モードへ切替
    const csv = [
      'Rectangle,1,3.00,10.00,7.00,-1,0,0,0,0,RecStation,,,,4',
      '2,4.00,4.00,4.00,1,1,,,,4',
      'Rectangle,3,2.00,3.00,2.00,2,2,0,1,0,SubStation,,,,4'
    ].join('\n');
    await loadCsv(csv);
    await new Promise(r => setTimeout(r, 100));

    // 控除ボタンをクリックして控除編集モードをONにする
    await click('fabDeduction');
    await new Promise(r => setTimeout(r, 250)); // モード切替のUI反映待ち

    // 控除モード特有の配色（背景赤系、ボタン色変化等）をビジュアル検証
    await screenshotWhenSettled('vrt-scenario-4-deduction-mode');

    // テスト後片付けとして控除モードをOFFに戻す
    await click('fabDeduction');
    await new Promise(r => setTimeout(r, 100));
  });

  it('5. 台形のD辺(side=3)タップ選択解除時の挙動 (D辺寸法が表示されっぱなしにならないこと)', async () => {
    // 3段の複雑なチェーンをロード
    const csv = [
      'Rectangle,1,3.00,10.00,7.00,-1,0,0,0,0,RecStation,,,,4',
      '2,4.00,4.00,4.00,1,1,,,,4',
      'Rectangle,3,2.00,3.00,2.00,2,2,0,1,0,SubStation,,,,4'
    ].join('\n');
    await loadCsv(csv);
    await new Promise(r => setTimeout(r, 100));

    // 台形3(tri=3) の D辺(side=3) の中心をタップして、辺選択ガイドを表示させる
    const s = await state();
    const line = s.prims?.find(
      (p: any) => p.type === 'line' && p.layer === 'tri' && p.tri === 3 && p.side === 3
    );
    if (!line) throw new Error('台形3のD辺(side=3)が見つかりません');

    const tx = (line.x1 + line.x2) / 2;
    const ty = (line.y1 + line.y2) / 2;
    await tap(tx, ty);
    await new Promise(r => setTimeout(r, 200));

    // 選択を解除するために適当な何もない座標(0, 0)をタップ
    await tap(0, 0);
    await new Promise(r => setTimeout(r, 200));

    // 選択解除後に、D辺の寸法（長さ2.2など）が表示されっぱなしになっていないかをVRTで検証
    await screenshotWhenSettled('vrt-scenario-5-rectangle-d-side-no-dim');
  });
});
