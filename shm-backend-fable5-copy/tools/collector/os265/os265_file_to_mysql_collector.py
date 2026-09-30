#!/usr/bin/env python3
"""OS265 file collector entrypoint.

Reads OS265 vendor record files (only *_通道*.txt; 物理量CH*.txt is always
excluded) and forwards normalized vendor readings to the backend's
unified vendor source layer:

    canonical: POST /api/sensor/os265/raw/upload
    fallback : POST /api/sensor/fiber/raw/upload (deprecated, transitional)

The intensity column (parts[4], around -10.x) is the primary
measuredValue/rawValue; the wavelength column (parts[3], around 1563.x)
is auxiliary only. Every uploaded record carries sourceFile and
sourceLine origin metadata.

Implementation lives in the os265_collector package next to this file.
"""

from __future__ import annotations

import sys
from pathlib import Path

# Allow running both as a script and with the os265 dir on sys.path.
sys.path.insert(0, str(Path(__file__).resolve().parent))

from os265_collector.config import LOG_FILE, parse_args
from os265_collector.log_setup import setup_logging
from os265_collector.scan_loop import run_loop
from os265_collector.state_manager import StateManager
from os265_collector.upload_client import UploadClient


def main() -> int:
    args = parse_args()
    logger = setup_logging()

    data_dir = Path(args.data_dir)
    state = StateManager(Path(args.state_file))
    client = UploadClient(
        base_url=args.base_url,
        timeout_seconds=args.timeout,
        logger=logger,
    )

    logger.info("OS265 file collector started")
    logger.info("listening data directory: %s", data_dir)
    logger.info("backend base url: %s", args.base_url)
    logger.info("state file: %s", state.state_file)
    logger.info("log file: %s", LOG_FILE)

    return run_loop(
        data_dir=data_dir,
        state=state,
        client=client,
        scan_interval=args.scan_interval,
        quiet_seconds=args.quiet_seconds,
        verify=args.verify,
        dry_run=args.dry_run,
        once=args.once,
        max_records=args.max_records,
        logger=logger,
    )


if __name__ == "__main__":
    raise SystemExit(main())
