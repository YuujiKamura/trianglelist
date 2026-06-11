import { defineConfig, type Plugin, type ViteDevServer } from 'vite';
import { fileURLToPath } from 'node:url';

// __tlcp: dev 専用の control protocol (keysynth の gui_cp と同じ思想の web 版)。
// 物理カメラ越しでなく canvas のピクセルそのものと UI state を CLI から取る検証口。
//   GET /__tlcp/capture → ページの canvas.toDataURL() を PNG バイトで返す
//   GET /__tlcp/state   → rows/selected/current/overrides/csv を JSON で返す
// トランスポートは Vite 標準の HMR WebSocket (server.ws) — 外部依存ゼロ、prod build には乗らない。
// ページ側の応答は web/src/main.ts 末尾の import.meta.hot ブロック。
function tlcpPlugin(): Plugin {
  type Pending = { resolve: (data: unknown) => void; timer: ReturnType<typeof setTimeout> };
  const pending = new Map<string, Pending>();
  let seq = 0;

  const request = (server: ViteDevServer, channel: 'capture' | 'state') =>
    new Promise<Record<string, unknown>>((resolve, reject) => {
      const id = String(++seq);
      const timer = setTimeout(() => {
        pending.delete(id);
        reject(new Error('tlcp: page did not respond in 3s (ブラウザでページが開いているか確認)'));
      }, 3000);
      pending.set(id, { resolve: resolve as (data: unknown) => void, timer });
      server.ws.send(`tlcp:${channel}-req`, { id });
    });

  return {
    name: 'tlcp',
    configureServer(server) {
      for (const channel of ['capture', 'state'] as const) {
        server.ws.on(`tlcp:${channel}-res`, (data: { id: string }) => {
          const p = pending.get(data.id);
          if (!p) return; // タブが複数開いていたら最初の応答だけ採用
          clearTimeout(p.timer);
          pending.delete(data.id);
          p.resolve(data);
        });
      }
      server.middlewares.use(async (req, res, next) => {
        if (!req.url) return next();
        if (req.url.startsWith('/__tlcp/capture')) {
          try {
            const data = await request(server, 'capture');
            const b64 = String(data.png ?? '').split(',')[1] ?? '';
            res.setHeader('Content-Type', 'image/png');
            res.end(Buffer.from(b64, 'base64'));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        if (req.url.startsWith('/__tlcp/state')) {
          try {
            const data = await request(server, 'state');
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        next();
      });
    },
  };
}

// Kotlin/Wasm 成果物 (common.mjs + .wasm) は public/wasm/ に static asset として置き、
// 実行時に dynamic import する方式 (bundler 統合を避ける、kotlin-wasm-browser-template 準拠)。
// そのため特別な wasm プラグインは不要。
export default defineConfig({
  plugins: [tlcpPlugin()],
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
