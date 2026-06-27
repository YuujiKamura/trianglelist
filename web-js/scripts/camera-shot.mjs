// 修正の目視確認用 (テスト不可、目で見る用)。
// AUTOSAVE_KEY 経由で CSV を localStorage に入れ reload → canvas をクリップ撮影。
import { chromium } from 'playwright';

const url = process.argv[2] || 'http://localhost:5173/';
const out = process.argv[3] || 'C:/Users/yuuji/.camera-eye/web-shot.png';
// 中央揃え (alignment=1) 台形 (length=5, widthA=10, widthB=4) を独立で 1 個。
// → 上辺 4 + 底辺 10 で対称、垂線 5 が真ん中、直角マーカーと延長 5.0 寸法・上下辺 寸法のみ
//   斜辺 寸法は出ない、guide 線が垂線そのもの
const csv = process.argv[4] || 'Rectangle,1,5,10,4,-1,0,1\n';

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
const errors = [];
page.on('pageerror', (e) => errors.push(`pageerror: ${e.message}`));
page.on('console', (m) => { if (m.type() === 'error') errors.push(`console: ${m.text()}`); });

await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });

// 1. AUTOSAVE 経由で CSV を流し込み reload
await page.evaluate((csv) => {
  localStorage.setItem('trianglelist.web.autosave.csv',
    JSON.stringify({ csv, overrides: { dims: [], numbers: [] } }));
}, csv);
await page.reload({ waitUntil: 'networkidle' });
await page.waitForTimeout(1000);

// 2. canvas のみクリップ
const canvas = await page.$('#cv');
if (canvas) {
  await canvas.screenshot({ path: out });
} else {
  await page.screenshot({ path: out });
}
await browser.close();
console.log(`shot: ${out}`);
if (errors.length) console.log('errors:\n' + errors.join('\n'));
