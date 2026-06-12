// vite.config.ts の define で焼き込むビルド識別子の型宣言。
// __BUILD_COMMIT__ = git の短縮コミット ID、__BUILD_TIME__ = 生成時刻 (JST)。
declare const __BUILD_COMMIT__: string;
declare const __BUILD_TIME__: string;
