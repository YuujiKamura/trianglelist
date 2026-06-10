// Kotlin の wasmJs ESM 成果物を web/public/wasm/ へコピーする。
// 取り込み元は webpack バンドル (dist/) ではなく compileSync の optimized 出力
// (TriangleList-common-wasm-js.mjs + .wasm + .d.ts) — named export の ESM を
// そのまま dynamic import するため。dev/build の前に必ず走る (package.json scripts)。
// パスはこのファイル位置からの相対解決のみ (ハードコードパス禁止ルール)。
import { cpSync, existsSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const src = resolve(here, '../../common/build/compileSync/wasmJs/main/productionExecutable/optimized');
const dst = resolve(here, '../public/wasm');

if (!existsSync(src)) {
  console.error(`wasm dist not found: ${src}`);
  console.error('Run first: ./gradlew :common:wasmJsBrowserDistribution');
  process.exit(1);
}

mkdirSync(dst, { recursive: true });
cpSync(src, dst, { recursive: true });
console.log(`synced wasm dist -> ${dst}`);
