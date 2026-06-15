// CP /__tlcp/state の prims 配列を tri 番号別に分類し、 アサーションの基礎指標を出す。
// 「期待 line 数 / 砂時計検査 / chain 連結性 / 寸法ラベル数」 を spec で同じパターンで使う。

import type { CpLinePrim, CpPrim } from './cpClient.ts';

export interface PerTri {
  triLines: CpLinePrim[];
  /** chain 連結可否 — 4 辺なら chain 連結成立 = 砂時計 X クロスにならない、 不成立 = 警告 */
  chainClosed: boolean;
  /** 砂時計 (蝶ネクタイ) 形状判定: 4 辺で端点グラフが 2 つ以上の独立 chain に分かれてる場合 true */
  hourglass: boolean;
}

const EPS = 1e-3;
const eq = (a: number, b: number): boolean => Math.abs(a - b) < EPS;

export function groupByTri(prims: CpPrim[]): Map<number, CpLinePrim[]> {
  const m = new Map<number, CpLinePrim[]>();
  for (const p of prims) {
    if (p.type !== 'line' || p.layer !== 'tri') continue;
    if (typeof p.tri !== 'number') continue;
    const arr = m.get(p.tri) ?? [];
    arr.push(p);
    m.set(p.tri, arr);
  }
  return m;
}

/**
 * lines を端点で連結試行して「全 line が 1 本の閉路に並ぶか」 を判定。
 * 砂時計 X クロスは「全部の line を端点で繋いだ時に 2 つに分裂する」 のと等価
 */
export function chainAndClose(lines: CpLinePrim[]): { closed: boolean; visited: number } {
  if (lines.length === 0) return { closed: false, visited: 0 };
  const used = new Array(lines.length).fill(false);
  used[0] = true;
  const startX = lines[0].x1;
  const startY = lines[0].y1;
  let curX = lines[0].x2;
  let curY = lines[0].y2;
  let visited = 1;
  while (visited < lines.length) {
    let advanced = false;
    for (let i = 0; i < lines.length; i++) {
      if (used[i]) continue;
      const ln = lines[i];
      if (eq(ln.x1, curX) && eq(ln.y1, curY)) {
        used[i] = true;
        curX = ln.x2;
        curY = ln.y2;
        advanced = true;
        visited++;
        break;
      }
      if (eq(ln.x2, curX) && eq(ln.y2, curY)) {
        used[i] = true;
        curX = ln.x1;
        curY = ln.y1;
        advanced = true;
        visited++;
        break;
      }
    }
    if (!advanced) break;
  }
  const closed = visited === lines.length && eq(curX, startX) && eq(curY, startY);
  return { closed, visited };
}

export function analyzeTri(lines: CpLinePrim[]): PerTri {
  const { closed, visited } = chainAndClose(lines);
  // 4 辺以上なのに chain が 1 本で閉じない場合 = 砂時計 X クロス候補
  const hourglass = lines.length >= 4 && (!closed || visited < lines.length);
  return { triLines: lines, chainClosed: closed, hourglass };
}

export function dimLabelsByTri(prims: CpPrim[]): Map<number, number> {
  const m = new Map<number, number>();
  for (const p of prims) {
    if (p.type !== 'text' || p.layer !== 'dim') continue;
    if (typeof p.tri !== 'number') continue;
    m.set(p.tri, (m.get(p.tri) ?? 0) + 1);
  }
  return m;
}
