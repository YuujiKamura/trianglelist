// Kotlin generateTypeScriptDefinitions の出力 (web/wasm/TriangleList-common-wasm-js.d.ts)
// と同じ宣言。tsc は .mjs import に .d.mts しか対応付けないため、ここで module 宣言する
declare module '*/TriangleList-common-wasm-js.mjs' {
  export function renderCsvToPrimitives(csv: string, scale: number): string;
}
