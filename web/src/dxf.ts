// DXF 表示ページ — dxf-viewer npm (vagran, WebGL/three.js) をそのまま使う。
// アプリ/desktop が書き出した既存 DXF をファイル選択 or D&D で表示する。
// Kotlin 側の工事はゼロ (buy-vs-build: insight #63)。

import { DxfViewer } from 'dxf-viewer';
import { Color } from 'three';

// dxf-viewer-example-src と同じフォント群 (raw TTF、グリフ欠落時に lazy-load)。
// HanaMinA が CJK を持つので Shift_JIS→UTF-8 変換済みの日本語 TEXT も描画対象になる。
const FONT_BASE =
  'https://raw.githubusercontent.com/vagran/dxf-viewer-example-src/master/src/assets/fonts/';
const FONTS = [
  FONT_BASE + 'Roboto-LightItalic.ttf',
  FONT_BASE + 'NotoSansDisplay-SemiCondensedLightItalic.ttf',
  FONT_BASE + 'HanaMinA.ttf',
  FONT_BASE + 'NanumGothic-Regular.ttf',
];

function setStatus(msg: string): void {
  const el = document.getElementById('status');
  if (el) el.textContent = msg;
}

// 既存アプリの DXF は Shift_JIS で書かれていることがある (golden fixture も SJIS)。
// dxf-viewer は UTF-8 前提で fetch するので、UTF-8 として壊れるバイト列なら
// shift_jis でデコードし直して UTF-8 Blob にしてから渡す。
function decodeDxf(buf: ArrayBuffer): { text: string; encoding: string } {
  try {
    return { text: new TextDecoder('utf-8', { fatal: true }).decode(buf), encoding: 'utf-8' };
  } catch {
    return { text: new TextDecoder('shift_jis').decode(buf), encoding: 'shift_jis' };
  }
}

const container = document.getElementById('container');
if (!(container instanceof HTMLElement)) throw new Error('#container not found');

const viewer = new DxfViewer(container, {
  clearColor: new Color('#ffffff'), // 白背景 (three.Color インスタンス必須 — DxfViewer.js:34 が getHex() を呼ぶ)
  autoResize: true,
  colorCorrection: true,
});

let currentUrl: string | null = null;

async function loadDxfFile(file: File): Promise<void> {
  setStatus(`${file.name} 読み込み中...`);
  try {
    const buf = await file.arrayBuffer();
    const { text, encoding } = decodeDxf(buf);
    if (currentUrl) URL.revokeObjectURL(currentUrl);
    currentUrl = URL.createObjectURL(new Blob([text], { type: 'application/dxf' }));
    await viewer.Load({
      url: currentUrl,
      fonts: FONTS,
      progressCbk: (phase) => setStatus(`${file.name}: ${phase}...`),
      workerFactory: null, // main thread で十分 (図面サイズ小、worker 構成は Vite 都合で省略)
    });
    setStatus(`${file.name} 表示中 (${encoding})`);
  } catch (e) {
    setStatus(`読み込み失敗: ${String(e)}`);
    console.error(e);
  }
}

const fileInput = document.getElementById('file') as HTMLInputElement | null;
fileInput?.addEventListener('change', () => {
  const file = fileInput.files?.[0];
  if (file) void loadDxfFile(file);
});

container.addEventListener('dragover', (e) => {
  e.preventDefault();
  container.classList.add('dragover');
});
container.addEventListener('dragleave', () => container.classList.remove('dragover'));
container.addEventListener('drop', (e) => {
  e.preventDefault();
  container.classList.remove('dragover');
  const file = e.dataTransfer?.files?.[0];
  if (file) void loadDxfFile(file);
});
