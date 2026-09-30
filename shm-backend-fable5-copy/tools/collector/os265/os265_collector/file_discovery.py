"""Candidate file discovery and incremental tail reading.

File selection rules:
    1. Exclusion takes precedence: any 物理量CH*.txt file is skipped.
    2. Inclusion: only *_通道*.txt channel record files are ingested.
"""

from __future__ import annotations

import time
from pathlib import Path
from typing import List, Tuple

from .config import FILE_LOCK_RETRY_ATTEMPTS, FILE_LOCK_RETRY_BACKOFF_SECONDS
from .line_parser import decode_bytes

_PHYSICAL_PREFIX = "\u7269\u7406\u91cfCH"
_CHANNEL_MARKER = "_\u901a\u9053"


def _read_bytes_with_retry(path: Path, start_offset: int) -> bytes:
    """Read from start_offset with brief retry/backoff on Windows locks.

    OS265 occasionally holds an exclusive handle for a moment while
    flushing; a transient PermissionError is retried instead of bubbling
    up immediately. The final attempt's PermissionError propagates so
    the caller can log it as a transient warning and retry next scan.
    """
    last_error: Exception | None = None
    for attempt in range(FILE_LOCK_RETRY_ATTEMPTS):
        try:
            with path.open("rb") as fh:
                fh.seek(start_offset)
                return fh.read()
        except PermissionError as exc:
            last_error = exc
            if attempt < FILE_LOCK_RETRY_ATTEMPTS - 1:
                time.sleep(FILE_LOCK_RETRY_BACKOFF_SECONDS)
    raise last_error  # type: ignore[misc]


def is_excluded_physical_file(path: Path) -> bool:
    return path.suffix.lower() == ".txt" and path.name.startswith(_PHYSICAL_PREFIX)


def is_channel_record_file(path: Path) -> bool:
    if not path.is_file() or path.suffix.lower() != ".txt":
        return False
    # Exclusion rule takes precedence over inclusion.
    if is_excluded_physical_file(path):
        return False
    return _CHANNEL_MARKER in path.name


def relative_source_file(path: Path, data_dir: Path) -> str:
    """Stable relative source file identifier for backend persistence."""
    try:
        return path.relative_to(data_dir).as_posix()
    except ValueError:
        return path.name


def list_candidate_files(data_dir: Path) -> List[Path]:
    if not data_dir.exists():
        return []
    candidates: List[Tuple[float, str, Path]] = []
    for path in data_dir.iterdir():
        if not is_channel_record_file(path):
            continue
        try:
            stat = path.stat()
        except PermissionError:
            continue
        candidates.append((stat.st_mtime, path.name, path))
    return [path for _, _, path in
            sorted(candidates, key=lambda item: (item[0], item[1]), reverse=True)]


def complete_lines_from_tail(path: Path, start_offset: int,
                             quiet_seconds: float) -> Tuple[List[Tuple[int, str]], int]:
    """Read complete lines from start_offset onward.

    Handles truncation/rotation (stored offset larger than the current
    file size resets to 0) and defers an incomplete trailing line until
    the file has been quiet long enough.
    """
    size = path.stat().st_size
    if start_offset > size:
        # File truncated or rotated: restart from the beginning.
        start_offset = 0
    if start_offset == size:
        # Includes freshly created zero-byte files: nothing to do yet.
        return [], start_offset

    data = _read_bytes_with_retry(path, start_offset)

    if not data:
        return [], start_offset

    lines: List[Tuple[int, str]] = []
    cursor = start_offset
    parts = data.splitlines(keepends=True)
    file_is_quiet = time.time() - path.stat().st_mtime >= quiet_seconds

    for index, raw_line in enumerate(parts):
        is_last = index == len(parts) - 1
        has_line_end = raw_line.endswith(b"\n") or raw_line.endswith(b"\r")
        if is_last and not has_line_end and not file_is_quiet:
            break

        line_offset = cursor
        cursor += len(raw_line)
        text = decode_bytes(raw_line).strip()
        if text:
            lines.append((line_offset, text))

    return lines, cursor
