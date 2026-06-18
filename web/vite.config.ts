import { defineConfig, type Plugin, type ViteDevServer } from 'vite';
import { fileURLToPath } from 'node:url';
import { execSync } from 'node:child_process';

// ビルド識別: 「どのコミットが・いつ生成したビルドか」をヘッダ右上に出すための値。
// Pages は手動デプロイ (deploy.yml の workflow_dispatch) なので、画面を見れば反映済みか
// 一目で分かるようにする。値はビルド時に焼き込み、main.ts の __BUILD_COMMIT__/__BUILD_TIME__
// (下の define) として参照する。CI の checkout でも git は使えるのでローカルと同じ経路。
function gitShortCommit(): string {
  try {
    return execSync('git rev-parse --short HEAD').toString().trim();
  } catch {
    return 'unknown';
  }
}
const BUILD_COMMIT = process.env.VITE_BUILD_COMMIT ?? gitShortCommit();
// 生成時刻は JST で "YYYY-MM-DD HH:mm" に整形 (CI runner は UTC なので timeZone を明示)。
const BUILD_TIME =
  process.env.VITE_BUILD_TIME ??
  new Date()
    .toLocaleString('sv-SE', { timeZone: 'Asia/Tokyo', dateStyle: 'short', timeStyle: 'short' });

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

  const request = (server: ViteDevServer, channel: 'capture' | 'state' | 'tap' | 'edit' | 'key' | 'click' | 'load' | 'page' | 'select' | 'options', payload: Record<string, unknown> = {}, timeoutMs = 3000) =>
    new Promise<Record<string, unknown>>((resolve, reject) => {
      const id = String(++seq);
      const timer = setTimeout(() => {
        pending.delete(id);
        reject(new Error(`tlcp: page did not respond in ${timeoutMs}ms (ブラウザでページが開いているか確認)`));
      }, timeoutMs);
      pending.set(id, { resolve: resolve as (data: unknown) => void, timer });
      server.ws.send(`tlcp:${channel}-req`, { id, ...payload });
    });

  // dev 起動時に裏で headless chromium を常駐させて localhost を開かせる。
  // これで「ユーザーが chrome を前に出していなくても」 CP 越しに /__tlcp/page も capture も
  // バッファを取れる ── ws の応答役は人間が開く chrome ではなく裏の headless chromium。
  let bgClose: (() => Promise<void>) | null = null;

  return {
    name: 'tlcp',
    async closeBundle() {
      if (bgClose) { await bgClose(); bgClose = null; }
    },
    configureServer(server) {
      server.httpServer?.once('listening', async () => {
        try {
          const addr = server.httpServer?.address();
          const port = typeof addr === 'object' && addr ? addr.port : 5173;
          const { chromium } = await import('playwright');
          const browser = await chromium.launch({ headless: true });
          const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
          const page = await context.newPage();
          await page.goto(`http://localhost:${port}/`);
          bgClose = async () => { await browser.close(); };
          console.log('[tlcp] headless chromium attached');
        } catch (e) {
          console.warn('[tlcp] headless chromium not available:', (e as Error).message);
        }
      });
      for (const channel of ['capture', 'state', 'tap', 'edit', 'key', 'click', 'load', 'page', 'select', 'options'] as const) {
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
        // ページ全体スクショ: GET /__tlcp/page — canvas だけの capture と違い、FAB 等の
        // DOM 込みで撮る (snapDOM が DOM を SVG 経由でラスタライズ。フロントバッファの
        // 厳密なピクセルではないが UI 検証には十分)。timeout はラスタライズ分長め
        if (req.url.startsWith('/__tlcp/page')) {
          try {
            const data = await request(server, 'page', {}, 10000);
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
        // 編集注入: GET /__tlcp/edit?tri=<番号>&key=<a|b|c>&value=<長さ> — 一覧セル編集と
        // 同じ動線 (row 書換え → redraw) を CLI から踏む。三角不等式関門の実走検証口
        if (req.url.startsWith('/__tlcp/edit')) {
          try {
            const q = new URL(req.url, 'http://localhost').searchParams;
            const data = await request(server, 'edit', {
              tri: Number(q.get('tri')),
              key: q.get('key'),
              value: q.get('value'),
            });
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        // キー注入: GET /__tlcp/key?target=<要素id>&value=<値>&key=Enter — キーボード UX の検証口
        if (req.url.startsWith('/__tlcp/key')) {
          try {
            const q = new URL(req.url, 'http://localhost').searchParams;
            const data = await request(server, 'key', {
              target: q.get('target'),
              key: q.get('key') ?? undefined,
              value: q.get('value') ?? undefined,
            });
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        // クリック注入: GET /__tlcp/click?target=<要素id> — ボタン動線の検証口
        if (req.url.startsWith('/__tlcp/click')) {
          try {
            const q = new URL(req.url, 'http://localhost').searchParams;
            const data = await request(server, 'click', { target: q.get('target') });
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        // CSV 注入: POST /__tlcp/load (body = CSV テキスト) — ファイルダイアログを経ずに
        // CSV を開く検証口。完全形式 CSV (手動配置列・golden 比較) の e2e に使う
        if (req.url.startsWith('/__tlcp/load')) {
          try {
            const chunks: Buffer[] = [];
            for await (const c of req) chunks.push(c as Buffer);
            const csv = Buffer.concat(chunks).toString('utf-8');
            const data = await request(server, 'load', { csv });
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        // option 列挙: GET /__tlcp/options?id=<select 要素 id> — <select> の option value/text を返す
        // (kind-toggle test で D 辺 option 出力を実 DOM で pin するため)
        if (req.url.startsWith('/__tlcp/options')) {
          try {
            const q = new URL(req.url, 'http://localhost').searchParams;
            const data = await request(server, 'options', { target: q.get('id') });
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        // 選択注入: GET /__tlcp/select?n=<番号> — hitTriangle が拾わない図形 (台形等) を直接 selected に
        if (req.url.startsWith('/__tlcp/select')) {
          try {
            const q = new URL(req.url, 'http://localhost').searchParams;
            const data = await request(server, 'select', { n: Number(q.get('n')) });
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(data.state));
          } catch (e) {
            res.statusCode = 503;
            res.end(String(e));
          }
          return;
        }
        // タップ注入: GET /__tlcp/tap?x=<model x>&y=<model y> — UX 検証をスクリプト化する口
        if (req.url.startsWith('/__tlcp/tap')) {
          try {
            const q = new URL(req.url, 'http://localhost').searchParams;
            const data = await request(server, 'tap', {
              x: Number(q.get('x')),
              y: Number(q.get('y')),
            });
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
  // GitHub Pages (project site = /trianglelist/) 用に base を環境変数で差し替える。
  // dev とローカル build は従来どおり '/'。
  base: process.env.VITE_BASE ?? '/',
  define: {
    __BUILD_COMMIT__: JSON.stringify(BUILD_COMMIT),
    __BUILD_TIME__: JSON.stringify(BUILD_TIME),
  },
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

// tlcp: server restart marker r1
