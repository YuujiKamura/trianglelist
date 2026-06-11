// Kotlin generateTypeScriptDefinitions の出力 (web/wasm/TriangleList-common-wasm-js.d.ts)
// と同じ宣言。tsc は .mjs import に .d.mts しか対応付けないため、ここで module 宣言する
declare module '*/TriangleList-common-wasm-js.mjs' {
  export function renderCsvToPrimitives(csv: string, scale: number): string;
  export function buildDxfText(csv: string): string;
  export function buildSfcText(csv: string, filename: string): string;
  export function hitTriangle(csv: string, x: number, y: number): number;
  // 段階2e (task #15): overrides 付き経路。overridesJson は WebOverrides の JSON 形式
  export function renderCsvToPrimitivesWithOverrides(csv: string, scale: number, overridesJson: string): string;
  export function buildDxfTextWithOverrides(csv: string, overridesJson: string): string;
  export function buildSfcTextWithOverrides(csv: string, filename: string, overridesJson: string): string;
  // 番号逆順 (アプリ保存ダイアログの NumReverse) 付き DXF/SFC。CSV 保存には影響しない
  export function buildDxfTextNumReverse(csv: string, overridesJson: string, numReverse: boolean): string;
  export function buildSfcTextNumReverse(csv: string, filename: string, overridesJson: string, numReverse: boolean): string;
  // ADR 0008: overrides 焼き込み済みの完全形式 28 列 CSV (手動配置の書き戻し)
  export function buildCsvTextWithOverrides(csv: string, overridesJson: string): string;
  // 控除編集: クリック位置 (モデル座標) に配置 → 13 列 Deduction CSV 行 (不正は空文字列)
  export function placeDeduction(csv: string, x: number, y: number, name: string, lenX: number, lenY: number, num: number): string;
  // 全体回転への控除連動: Deduction 行 1 本を degrees 回転して返す
  export function rotateDeductionLine(line: string, degrees: number): string;
  // 控除モードの rot FAB: 選択控除を自身の中心回りに回す (位置不動、Box の shapeAngle のみ)
  export function rotateDeductionShape(line: string, degrees: number): string;
  // 図面枠 (A3、DXF の writeDrawingFrame と同形) を prim JSON 配列で返す。layer "frame"
  export function renderFrame(csv: string): string;
}
