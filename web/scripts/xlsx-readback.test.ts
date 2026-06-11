// XLSX read-back 検証: buildXlsxBlob の生成物を ExcelJS で読み戻し、
// アプリの XlsxWriter (XlsxWriter.kt) と同じレイアウト・数式・スタイルかを assert する。
// 実行: cd web && npx vitest run scripts/xlsx-readback.test.ts
import { describe, it, expect, beforeAll } from 'vitest';
import * as ExcelJSImport from 'exceljs';
import { buildXlsxBlob, parseCsvForXlsx } from '../src/xlsx-export';

const ExcelJS: typeof ExcelJSImport =
  (ExcelJSImport as { default?: typeof ExcelJSImport }).default ?? ExcelJSImport;

// 三角形 3 行 + Deduction 2 行 (同名・同寸法 1 組 → 2 個目が "(2)")
const SAMPLE_CSV = [
  'koujiname,工事A',
  'rosenname,テスト路線',
  'gyousyaname,業者C',
  'zumennum,Z-1',
  '1,6.0,5.0,4.0,-1,-1',
  '2,5.0,4.0,3.0,1,1',
  '3,4.0,3.5,3.0,1,2',
  'ListAngle, 0',
  'ListScale, 1.0',
  'Deduction,1,集水桝,0.5,0.5,1,Box,0.0,1.0,1.0,1.5,1.5,0.0',
  'Deduction,2,集水桝,0.5,0.5,2,Circle,0.0,2.0,2.0,2.5,2.5,0.0',
  '',
].join('\n');

async function readBack(csv: string): Promise<ExcelJSImport.Worksheet> {
  const blob = await buildXlsxBlob(csv);
  const wb = new ExcelJS.Workbook();
  await wb.xlsx.load(await blob.arrayBuffer());
  const ws = wb.getWorksheet(1);
  if (!ws) throw new Error('worksheet not found');
  return ws;
}

describe('parseCsvForXlsx', () => {
  const parsed = parseCsvForXlsx(SAMPLE_CSV);

  it('路線名はラベル付きヘッダの 2 カラム目', () => {
    expect(parsed.rosenname).toBe('テスト路線');
  });

  it('三角形 3 行が連番で読める', () => {
    expect(parsed.triangles).toEqual([
      { num: 1, a: 6, b: 5, c: 4 },
      { num: 2, a: 5, b: 4, c: 3 },
      { num: 3, a: 4, b: 3.5, c: 3 },
    ]);
  });

  it('控除 2 行: 同名同寸法の 2 個目に samecount が付く (verify = name+lenX+lenY)', () => {
    expect(parsed.deductions).toHaveLength(2);
    expect(parsed.deductions[0]).toMatchObject({
      num: 1, name: '集水桝', typenum: 0, typestring: '長方形', samecount: 0,
    });
    expect(parsed.deductions[1]).toMatchObject({
      num: 2, name: '集水桝', typenum: 1, typestring: '円', samecount: 1,
    });
  });
});

describe('XLSX read-back (XlsxWriter.kt 鏡写し / n=3, m=2)', () => {
  let ws: ExcelJSImport.Worksheet;
  beforeAll(async () => {
    ws = await readBack(SAMPLE_CSV);
  });

  it('タイトル: D2 中央寄せ・枠なし (XlsxWriter.kt:209)', () => {
    const c = ws.getCell('D2');
    expect(c.value).toBe('面 積 計 算 書');
    expect(c.alignment?.horizontal).toBe('center');
    expect(c.border?.bottom?.style ?? undefined).toBeUndefined();
  });

  it('路線名: D3 (XlsxWriter.kt:210)', () => {
    expect(ws.getCell('D3').value).toBe('テスト路線');
  });

  it('ヘッダ行 4: B-F 中央寄せ・下罫線 (XlsxWriter.kt:211)', () => {
    expect(ws.getCell('B4').value).toBe('番号');
    expect(ws.getCell('C4').value).toBe('辺長A');
    expect(ws.getCell('D4').value).toBe('辺長B');
    expect(ws.getCell('E4').value).toBe('辺長C');
    expect(ws.getCell('F4').value).toBe('面積');
    expect(ws.getCell('B4').alignment?.horizontal).toBe('center');
    expect(ws.getCell('B4').border?.bottom?.style).toBe('thin');
  });

  it('本体行 5-7: 番号 numeric + 辺長 2dp 丸め値 + 書式 0.00', () => {
    expect(ws.getCell('B5').value).toBe(1);
    expect(ws.getCell('C5').value).toBe(6);
    expect(ws.getCell('D5').value).toBe(5);
    expect(ws.getCell('E5').value).toBe(4);
    expect(ws.getCell('C5').numFmt).toBe('0.00');
    expect(ws.getCell('B7').value).toBe(3);
    expect(ws.getCell('D7').value).toBe(3.5);
  });

  it('面積セル F5-F7: Heron を Excel 数式で (XlsxWriter.kt:102-117 と同一文字列)', () => {
    const heron = (r: number) => {
      const s = `(0.5*(C${r}+D${r}+E${r}))`;
      return `ROUND(((${s}*(${s}-C${r})*(${s}-D${r})*(${s}-E${r}))^0.5),2)`;
    };
    expect(ws.getCell('F5').formula).toBe(heron(5));
    expect(ws.getCell('F6').formula).toBe(heron(6));
    expect(ws.getCell('F7').formula).toBe(heron(7));
  });

  it('小計(1) 行 8: sum 式 (XlsxWriter.kt:163-182)', () => {
    expect(ws.getCell('B8').value).toBe('小計(1)');
    expect(ws.getCell('F8').formula).toBe('sum(F5:F7)');
  });

  it('控除タイトル D9 + ヘッダ行 10: 赤フォント (XlsxWriter.kt:199-200)', () => {
    expect(ws.getCell('D9').value).toBe('面積控除');
    expect(ws.getCell('D9').font?.color?.argb).toBe('FFFF0000');
    expect(ws.getCell('B10').value).toBe('名称');
    expect(ws.getCell('C10').value).toBe('寸法１');
    expect(ws.getCell('D10').value).toBe('寸法２');
    expect(ws.getCell('E10').value).toBe('形状');
    expect(ws.getCell('F10').value).toBe('面積');
    expect(ws.getCell('B10').font?.color?.argb).toBe('FFFF0000');
  });

  it('控除本体 11-12: 同名 2 個目に "(2)"、Box/円 の数式、全て赤 (XlsxWriter.kt:128-161)', () => {
    expect(ws.getCell('B11').value).toBe('1.集水桝');
    expect(ws.getCell('C11').value).toBe(0.5);
    expect(ws.getCell('D11').value).toBe(0.5);
    expect(ws.getCell('E11').value).toBe('長方形');
    expect(ws.getCell('F11').formula).toBe('ROUND(-C11*D11,2)');
    expect(ws.getCell('B12').value).toBe('2.集水桝(2)');
    expect(ws.getCell('E12').value).toBe('円');
    expect(ws.getCell('F12').formula).toBe('ROUND(-(C12*0.5)*(C12*0.5)*3.14,2)');
    expect(ws.getCell('F11').font?.color?.argb).toBe('FFFF0000');
    expect(ws.getCell('C11').numFmt).toBe('0.00');
  });

  it('小計(2) 行 13 (赤) と 合計(1)+(2) 行 14 (黒) (XlsxWriter.kt:202-203)', () => {
    expect(ws.getCell('B13').value).toBe('小計(2)');
    expect(ws.getCell('F13').formula).toBe('sum(F11:F12)');
    expect(ws.getCell('F13').font?.color?.argb).toBe('FFFF0000');
    expect(ws.getCell('B14').value).toBe('合計(1)+(2)');
    expect(ws.getCell('F14').formula).toBe('F8+F13');
    expect(ws.getCell('F14').font?.color?.argb ?? undefined).toBeUndefined();
  });

  it('列幅 A-F = [2,12,8,8,8,12] (XlsxWriter.kt:54)', () => {
    const widths = [1, 2, 3, 4, 5, 6].map((i) => ws.getColumn(i).width);
    expect(widths).toEqual([2, 12, 8, 8, 8, 12]);
  });

  it('行高 = 標準 15pt × 1.25 = 18.75 (XlsxWriter.kt:19,72)', () => {
    expect(ws.getRow(2).height).toBeCloseTo(18.75, 5);
    expect(ws.getRow(5).height).toBeCloseTo(18.75, 5);
    expect(ws.getRow(14).height).toBeCloseTo(18.75, 5);
  });

  it('末尾クレジット: ExcelJS への謝辞 + ハイパーリンク (XlsxWriter.kt:219-224 の web 版)', () => {
    // POI endrow = 4+n+1+m+4 = 14 → クレジットは Excel 行 16,17
    expect(ws.getCell('D16').value).toBe('( This .xlsx file was exported by ExcelJS )');
    const link = ws.getCell('D17').value as { text?: string; hyperlink?: string };
    expect(link.text).toBe('( https://github.com/exceljs/exceljs )');
    expect(link.hyperlink).toBe('https://github.com/exceljs/exceljs');
  });
});

describe('生成物の XML レベル検証 (unzip → xl/worksheets/sheet1.xml)', () => {
  it('sheet1.xml に Heron / 控除 / 小計 / 合計の数式が <f> として居る', async () => {
    // exceljs が依存している jszip でそのまま unzip する
    const { default: JSZip } = await import('jszip');
    const blob = await buildXlsxBlob(SAMPLE_CSV);
    const zip = await JSZip.loadAsync(await blob.arrayBuffer());
    const sheetXml = await zip.file('xl/worksheets/sheet1.xml')?.async('string');
    expect(sheetXml).toBeTruthy();
    const xml = sheetXml ?? '';
    expect(xml).toContain(
      '<f>ROUND((((0.5*(C5+D5+E5))*((0.5*(C5+D5+E5))-C5)*((0.5*(C5+D5+E5))-D5)*((0.5*(C5+D5+E5))-E5))^0.5),2)</f>',
    );
    expect(xml).toContain('<f>ROUND(-C11*D11,2)</f>');
    expect(xml).toContain('<f>ROUND(-(C12*0.5)*(C12*0.5)*3.14,2)</f>');
    expect(xml).toContain('<f>sum(F5:F7)</f>');
    expect(xml).toContain('<f>sum(F11:F12)</f>');
    expect(xml).toContain('<f>F8+F13</f>');
  });
});

describe('XLSX read-back (控除なし / n=2, m=0)', () => {
  const CSV_NO_DED = [
    'koujiname,工事A',
    'rosenname,路線X',
    '1,3.0,4.0,5.0,-1,-1',
    '2,4.0,4.0,4.0,1,1',
    'ListAngle, 0',
    '',
  ].join('\n');

  it('控除セクションが出ず、小計(1) とクレジットだけが正位置に出る', async () => {
    const ws = await readBack(CSV_NO_DED);
    expect(ws.getCell('D3').value).toBe('路線X');
    expect(ws.getCell('B7').value).toBe('小計(1)');
    expect(ws.getCell('F7').formula).toBe('sum(F5:F6)');
    expect(ws.getCell('D8').value ?? null).toBeNull(); // 面積控除は無い
    // POI endrow = 4+2+1+0+4 = 11 → クレジットは Excel 行 13,14
    expect(ws.getCell('D13').value).toBe('( This .xlsx file was exported by ExcelJS )');
  });
});
