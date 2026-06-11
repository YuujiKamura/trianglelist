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
  // ADR 0008: overrides 焼き込み済みの完全形式 28 列 CSV (手動配置の書き戻し)
  export function buildCsvTextWithOverrides(csv: string, overridesJson: string): string;
}
