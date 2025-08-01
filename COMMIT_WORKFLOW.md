# コミット前チェックワークフロー

## 自動チェック (Pre-commit Hook)
Git hookが設定済みなので、`git commit`時に自動的にlintチェックが実行されます。

## 手動チェック
コミット前に手動でチェックする場合：

```bash
# 方法1: カスタムタスク (推奨)
./gradlew preCommitChecks

# 方法2: 個別実行
./gradlew lintDevDebug
./gradlew testDevDebugUnitTest

# 方法3: pre-commit（別途インストール必要）
pre-commit run --all-files
```

## エラーが出た場合
1. lint結果を確認: `app/build/reports/lint-results-devDebug.html`
2. 問題を修正
3. 再度チェック実行
4. 問題なければコミット

## 設定の無効化
緊急時にhookを無効化する場合：
```bash
git commit --no-verify -m "緊急修正"
```