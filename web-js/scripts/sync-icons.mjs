// アプリの FAB アイコン PNG (app/src/main/res/drawable-nodpi) を web/public/icons/ へコピーする。
// SoT はアプリ側 — アイコンを描き直したら dev/build の度にここで web へも反映される
// (sync-wasm.mjs と同じ「ビルド時コピーで二重管理を避ける」流儀)。
// 対応表の正は app/src/main/res/layout/fabs.xml の app:srcCompat。
// パスはこのファイル位置からの相対解決のみ (ハードコードパス禁止ルール)。
import { copyFileSync, existsSync, mkdirSync, readdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const src = resolve(here, '../../app/src/main/res/drawable-nodpi');
const dst = resolve(here, '../public/icons');

if (!existsSync(src)) {
  console.error(`app drawable-nodpi not found: ${src}`);
  process.exit(1);
}

mkdirSync(dst, { recursive: true });
let n = 0;
for (const f of readdirSync(src)) {
  if (!f.endsWith('.png')) continue;
  copyFileSync(resolve(src, f), resolve(dst, f));
  n++;
}
console.log(`synced: ${n} icons -> ${dst}`);
