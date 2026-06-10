import { defineConfig } from 'vite';
import { fileURLToPath } from 'node:url';

// Kotlin/Wasm 成果物 (common.mjs + .wasm) は public/wasm/ に static asset として置き、
// 実行時に dynamic import する方式 (bundler 統合を避ける、kotlin-wasm-browser-template 準拠)。
// そのため特別な wasm プラグインは不要。
export default defineConfig({
  build: {
    target: 'esnext',
    rollupOptions: {
      // マルチページ: / = CSV 三角形リスト (段階1)、/dxf.html = DXF ビューワ (dxf-viewer npm)
      input: {
        main: fileURLToPath(new URL('./index.html', import.meta.url)),
        dxf: fileURLToPath(new URL('./dxf.html', import.meta.url)),
      },
    },
  },
});
