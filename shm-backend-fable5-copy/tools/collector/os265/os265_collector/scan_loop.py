"""Scan pass and long-running loop for the OS265 file collector.

Hot-path behavior:
    * Every cycle rescans all *_通道*.txt candidates (newest first), so
      a freshly rotated file is picked up immediately and an older file
      that just received its final flush is still drained.
    * Growing files are processed without waiting for them to become
      stable; only the final incomplete line is deferred (handled by
      complete_lines_from_tail), so completed lines reach the backend
      within roughly one scan interval after OS265 flushes them.
    * State is persisted only when something actually changed, keeping
      the 0.5 s loop cheap.
"""

from __future__ import annotations

import logging
import time
from collections import OrderedDict
from datetime import datetime
from pathlib import Path
from typing import Optional

from .file_discovery import (
    complete_lines_from_tail,
    list_candidate_files,
    relative_source_file,
)
from .line_parser import parse_channel_line
from .state_manager import StateManager
from .upload_client import UploadClient


def scan_once(
    data_dir: Path,
    state: StateManager,
    client: UploadClient,
    quiet_seconds: float,
    verify: bool,
    dry_run: bool,
    persist_state: bool,
    max_records: Optional[int],
    logger: logging.Logger,
) -> int:
    files_state = state.files
    posted_keys = state.posted_keys

    if not data_dir.exists():
        logger.warning("OS265 data directory does not exist yet: %s", data_dir)
        return 0

    uploaded_count = 0
    state_changed = False
    candidate_files = list_candidate_files(data_dir)

    for path in candidate_files:
        key = str(path)
        try:
            stat = path.stat()
        except PermissionError:
            logger.warning("Transient lock on %s (stat), retry next scan", path.name)
            continue

        file_state = files_state.get(key, {})
        if not isinstance(file_state, dict):
            file_state = {}
        offset = int(file_state.get("offset", 0))

        try:
            # Growing files are read immediately; only an incomplete
            # trailing line is deferred inside complete_lines_from_tail.
            lines, next_offset = complete_lines_from_tail(path, offset, quiet_seconds)
        except PermissionError:
            logger.warning("Transient lock on %s (read), retry next scan", path.name)
            continue

        start_line_no = int(file_state.get("line", 0))
        if next_offset < offset:
            # Truncation/rotation reset: line numbering restarts too.
            start_line_no = 0

        if not lines:
            if next_offset != offset:
                files_state[key] = {
                    "offset": next_offset,
                    "line": start_line_no,
                    "size": stat.st_size,
                    "mtime": stat.st_mtime,
                }
                state_changed = True
            continue

        source_file = relative_source_file(path, data_dir)

        parsed_records = OrderedDict()
        for line_index, (line_offset, line) in enumerate(lines, start=start_line_no + 1):
            record = parse_channel_line(path, source_file, line, line_offset, line_index)
            if record:
                parsed_records[record.unique_key] = record

        # Failure-safe state advancement: if a record fails (after the
        # client's bounded retries) or the per-run cap is reached, stop
        # at that record and keep the file offset pointing at it, so it
        # is re-attempted next cycle. Failed uploads are never marked
        # successful and never skipped over.
        stop_record = None
        stopped_by_limit = False
        for record in parsed_records.values():
            if max_records is not None and uploaded_count >= max_records:
                stop_record = record
                stopped_by_limit = True
                break

            if record.unique_key in posted_keys:
                continue

            if client.upload_record(record, verify, dry_run):
                posted_keys[record.unique_key] = datetime.now().strftime(
                    "%Y-%m-%d %H:%M:%S")
                uploaded_count += 1
                state_changed = True
            else:
                stop_record = record
                logger.warning(
                    "Holding file offset at unsettled record for next cycle: "
                    "file=%s line=%s", source_file, record.source_line)
                break

        if stop_record is None:
            files_state[key] = {
                "offset": next_offset,
                "line": start_line_no + len(lines),
                "size": stat.st_size,
                "mtime": stat.st_mtime,
            }
        else:
            files_state[key] = {
                "offset": stop_record.source_offset,
                "line": stop_record.source_line - 1,
                "size": stat.st_size,
                "mtime": stat.st_mtime,
            }
        state_changed = True

        if stopped_by_limit:
            break

    if persist_state and state_changed:
        state.save()

    return uploaded_count


def run_loop(
    data_dir: Path,
    state: StateManager,
    client: UploadClient,
    scan_interval: float,
    quiet_seconds: float,
    verify: bool,
    dry_run: bool,
    once: bool,
    max_records: Optional[int],
    logger: logging.Logger,
) -> int:
    logger.info(
        "scan loop starting: dataDir=%s interval=%.2fs verifyAfterUpload=%s dryRun=%s",
        data_dir, scan_interval, verify, dry_run,
    )

    while True:
        cycle_started = time.monotonic()
        try:
            count = scan_once(
                data_dir=data_dir,
                state=state,
                client=client,
                quiet_seconds=quiet_seconds,
                verify=verify,
                dry_run=dry_run,
                persist_state=not dry_run,
                max_records=max_records,
                logger=logger,
            )
            if count > 0:
                logger.info(
                    "cycle done: uploaded=%d records, cycle=%.2fs, interval=%.2fs",
                    count, time.monotonic() - cycle_started, scan_interval,
                )
            if once:
                logger.info("scan once finished, uploaded=%s", count)
                return 0
        except KeyboardInterrupt:
            logger.info("collector stopped by user")
            if not dry_run:
                state.save()
            return 0
        except Exception as exc:
            logger.exception("collector loop error: %s", exc)
            if not dry_run:
                state.save()
            if once:
                return 1

        time.sleep(scan_interval)
