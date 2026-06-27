import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import { pageBuffer } from './cpClient.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * 現在のブラウザ画面をキャプチャし、基準画像（Golden）と比較します。
 * @param snapshotName 保存するスナップショットの識別名
 * @param threshold 差分検知のしきい値 (0.0〜1.0)
 */
export async function expectScreenshotToMatch(snapshotName: string, threshold = 0.1): Promise<void> {
  const snapshotDir = path.resolve(__dirname, '../__snapshots__');
  const goldenPath = path.join(snapshotDir, `${snapshotName}.png`);
  const currentPath = path.join(snapshotDir, `${snapshotName}-current.png`);
  const diffPath = path.join(snapshotDir, `${snapshotName}-diff.png`);

  // 環境変数 VRT_UPDATE=true が指定されているかチェック
  const shouldUpdate = process.env.VRT_UPDATE === 'true';

  // 1. cpClient を通じて現在のブラウザバッファを取得
  const currentBuffer = await pageBuffer();

  // 2. 基準画像（Golden）が存在しない、または更新モードの場合
  if (!fs.existsSync(goldenPath) || shouldUpdate) {
    // ディレクトリがなければ作成して保存
    if (!fs.existsSync(snapshotDir)) {
      fs.mkdirSync(snapshotDir, { recursive: true });
    }
    fs.writeFileSync(goldenPath, currentBuffer);
    console.log(`\n[VRT] 基準画像を保存/更新しました: ${goldenPath}`);
    return;
  }

  // 3. 画像の読み込みとデコード
  const goldenPng = PNG.sync.read(fs.readFileSync(goldenPath));
  const currentPng = PNG.sync.read(currentBuffer);
  const { width, height } = goldenPng;
  
  if (currentPng.width !== width || currentPng.height !== height) {
    // 画像サイズが異なる場合は即時失敗させ、現在画像を書き出す
    fs.writeFileSync(currentPath, currentBuffer);
    throw new Error(
      `[VRT Mismatch] 画像サイズが異なります。\n` +
      `期待値: ${width}x${height}, 実測値: ${currentPng.width}x${currentPng.height}\n` +
      `現在の画面イメージ: ${currentPath}`
    );
  }

  const diffPng = new PNG({ width, height });

  // 4. ピクセル差分の比較を実行
  const mismatchedPixels = pixelmatch(
    goldenPng.data,
    currentPng.data,
    diffPng.data,
    width,
    height,
    { threshold }
  );

  // 5. 差分があった場合はエラーをスローし、デバッグ用の画像を書き出す
  if (mismatchedPixels > 0) {
    fs.writeFileSync(currentPath, currentBuffer);
    fs.writeFileSync(diffPath, PNG.sync.write(diffPng));
    
    throw new Error(
      `[VRT Mismatch] "${snapshotName}" のビジュアルテストが失敗しました。\n` +
      `不一致ピクセル数: ${mismatchedPixels}px\n` +
      `差分画像を確認してください: ${diffPath}\n` +
      `現在の画面イメージ: ${currentPath}`
    );
  }

  // テストが成功した場合は、過去のデバッグ画像を削除する（クリーンアップ）
  if (fs.existsSync(diffPath)) fs.unlinkSync(diffPath);
  if (fs.existsSync(currentPath)) fs.unlinkSync(currentPath);
}
