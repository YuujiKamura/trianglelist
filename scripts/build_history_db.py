"""trianglelist git履歴をSQLiteに落とす。"""
from __future__ import annotations

import os
import sqlite3
import sys
from pathlib import Path

VAULT = Path(os.path.expanduser("~/user-context-vault/repos/trianglelist"))
DB = VAULT / "history.sqlite"
COMMITS_DUMP = VAULT / "_dump" / "tl_commits.txt"
NUMSTAT_DUMP = VAULT / "_dump" / "tl_numstat.txt"
BRANCHES_DUMP = VAULT / "_dump" / "tl_branches.tsv"


def parse_commits(text: str):
    blocks = text.split("__COMMIT__\n")
    for b in blocks:
        if not b.strip():
            continue
        lines = b.split("\n")
        if len(lines) < 9:
            continue
        h, sh, adate, cdate, an, ae, parents, subj = lines[:8]
        rest = "\n".join(lines[8:])
        body = ""
        if "__BODY__" in rest and "__ENDBODY__" in rest:
            body = rest.split("__BODY__\n", 1)[1].rsplit("\n__ENDBODY__", 1)[0]
        yield (h, sh, adate, cdate, an, ae, parents, subj, body)


def parse_numstat(text: str):
    cur = None
    for line in text.split("\n"):
        if not line:
            continue
        if line.startswith("__COMMIT__"):
            cur = line[len("__COMMIT__"):]
            continue
        if cur is None:
            continue
        parts = line.split("\t")
        if len(parts) != 3:
            continue
        ins, dele, path = parts
        ins_n = 0 if ins == "-" else int(ins)
        del_n = 0 if dele == "-" else int(dele)
        yield (cur, path, "M", ins_n, del_n)


INSIGHTS_RULE = (
    "全エントリに evidence (出典/一次資料/観測) 必須 + "
    "code_refs (file:line / commit hash) か related_md のどちらか必須。"
    "trigger insights_require_source が物理 gate。2026-06-10 user 指示"
)

SCHEMA = """
CREATE TABLE IF NOT EXISTS commits (
  hash TEXT PRIMARY KEY,
  short_hash TEXT,
  author_date TEXT,
  commit_date TEXT,
  author_name TEXT,
  author_email TEXT,
  subject TEXT,
  body TEXT,
  parent_hashes TEXT,
  files_changed INTEGER DEFAULT 0,
  insertions INTEGER DEFAULT 0,
  deletions INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_commits_date ON commits(author_date);
CREATE INDEX IF NOT EXISTS idx_commits_author ON commits(author_name);
CREATE TABLE IF NOT EXISTS file_changes (
  commit_hash TEXT,
  file_path TEXT,
  status TEXT,
  insertions INTEGER,
  deletions INTEGER
);
CREATE INDEX IF NOT EXISTS idx_file_changes_commit ON file_changes(commit_hash);
CREATE INDEX IF NOT EXISTS idx_file_changes_path ON file_changes(file_path);
CREATE TABLE IF NOT EXISTS branches (
  name TEXT PRIMARY KEY,
  head_hash TEXT,
  is_remote INTEGER,
  last_commit_date TEXT
);
CREATE TABLE IF NOT EXISTS meta (
  key TEXT PRIMARY KEY,
  value TEXT
);
CREATE TABLE IF NOT EXISTS insights (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recorded_at TEXT NOT NULL,
    session_topic TEXT,
    finding TEXT NOT NULL,
    evidence TEXT,
    code_refs TEXT,
    related_md TEXT
);
CREATE INDEX IF NOT EXISTS idx_insights_recorded ON insights(recorded_at);
CREATE INDEX IF NOT EXISTS idx_insights_topic ON insights(session_topic);
CREATE TRIGGER IF NOT EXISTS insights_require_source
BEFORE INSERT ON insights
FOR EACH ROW
WHEN (NEW.evidence IS NULL OR trim(NEW.evidence)='')
  OR ((NEW.code_refs IS NULL OR trim(NEW.code_refs)='')
      AND (NEW.related_md IS NULL OR trim(NEW.related_md)=''))
BEGIN
  SELECT RAISE(ABORT, 'source required: evidence + (code_refs or related_md)');
END;
"""


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    cur.executescript(SCHEMA)

    commits_text = COMMITS_DUMP.read_text(encoding="utf-8", errors="replace")
    rows = list(parse_commits(commits_text))
    cur.executemany(
        "INSERT OR IGNORE INTO commits "
        "(hash, short_hash, author_date, commit_date, author_name, author_email, parent_hashes, subject, body) "
        "VALUES (?,?,?,?,?,?,?,?,?)",
        rows,
    )
    print(f"commits: {len(rows)} inserted")

    numstat_text = NUMSTAT_DUMP.read_text(encoding="utf-8", errors="replace")
    fc_rows = list(parse_numstat(numstat_text))
    cur.executemany(
        "INSERT INTO file_changes (commit_hash, file_path, status, insertions, deletions) "
        "VALUES (?,?,?,?,?)",
        fc_rows,
    )
    print(f"file_changes: {len(fc_rows)} inserted")

    cur.execute(
        "UPDATE commits SET "
        " files_changed = (SELECT COUNT(*) FROM file_changes WHERE commit_hash = commits.hash), "
        " insertions = (SELECT COALESCE(SUM(insertions),0) FROM file_changes WHERE commit_hash = commits.hash), "
        " deletions  = (SELECT COALESCE(SUM(deletions),0)  FROM file_changes WHERE commit_hash = commits.hash) "
    )

    branches_text = BRANCHES_DUMP.read_text(encoding="utf-8", errors="replace")
    br_rows = []
    for line in branches_text.strip().split("\n"):
        if not line:
            continue
        parts = line.split("|")
        if len(parts) < 3:
            continue
        name, h, date = parts[0], parts[1], parts[2]
        br_rows.append((name, h, 1 if name.startswith("origin/") else 0, date))
    cur.executemany(
        "INSERT OR REPLACE INTO branches (name, head_hash, is_remote, last_commit_date) VALUES (?,?,?,?)",
        br_rows,
    )
    print(f"branches: {len(br_rows)} inserted")

    cur.execute("DELETE FROM meta")
    cur.executemany(
        "INSERT INTO meta (key, value) VALUES (?,?)",
        [
            ("source_repo", "C:/Users/yuuji/StudioProjects/trianglelist"),
            ("first_commit", str(min(r[2] for r in rows))),
            ("last_commit", str(max(r[2] for r in rows))),
            ("total_commits", str(len(rows))),
            ("insights_rule", INSIGHTS_RULE),
        ],
    )

    conn.commit()
    conn.close()
    print(f"done -> {DB}")


if __name__ == "__main__":
    main()
