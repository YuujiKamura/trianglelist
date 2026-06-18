import { describe, it, expect } from 'vitest';
import { safeOpenUrl } from '../src/url-safety';

// 2026-06-18 security review (automated) で指摘された XSS 経路を pin する unit test。
// 図面枠の url click → window.open に渡す前に scheme を http/https に限定する gate の
// 動作確認。 現状 url 値は wasm 内固定だが、 将来 user 編集可能になる場面で gate が
// 動かないと javascript:/data:/vbscript:/file: で 任意 script 実行可能になる。

describe('safeOpenUrl', () => {
  it('http URL を sanitized で返す', () => {
    expect(safeOpenUrl('http://trianglelist.home.blog')).toBe('http://trianglelist.home.blog/');
  });

  it('https URL を sanitized で返す', () => {
    expect(safeOpenUrl('https://example.com/path?q=1')).toBe('https://example.com/path?q=1');
  });

  it('javascript: scheme を弾く', () => {
    expect(safeOpenUrl('javascript:alert(1)')).toBeNull();
  });

  it('data: scheme を弾く', () => {
    expect(safeOpenUrl('data:text/html,<script>alert(1)</script>')).toBeNull();
  });

  it('vbscript: scheme を弾く', () => {
    expect(safeOpenUrl('vbscript:msgbox("xss")')).toBeNull();
  });

  it('file: scheme を弾く', () => {
    expect(safeOpenUrl('file:///etc/passwd')).toBeNull();
  });

  it('ftp scheme も弾く (http/https 以外は全て)', () => {
    expect(safeOpenUrl('ftp://example.com/file')).toBeNull();
  });

  it('不正 URL (パースエラー) を弾く', () => {
    expect(safeOpenUrl('not a url')).toBeNull();
  });

  it('空文字を弾く', () => {
    expect(safeOpenUrl('')).toBeNull();
  });

  it('null/undefined を弾く', () => {
    expect(safeOpenUrl(null)).toBeNull();
    expect(safeOpenUrl(undefined)).toBeNull();
  });

  it('大文字 scheme (HTTPS:) も通す (= URL ctor が正規化)', () => {
    expect(safeOpenUrl('HTTPS://Example.COM')).toBe('https://example.com/');
  });
});
