// /__tlcp/* (vite tlcpPlugin) を叩く thin wrapper。 dev サーバが立ち上がっていて
// 裏で headless chromium が常駐している前提 (web/vite.config.ts の tlcpPlugin)。
// テスト実装側は CP の HTTP 経路を意識せず、 型のついた関数呼び出しで使う。

const DEFAULT_BASE = process.env.TLCP_BASE ?? 'http://localhost:5173';

export interface CpRow {
  kind: 'triangle' | 'rectangle' | 'recttri';
  a: string;
  b: string;
  c: string;
  parent: string;
  conn: string;
  parentKind?: number;
  align?: number;
  extras?: string[];
}

export interface CpLinePrim {
  type: 'line';
  layer: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  tri?: number;
  side?: number;
  ded?: number;
}

export interface CpFillPrim {
  type: 'fill';
  layer: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  x3: number;
  y3: number;
  color: number;
  tri?: number;
}

export interface CpTextPrim {
  type: 'text';
  layer: string;
  text: string;
  x: number;
  y: number;
  angle: number;
  size: number;
  align: number;
  tri?: number;
  side?: number;
}

export type CpPrim = CpLinePrim | CpFillPrim | CpTextPrim | { type: 'circle'; layer: string; cx: number; cy: number; r: number; tri?: number };

export interface CpState {
  rows: CpRow[];
  selected: number;
  current: number;
  prims?: CpPrim[];
  csv?: string;
  view?: { scale: number; offsetX: number; offsetY: number } | null;
}

async function get(path: string): Promise<Response> {
  const url = `${DEFAULT_BASE}${path}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`CP ${path} → ${res.status}: ${await res.text().catch(() => '')}`);
  return res;
}

export async function health(): Promise<boolean> {
  try {
    const res = await fetch(`${DEFAULT_BASE}/`);
    return res.ok;
  } catch {
    return false;
  }
}

export async function loadCsv(csv: string): Promise<{ ok: boolean; rows: number; listAngle: number }> {
  const res = await fetch(`${DEFAULT_BASE}/__tlcp/load`, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body: csv,
  });
  if (!res.ok) throw new Error(`CP load → ${res.status}`);
  return res.json();
}

export async function state(): Promise<CpState> {
  const res = await get('/__tlcp/state');
  return res.json() as Promise<CpState>;
}

export async function select(n: number): Promise<{ ok: boolean; selected: number; current: number }> {
  const res = await get(`/__tlcp/select?n=${n}`);
  return res.json();
}

export async function click(target: string): Promise<{ ok: boolean; status?: string }> {
  const res = await get(`/__tlcp/click?target=${encodeURIComponent(target)}`);
  return res.json();
}

export async function pageBuffer(): Promise<Buffer> {
  const res = await get('/__tlcp/page');
  const arr = await res.arrayBuffer();
  return Buffer.from(arr);
}
