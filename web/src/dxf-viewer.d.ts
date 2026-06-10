// dxf-viewer (1.0.x) は型定義を同梱しないので、使う範囲だけ最小宣言する。
// API は github.com/vagran/dxf-viewer の README と example-src の DxfViewer.vue 準拠。
// three は dxf-viewer の依存として node_modules に居るが型定義を同梱しない。
// 使うのは Color だけなので最小宣言 (@types/three を入れるほどではない)。
declare module 'three' {
  export class Color {
    constructor(color?: string | number);
    getHex(): number;
  }
}

declare module 'dxf-viewer' {
  export interface DxfViewerLoadParams {
    url: string;
    fonts?: string[] | null;
    progressCbk?: (phase: 'font' | 'fetch' | 'parse' | 'prepare', size: number, totalSize: number | null) => void;
    workerFactory?: (() => Worker) | null;
  }

  export class DxfViewer {
    constructor(domContainer: HTMLElement, options?: Record<string, unknown> | null);
    Load(params: DxfViewerLoadParams): Promise<void>;
    Clear(): void;
    Destroy(): void;
    Subscribe(eventName: string, eventHandler: (event: unknown) => void): void;
    SetSize(width: number, height: number): void;
    GetDxf(): unknown;
  }
}
