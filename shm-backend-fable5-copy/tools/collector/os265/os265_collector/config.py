"""Constants and CLI argument parsing for the OS265 file collector."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

# tools/collector/os265
COLLECTOR_DIR = Path(__file__).resolve().parents[1]
# Neutral example location; select vendor data explicitly with --data-dir.
PROJECT_ROOT = COLLECTOR_DIR.parents[3]
DATA_DIR = PROJECT_ROOT / "sample-data" / "os265"
STATE_FILE = COLLECTOR_DIR / "state" / "os265_file_offsets.json"
LOG_FILE = COLLECTOR_DIR / "logs" / "os265_file_to_mysql.log"

DEFAULT_BASE_URL = "http://localhost:8080"

# Live-mode hot path defaults. The scan interval targets 0.5 s; OS265's
# own write/flush cadence (it rotates channel files roughly every 50 s
# and flushes buffered lines in chunks) is the upstream limit.
DEFAULT_SCAN_INTERVAL_SECONDS = 0.5
# Post-upload verification is off by default in live mode: it triples
# the HTTP calls per record and only exists for acceptance testing.
DEFAULT_VERIFY_AFTER_UPLOAD = False

# Windows file-lock retry: OS265 briefly holds exclusive handles while
# flushing; reads retry with a short backoff instead of failing fatally.
FILE_LOCK_RETRY_ATTEMPTS = 3
FILE_LOCK_RETRY_BACKOFF_SECONDS = 0.15

# Upload resilience: every HTTP call has a hard timeout so a stalled
# backend cannot freeze the collector, and transient failures
# (connection error / timeout / HTTP 5xx / HTTP 429 with Retry-After)
# are retried up to 3 total attempts with bounded backoff.
DEFAULT_UPLOAD_TIMEOUT_SECONDS = 5.0
UPLOAD_MAX_ATTEMPTS = 3
UPLOAD_BACKOFF_SECONDS = (1, 2, 4)


def _env_bool(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None or not value.strip():
        return default
    return value.strip().lower() in ("1", "true", "yes", "on")


def _env_float(name: str, default: float) -> float:
    value = os.getenv(name)
    if value is None or not value.strip():
        return default
    try:
        parsed = float(value)
    except ValueError:
        return default
    return parsed if parsed > 0 else default

SOURCE_TYPE = "OS265"
DEVICE_NO = "OS-265"
MAX_POSTED_KEYS = 50000

# Canonical unified vendor source endpoints.
CANONICAL_UPLOAD_PATH = "/api/sensor/os265/raw/upload"
CANONICAL_LATEST_PATH = "/api/data/os265/latest"
CANONICAL_HISTORY_PATH = "/api/data/os265/history"

# Transitional legacy fiber compatibility endpoints (deprecated).
LEGACY_UPLOAD_PATH = "/api/sensor/fiber/raw/upload"
LEGACY_LATEST_PATH = "/api/data/fiber/latest"
LEGACY_HISTORY_PATH = "/api/data/fiber/history"

# Validated demo mapping only: OS-265 CH2 -> strain (FBG-STRAIN-CH2).
# Other channels are never hardcoded here; the backend resolves their
# module from vendor_channel_mapping, or leaves them unassigned.
VALIDATED_MODULE_BY_CHANNEL = {"2": "strain"}

TIME_PATTERNS = (
    "%Y-%m-%d %H:%M:%S",
    "%Y-%m-%d-%H:%M:%S",
    "%Y/%m/%d %H:%M:%S",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="OS265 file to unified vendor source collector")
    parser.add_argument("--data-dir", default=str(DATA_DIR),
                        help="OS265 data record directory")
    parser.add_argument("--base-url",
                        default=os.getenv("OS265_COLLECTOR_BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--state-file", default=str(STATE_FILE),
                        help="state file path")
    parser.add_argument("--scan-interval", type=float,
                        default=_env_float("OS265_SCAN_INTERVAL_SECONDS",
                                           DEFAULT_SCAN_INTERVAL_SECONDS),
                        help="scan interval seconds "
                             "(env OS265_SCAN_INTERVAL_SECONDS, default 0.5)")
    parser.add_argument("--timeout", type=float,
                        default=DEFAULT_UPLOAD_TIMEOUT_SECONDS,
                        help="hard timeout seconds per upload HTTP call (default 5)")
    parser.add_argument("--quiet-seconds", type=float, default=2.0)
    parser.add_argument("--once", action="store_true", help="scan once then exit")
    parser.add_argument("--max-records", type=int, default=None,
                        help="limit uploaded records in this run")
    parser.add_argument("--verify", dest="verify", action="store_true",
                        default=_env_bool("OS265_VERIFY_AFTER_UPLOAD",
                                          DEFAULT_VERIFY_AFTER_UPLOAD),
                        help="verify each record via latest/history after upload "
                             "(env OS265_VERIFY_AFTER_UPLOAD, default false)")
    parser.add_argument("--no-verify", dest="verify", action="store_false",
                        help="force post-upload verification off")
    parser.add_argument("--dry-run", action="store_true",
                        help="parse and log without posting")
    return parser.parse_args()
