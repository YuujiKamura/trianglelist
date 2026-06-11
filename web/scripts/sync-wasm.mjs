// Kotlin の wasmJs 成果物を web/ へ振り分けコピーする。dev/build の前に必ず走る。
//
// 振り分けの理由 (Vite の流儀):
// - .mjs / .uninstantiated.mjs (純 JS) -> web/wasm/
//   src から静的 import してバンドルに乗せる。public 内ファイルの JS import は
//   Vite dev が禁止しているため public には置けない
// - .wasm (+ .wasm.map) -> web/public/ 直下
//   Kotlin 生成の uninstantiated.mjs はブラウザでは fetch('./TriangleList-common-wasm-js.wasm')
//   = document base 相対で読むので、ルート (/) に as-is で置く必要がある
//
// パスはこのファイル位置からの相対解決のみ (ハードコードパス禁止ルール)。
import { copyFileSync, existsSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const src = resolve(here, '../../common/build/compileSync/wasmJs/main/productionExecutable/optimized');
const jsDst = resolve(here, '../wasm');
const publicDst = resolve(here, '../public');

if (!existsSync(src)) {
  console.error(`wasm dist not found: ${src}`);
  console.error('Run first: ./gradlew :common:wasmJsBrowserDistribution');
  process.exit(1);
}

mkdirSync(jsDst, { recursive: true });
mkdirSync(publicDst, { recursive: true });

const jsFiles = [
  'TriangleList-common-wasm-js.mjs',
  'TriangleList-common-wasm-js.uninstantiated.mjs',
  'TriangleList-common-wasm-js.d.ts',
];
const wasmFiles = ['TriangleList-common-wasm-js.wasm', 'TriangleList-common-wasm-js.wasm.map'];

for (const f of jsFiles) {
  copyFileSync(resolve(src, f), resolve(jsDst, f));
}
for (const f of wasmFiles) {
  if (!existsSync(resolve(src, f))) continue;
  // Kotlin 2.2.x の uninstantiated.mjs は .wasm を new URL('./...', import.meta.url) =
  // モジュール相対で fetch する (旧: document base 相対)。.mjs と同じ web/wasm/ に置くのが本命。
  // public 直下にも残すのは旧版キャッシュ/直リンク互換のため (無害)
  copyFileSync(resolve(src, f), resolve(jsDst, f));
  copyFileSync(resolve(src, f), resolve(publicDst, f));
}
console.log(`synced: ${jsFiles.length} js files -> ${jsDst}, wasm -> ${jsDst} + ${publicDst}`);
