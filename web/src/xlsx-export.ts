// 面積計算書 XLSX 書き出し (web 版)。
// 正 = アプリの XlsxWriter (app/src/main/java/com/jpaver/trianglelist/datamanager/XlsxWriter.kt)。
// レイアウト・数式・スタイルを行単位で鏡写しにする。POI は row/col とも 0-based、
// ExcelJS は 1-based なので「Excel 表示上の行番号 = POI index + 1」で全て変換してある。
// アプリの数式が参照する行番号 (rn = POI index + 1) はもともと Excel 表示行なので、
// ExcelJS では「セルを置いた行番号そのもの」を数式に書けばよい。
//
// データ源は serializeState() の CSV テキストのみ (wasm 非依存・main.ts から独立)。
// ExcelJS 採用根拠: SheetJS CE は書き込み時スタイル strip (Pro 限定) のため、
// 赤フォント・罫線・列幅・数式が仕様の本件には ExcelJS (MIT) が適合。

import * as ExcelJSImport from 'exceljs';

// vitest (node ESM) では CJS interop で default 側に実体が入ることがあるため両対応
const ExcelJS: typeof ExcelJSImport =
  (ExcelJSImport as { default?: typeof ExcelJSImport }).default ?? ExcelJSImport;

// ---- CSV パース (main.ts の parseCsvToState / common の WebDrawingExport.parseHeader と同じ判定) ----

interface TriRow {
  num: number; // 連番 (1-based)。serializeState は番号を i+1 に書き直すのでこれが正
  a: number;
  b: number;
  c: number;
}

interface TrapRow {
  num: number; // 混在通し番号 (三角形+台形の通し)
  widthA: number; // 底辺
  length: number; // 延長 (高さ)
  widthB: number; // 上辺
}

interface DedRow {
  num: number;
  name: string;
  lenX: number;
  lenY: number;
  typenum: 0 | 1; // 0=Box(長方形) / 1=Circle(円) — Deduction.kt:71-78
  typestring: string;
  samecount: number; // 自分より前の同一 (name+lenX+lenY) の数 — DeductionList.searchSameDed + Deduction.verify
}

export interface ParsedCsvForXlsx {
  rosenname: string;
  triangles: TriRow[];
  trapezoids: TrapRow[];
  /** 台形を親に持つ三角形 (TriTrap)。Heron 数式で書く点は triangles と同じだが、
   *  DXF/SFC writer (SfcWriter:62 / DxfFileWriter) は 三角形→台形→TriTrap の順で
   *  並べるので、XLSX もそれに揃える (帳票の番号順 = 描画の番号順) */
  triTraps: TriRow[];
  deductions: DedRow[];
}

/** app CsvLoader 完全形式のヘッダラベル (WebDrawingExport.kt:33 と同一・同順) */
const HEADER_LABELS = ['koujiname', 'rosenname', 'gyousyaname', 'zumennum'];

/** 三角形行でもヘッダでもない app 由来のマーカー行 (MainActivity.kt:2781-2796 が書く)。
 *  Trapezoid / TriTrap は混在リスト (web) で serializeState が書く図形行マーカー。
 *  ここに入れておかないと「ヘッダ収集ループ」 (4 個に達するまで line.trim を拾う) に
 *  Trapezoid / TriTrap 行が混ざって路線名等が化ける */
const MARKER_TOKENS = ['Deduction', 'ListAngle', 'ListScale', 'TextSize', 'Trapezoid', 'TriTrap'];

// Kotlin の toIntOrNull と同じ判定 (main.ts:417 と同じ regex)
function intOrNull(s: string): number | null {
  return /^[+-]?\d+$/.test(s) ? parseInt(s, 10) : null;
}

export function parseCsvForXlsx(csvText: string): ParsedCsvForXlsx {
  const triangles: TriRow[] = [];
  const trapezoids: TrapRow[] = [];
  const triTraps: TriRow[] = [];
  const deductions: DedRow[] = [];
  const headerValues: string[] = [];

  for (const line of csvText.split(/\r?\n/)) {
    if (line.trim() === '') continue;
    const chunks = line.split(',').map((s) => s.trim());

    // 台形行: Trapezoid, num, length, widthA, widthB, parent, side, align, parentKind
    // (CsvCodec.serialize / main.ts serializeState と同じ列順)
    if (chunks[0] === 'Trapezoid') {
      trapezoids.push({
        num: triangles.length + trapezoids.length + 1, // 三角形+台形の通し
        widthA: parseFloat(chunks[3] ?? ''),
        length: parseFloat(chunks[2] ?? ''),
        widthB: parseFloat(chunks[4] ?? ''),
      });
      continue;
    }

    // 台形を親に持つ三角形 (TriTrap, num, a, b, c, parent, side) は Heron 面積 (三角形と同形)
    // 通し番号は 三角形+台形+TriTrap の合算 (WebDrawingExport.numberTrapTris と同形)
    if (chunks[0] === 'TriTrap') {
      triTraps.push({
        num: triangles.length + trapezoids.length + triTraps.length + 1,
        a: parseFloat(chunks[2] ?? ''),
        b: parseFloat(chunks[3] ?? ''),
        c: parseFloat(chunks[4] ?? ''),
      });
      continue;
    }

    // 控除行: Deduction,num,name,lenX,lenY,parent,type,angle,px,py,fx,fy,shapeAngle (MainActivity.kt:2795)
    if (chunks[0] === 'Deduction') {
      const name = chunks[2] ?? '';
      const lenX = parseFloat(chunks[3] ?? '');
      const lenY = parseFloat(chunks[4] ?? '');
      const type = chunks[6] ?? '';
      // samecount = 既存リスト中の同一物の数 (DeductionList.kt:101-103 searchSameDed → Deduction.verify:
      // name と lengthX と lengthY が全て一致)。2 個目が (2)、3 個目が (3) になる
      const samecount = deductions.filter(
        (d) => d.name === name && d.lenX === lenX && d.lenY === lenY,
      ).length;
      const numParsed = intOrNull(chunks[1] ?? '');
      deductions.push({
        num: numParsed ?? deductions.length + 1,
        name,
        lenX,
        lenY,
        typenum: type === 'Box' ? 0 : 1, // Deduction.kt:71-78 ("Box" 以外は全て円)
        typestring: type === 'Box' ? '長方形' : '円',
        samecount,
      });
      continue;
    }

    // 三角形行: 4 カラム以上 + 先頭が非負整数 (main.ts parseCsvToState と同じ判定)
    const num = chunks.length >= 4 ? intOrNull(chunks[0] ?? '') : null;
    if (num !== null && num >= 0) {
      // 新 schema: parent が「現在までの三角形数」を超えていたら台形を指す混在通し番号 = 台形子三角形
      // (CsvCodec.parse と同じ判定。TriTrap タグ廃止後の主経路)
      const parent = intOrNull(chunks[4] ?? '') ?? -1;
      if (parent > triangles.length) {
        triTraps.push({
          num: triangles.length + trapezoids.length + triTraps.length + 1,
          a: parseFloat(chunks[1] ?? ''),
          b: parseFloat(chunks[2] ?? ''),
          c: parseFloat(chunks[3] ?? ''),
        });
        continue;
      }
      triangles.push({
        num: triangles.length + trapezoids.length + 1, // 混在通し (台形が先に在っても番号がずれない)
        a: parseFloat(chunks[1] ?? ''),
        b: parseFloat(chunks[2] ?? ''),
        c: parseFloat(chunks[3] ?? ''),
      });
      continue;
    }

    // ヘッダ行: 最初の 4 つを koujiname/rosenname/gyousyaname/zumennum として読む
    // (WebDrawingExport.parseHeader と同形。マーカー行だけは XLSX では明示スキップ —
    //  最小ヘッダ CSV で Deduction/ListScale 行が路線名に化けるのを防ぐ)
    if (MARKER_TOKENS.includes(chunks[0] ?? '')) continue;
    if (headerValues.length < 4) {
      headerValues.push(
        HEADER_LABELS.includes(chunks[0] ?? '') ? (chunks[1] ?? '') : line.trim(),
      );
    }
  }

  return { rosenname: headerValues[1] ?? '', triangles, trapezoids, triTraps, deductions };
}

// ---- ワークブック組み立て (XlsxWriter.kt の鏡写し) ----

// XlsxWriter.kt:19 rowsizer=1.25 × POI XSSF の既定行高 15pt
const ROW_HEIGHT_POINTS = 15 * 1.25; // 18.75
// XlsxWriter.kt:54 列 0-5 = [2,12,8,8,8,12] 文字
const COLUMN_WIDTHS = [2, 12, 8, 8, 8, 12];
const RED = { argb: 'FFFF0000' } as const; // IndexedColors.RED (XlsxWriter.kt:44)

type CellStyleKind = 'C' | 'Title' | 'Digit' | 'CRed' | 'DigitRed';

// XlsxWriter init (34-51): 全スタイル中央寄せ、Title 以外は下罫線 THIN、
// Digit 系は表示書式 "0.00"、Red 系は赤フォント
function applyStyle(cell: ExcelJSImport.Cell, kind: CellStyleKind): void {
  cell.alignment = { horizontal: 'center' };
  if (kind !== 'Title') {
    cell.border = { bottom: { style: 'thin' } };
  }
  if (kind === 'Digit' || kind === 'DigitRed') {
    cell.numFmt = '0.00';
  }
  if (kind === 'CRed' || kind === 'DigitRed') {
    cell.font = { color: RED };
  }
}

// Kotlin: Math.round(len * 100) * 0.01 (XlsxWriter.kt:103-105) — 値そのものを 2dp に丸めて書く
function round2(v: number): number {
  return Math.round(v * 100) * 0.01;
}

function setRowHeight(sheet: ExcelJSImport.Worksheet, row: number): void {
  sheet.getRow(row).height = ROW_HEIGHT_POINTS;
}

// writeRow(rownum, string, style) 相当: D 列 (POI cell index 3) に 1 セルだけ書く
function writeSingle(
  sheet: ExcelJSImport.Worksheet,
  row: number,
  text: string,
  kind: CellStyleKind,
  hyperlink?: string,
): void {
  setRowHeight(sheet, row);
  const cell = sheet.getCell(row, 4);
  cell.value = hyperlink ? { text, hyperlink } : text;
  applyStyle(cell, kind);
}

// writeRow(rownum, style, vararg) 相当: B 列 (POI cell index 1) から並べる
function writeStrings(
  sheet: ExcelJSImport.Worksheet,
  row: number,
  kind: CellStyleKind,
  ...strings: string[]
): void {
  setRowHeight(sheet, row);
  strings.forEach((s, i) => {
    const cell = sheet.getCell(row, 2 + i);
    cell.value = s;
    applyStyle(cell, kind);
  });
}

// writeGoukei (XlsxWriter.kt:163-182) 相当。rowExcel は POI の rownum + 1。
// formulaid 0 = sum(F開始:F終了) / 1 = F{前の小計}+F{次の小計}
function writeGoukei(
  sheet: ExcelJSImport.Worksheet,
  rowExcelBase: number,
  listSize: number,
  label: string,
  formulaid: 0 | 1,
  kind: CellStyleKind,
): void {
  // POI: sumstart = rownum+1, sumend = rownum+size。rowExcelBase = rownum+1 なので
  // Excel 行で書くと sumstart = rowExcelBase, sumend = rowExcelBase + size - 1、
  // 合計行の位置は POI sumend → Excel sumend + 1
  const sumstart = rowExcelBase;
  const sumend = rowExcelBase + listSize - 1;
  const beforesyoukei = sumstart - 4; // XlsxWriter.kt:166 (POI 値 = Excel 値、式に入る数字は同一)
  const formula =
    formulaid === 0 ? `sum(F${sumstart}:F${sumend})` : `F${beforesyoukei}+F${sumend}`;

  const rowSum = sumend + 1;
  setRowHeight(sheet, rowSum);
  const labels = [label, '', '', ''];
  labels.forEach((s, i) => {
    const cell = sheet.getCell(rowSum, 2 + i);
    cell.value = s;
    applyStyle(cell, kind);
  });
  const fCell = sheet.getCell(rowSum, 6);
  fCell.value = { formula };
  applyStyle(fCell, kind);
}

/** CSV → ExcelJS Workbook。レイアウトは XlsxWriter.write (XlsxWriter.kt:207-239) を鏡写し */
export function buildXlsxWorkbook(csvText: string): ExcelJSImport.Workbook {
  const { rosenname, triangles, trapezoids, triTraps, deductions } = parseCsvForXlsx(csvText);
  const wb = new ExcelJS.Workbook();
  const sheet = wb.addWorksheet('Sheet0'); // POI createSheet() の既定名

  COLUMN_WIDTHS.forEach((w, i) => {
    sheet.getColumn(i + 1).width = w;
  });

  const n = triangles.length;
  const k = trapezoids.length;
  const t = triTraps.length;
  const m = deductions.length;
  // 図形合計 (三角形+台形+TriTrap)。小計(1)・控除セクションの開始位置・末尾クレジット行はここを基準
  const figCount = n + k + t;

  // タイトル・路線名・ヘッダ (XlsxWriter.kt:209-211 — POI row 1,2,3 → Excel row 2,3,4)
  writeSingle(sheet, 2, '面 積 計 算 書', 'Title');
  writeSingle(sheet, 3, rosenname, 'Title');
  writeStrings(sheet, 4, 'C', '番号', '辺長A', '辺長B', '辺長C', '面積');

  // 本体 (writeBody TriangleList 版, XlsxWriter.kt:97-126 — POI row 4+i → Excel row 5+i)
  triangles.forEach((t, i) => {
    const r = 5 + i; // = アプリの rn (数式が参照する Excel 行番号)
    setRowHeight(sheet, r);
    const lsum = `(0.5*(C${r}+D${r}+E${r}))`;
    const cells = [2, 3, 4, 5, 6].map((c) => sheet.getCell(r, c));
    cells[0].value = t.num;
    cells[1].value = round2(t.a);
    cells[2].value = round2(t.b);
    cells[3].value = round2(t.c);
    cells[4].value = {
      formula: `ROUND(((${lsum}*(${lsum}-C${r})*(${lsum}-D${r})*(${lsum}-E${r}))^0.5),2)`,
    };
    applyStyle(cells[0], 'C');
    applyStyle(cells[1], 'Digit');
    applyStyle(cells[2], 'Digit');
    applyStyle(cells[3], 'Digit');
    applyStyle(cells[4], 'Digit');
  });

  // 台形 (混在リスト)。三角形の後に続けて出す。面積 = (底辺+上辺)*高さ/2 (台形面積公式)。
  // C 列 = widthA(底辺), D 列 = length(延長/高さ), E 列 = widthB(上辺) — 三角形と同じ列順を流用
  // (帳票で「辺長A/B/C」ラベルのまま値を入れる。台形のラベル列は別途要望が来たら独立化)
  trapezoids.forEach((trap, i) => {
    const r = 5 + n + i;
    setRowHeight(sheet, r);
    const cells = [2, 3, 4, 5, 6].map((c) => sheet.getCell(r, c));
    cells[0].value = trap.num;
    cells[1].value = round2(trap.widthA);
    cells[2].value = round2(trap.length);
    cells[3].value = round2(trap.widthB);
    cells[4].value = { formula: `ROUND((C${r}+E${r})*D${r}/2,2)` };
    applyStyle(cells[0], 'C');
    applyStyle(cells[1], 'Digit');
    applyStyle(cells[2], 'Digit');
    applyStyle(cells[3], 'Digit');
    applyStyle(cells[4], 'Digit');
  });

  // 台形を親に持つ三角形 (TriTrap)。台形の後に Heron 数式で出す。
  // DXF/SFC writer (SfcWriter:62 等) と同順 — 帳票の番号順 = 描画の番号順を保つ
  triTraps.forEach((tt, i) => {
    const r = 5 + n + k + i;
    setRowHeight(sheet, r);
    const lsum = `(0.5*(C${r}+D${r}+E${r}))`;
    const cells = [2, 3, 4, 5, 6].map((c) => sheet.getCell(r, c));
    cells[0].value = tt.num;
    cells[1].value = round2(tt.a);
    cells[2].value = round2(tt.b);
    cells[3].value = round2(tt.c);
    cells[4].value = {
      formula: `ROUND(((${lsum}*(${lsum}-C${r})*(${lsum}-D${r})*(${lsum}-E${r}))^0.5),2)`,
    };
    applyStyle(cells[0], 'C');
    applyStyle(cells[1], 'Digit');
    applyStyle(cells[2], 'Digit');
    applyStyle(cells[3], 'Digit');
    applyStyle(cells[4], 'Digit');
  });

  // 小計(1) (XlsxWriter.kt:214 writeGoukei(4, trilist, ...): POI rownum 4 → Excel base 5)
  // 図形合計ぶんを集計 (三角形 n + 台形 k + TriTrap t)
  writeGoukei(sheet, 5, figCount, '小計(1)', 0, 'C');

  // 控除セクション (XlsxWriter.kt:216 — dedlist がある時だけ。開始 POI 行 = 4 + figCount + 1)
  if (m > 0) {
    const rowStartD = 4 + figCount + 1 + 1; // Excel 行 (POI + 1) = 6 + figCount
    writeSingle(sheet, rowStartD, '面積控除', 'CRed');
    writeStrings(sheet, rowStartD + 1, 'CRed', '名称', '寸法１', '寸法２', '形状', '面積');

    // writeBody DeductionList 版 (XlsxWriter.kt:128-161)
    deductions.forEach((d, i) => {
      const r = rowStartD + 2 + i; // = アプリの rowcounter
      setRowHeight(sheet, r);
      const formulas = [`-C${r}*D${r}`, `-(C${r}*0.5)*(C${r}*0.5)*3.14`];
      // get_number_name_samecount (Deduction.kt:166-171): "num.name"、2 個目以降は "(n)"
      const nameCell = d.samecount > 0 ? `${d.num}.${d.name}(${d.samecount + 1})` : `${d.num}.${d.name}`;
      const cells = [2, 3, 4, 5, 6].map((c) => sheet.getCell(r, c));
      cells[0].value = nameCell;
      cells[1].value = round2(d.lenX);
      cells[2].value = round2(d.lenY);
      cells[3].value = d.typestring;
      cells[4].value = { formula: `ROUND(${formulas[d.typenum]},2)` };
      applyStyle(cells[0], 'CRed');
      applyStyle(cells[1], 'DigitRed');
      applyStyle(cells[2], 'DigitRed');
      applyStyle(cells[3], 'CRed');
      applyStyle(cells[4], 'DigitRed');
    });

    // 小計(2) / 合計(1)+(2) (XlsxWriter.kt:202-203 — POI rowStartD+2 / +3 → Excel base +3 / +4)
    writeGoukei(sheet, rowStartD + 2, m, '小計(2)', 0, 'CRed');
    writeGoukei(sheet, rowStartD + 3, m, '合計(1)+(2)', 1, 'C');
  }

  // 末尾クレジット (XlsxWriter.kt:219-224 — POI endrow = 4+figCount+1+m+4、m=0 でも同式)。
  // アプリは Apache POI への謝辞を書く。web は使用ライブラリ ExcelJS に合わせる (出典明記は敬意)
  const creditRow = 4 + figCount + 1 + m + 4 + 1 + 1; // POI endrow+1 → Excel
  writeSingle(sheet, creditRow, '( This .xlsx file was exported by ExcelJS )', 'Title');
  writeSingle(
    sheet,
    creditRow + 1,
    '( https://github.com/exceljs/exceljs )',
    'Title',
    'https://github.com/exceljs/exceljs',
  );

  return wb;
}

/** CSV テキスト → ダウンロード用 Blob (保存ボタン配線から呼ぶ唯一の入口) */
export async function buildXlsxBlob(csvText: string): Promise<Blob> {
  const wb = buildXlsxWorkbook(csvText);
  const buf = await wb.xlsx.writeBuffer();
  return new Blob([buf], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
}
