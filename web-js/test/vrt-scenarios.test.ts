import { describe, it, expect, beforeAll, beforeEach } from 'vitest';
import { health, state, loadCsv, click, tap, key } from './combo/cpClient.ts';
import { expectScreenshotToMatch } from './combo/vrt.ts';

beforeAll(async () => {
  const ok = await health();
  if (!ok) throw new Error('dev server (http://localhost:5173) が立ってない');
}, 10_000);

beforeEach(async () => {
  // テスト間の状態汚染を防ぐため、フォームの入力をすべてクリアする
  await key('newA', '');
  await key('newB', '');
  await key('newC', '');
  await key('newParent', '');
  await key('newConn', '0');
  await key('newCType', '0');
  await key('newLcr', '0');
  await tap(0, 0); // 選択状態のクリア
  await new Promise(r => setTimeout(r, 200));
});

describe('VRT Scenarios (主要UI状態のビジュアル回帰テスト)', () => {
  it('1. 初期状態 (標準UIレイアウトと基準三角形1つ)', async () => {
    // 基準の三角形1つだけの状態をロードして初期表示を検証
    await loadCsv('1,3.00,3.00,3.00,-1,-1');
    await new Promise(r => setTimeout(r, 200)); // レンダリング待ち
    
    await expectScreenshotToMatch('vrt-scenario-1-initial-state');
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
    await expectScreenshotToMatch('vrt-scenario-2-shadow-preview');
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
    
    await expectScreenshotToMatch('vrt-scenario-3-complex-chain');
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
    await expectScreenshotToMatch('vrt-scenario-4-deduction-mode');

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
    await expectScreenshotToMatch('vrt-scenario-5-rectangle-d-side-no-dim');
  });
});
