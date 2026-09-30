"""HTTP upload client for the unified vendor source backend.

Upload policy:
    * Prefer the canonical endpoint POST /api/sensor/os265/raw/upload.
    * Fall back to the legacy fiber endpoint only when the canonical
      endpoint is missing (HTTP 404/405); every fallback hit logs a
      deprecation warning.
    * A record is never posted to both endpoints: canonical success
      (including backend-side duplicate) ends the attempt.

Concurrency safety:
    * A per-record in-flight set (guarded by a threading.Lock) ensures
      the same record identity (source file + source line) is never
      uploaded concurrently. The scan loop is single-threaded today, so
      the guard is cheap insurance: a skipped upload is logged and the
      record is retried on a later cycle, never queued.

Resilience:
    * Every HTTP call carries a hard timeout (default 5 s) so a stalled
      backend cannot freeze the collector.
    * Transient failures (connection error, timeout, HTTP 5xx, and HTTP
      429 — the backend documents Retry-After on its rate limiter) are
      retried up to 3 total attempts with 1 s / 2 s backoff.
    * Permanent failures (other 4xx, business rejections) are not
      retried and are never marked successful.
"""

from __future__ import annotations

import json
import logging
import socket
import threading
import time
from dataclasses import dataclass
from typing import Dict, Tuple
from urllib import error, request

from .config import (
    CANONICAL_HISTORY_PATH,
    CANONICAL_LATEST_PATH,
    CANONICAL_UPLOAD_PATH,
    LEGACY_HISTORY_PATH,
    LEGACY_LATEST_PATH,
    LEGACY_UPLOAD_PATH,
    UPLOAD_BACKOFF_SECONDS,
    UPLOAD_MAX_ATTEMPTS,
)
from .line_parser import decode_bytes
from .record_model import ParsedRecord

_DUPLICATE_TOKENS = (
    "duplicate", "already", "exists", "已存在",
    "uk_vendor_reading_origin", "uk_fiber_sensor_collect",
)

_FALLBACK_HTTP_STATUSES = (404, 405)


@dataclass(frozen=True)
class HttpResult:
    ok: bool
    obj: Dict[str, object]
    text: str
    status: int
    timed_out: bool


def post_json(url: str, payload: Dict[str, object],
              timeout_seconds: float) -> HttpResult:
    """POST JSON with a hard timeout; never raises."""
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=timeout_seconds) as resp:
            text = decode_bytes(resp.read())
            status = resp.status
    except error.HTTPError as exc:
        text = decode_bytes(exc.read())
        return HttpResult(False, parse_json_object(text), text, exc.code, False)
    except (socket.timeout, TimeoutError) as exc:
        return HttpResult(False, {}, f"timeout: {exc}", 0, True)
    except error.URLError as exc:
        timed_out = isinstance(getattr(exc, "reason", None), (socket.timeout, TimeoutError))
        return HttpResult(False, {}, str(exc), 0, timed_out)
    except Exception as exc:
        return HttpResult(False, {}, str(exc), 0, False)

    obj = parse_json_object(text)
    if obj and obj.get("code") != 200:
        return HttpResult(False, obj, text, status, False)
    return HttpResult(True, obj, text, status, False)


def parse_json_object(text: str) -> Dict[str, object]:
    try:
        value = json.loads(text)
    except Exception:
        return {}
    return value if isinstance(value, dict) else {}


def looks_like_duplicate_failure(text: str, obj: Dict[str, object]) -> bool:
    message = str(obj.get("message", "")) if obj else ""
    combined = f"{message} {text}".lower()
    return any(token in combined for token in _DUPLICATE_TOKENS)


def _is_transient(result: HttpResult) -> bool:
    """Retry only on connection error, timeout, HTTP 5xx, or HTTP 429
    (the snapshot backend documents Retry-After: 1 on rate limiting)."""
    if result.timed_out:
        return True
    if result.status == 0:
        return True  # connection-level failure
    if 500 <= result.status < 600:
        return True
    if result.status == 429:
        return True
    return False


class UploadClient:

    def __init__(self, base_url: str, timeout_seconds: float,
                 logger: logging.Logger):
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds
        self.logger = logger
        self._in_flight_lock = threading.Lock()
        self._in_flight: set[str] = set()

    def upload_record(self, record: ParsedRecord, verify: bool,
                      dry_run: bool) -> bool:
        """Upload one record with in-flight protection and bounded
        retries. Returns True only when the record is settled (stored
        now or already stored on the backend); failures are never
        reported as success."""
        if dry_run:
            self.logger.info(
                "DRY RUN sensorId=%s measuredValue/rawValue=%s auxiliary wavelength=%s "
                "collectTime=%s file=%s line=%s",
                record.sensor_id, record.measured_value, record.wavelength,
                record.collect_time, record.source_file, record.source_line,
            )
            return True

        identity = record.unique_key
        with self._in_flight_lock:
            if identity in self._in_flight:
                self.logger.warning(
                    "Upload skipped because previous upload is still in flight: record=%s",
                    identity)
                return False
            self._in_flight.add(identity)

        started = time.monotonic()
        try:
            return self._upload_with_retry(record, verify, started)
        finally:
            with self._in_flight_lock:
                self._in_flight.discard(identity)

    def _upload_with_retry(self, record: ParsedRecord, verify: bool,
                           started: float) -> bool:
        identity = record.unique_key

        for attempt in range(1, UPLOAD_MAX_ATTEMPTS + 1):
            settled, used_legacy, result = self._post_upload(record)
            duration_ms = int((time.monotonic() - started) * 1000)

            if settled:
                latest_ok = history_ok = False
                if verify:
                    latest_ok, history_ok = self._verify_record(record, used_legacy)
                self.logger.info(
                    "Upload completed: record=%s durationMs=%d status=%s attempts=%d "
                    "endpoint=%s measuredValue/rawValue=%s auxiliary wavelength=%s "
                    "collectTime=%s%s",
                    identity, duration_ms, result.status or "n/a", attempt,
                    "legacy-fiber" if used_legacy else "os265-canonical",
                    record.measured_value, record.wavelength, record.collect_time,
                    f" latest={latest_ok} history={history_ok}" if verify else "",
                )
                return True

            transient = _is_transient(result)
            reason = ("timeout" if result.timed_out
                      else f"http{result.status}" if result.status
                      else "connection-error")

            if not transient:
                self.logger.error(
                    "Upload failed (permanent, no retry): record=%s durationMs=%d "
                    "status=%s attempts=%d timeout=%s response=%s",
                    identity, duration_ms, result.status or "n/a", attempt,
                    result.timed_out, result.text[:300],
                )
                return False

            if attempt < UPLOAD_MAX_ATTEMPTS:
                delay = UPLOAD_BACKOFF_SECONDS[
                    min(attempt - 1, len(UPLOAD_BACKOFF_SECONDS) - 1)]
                self.logger.warning(
                    "Upload retry scheduled: record=%s attempt=%d/%d reason=%s "
                    "durationMs=%d delaySeconds=%s",
                    identity, attempt, UPLOAD_MAX_ATTEMPTS, reason,
                    duration_ms, delay,
                )
                time.sleep(delay)
            else:
                self.logger.error(
                    "Upload failed after retries: record=%s durationMs=%d "
                    "attempts=%d reason=%s timeout=%s",
                    identity, duration_ms, attempt, reason, result.timed_out,
                )

        return False

    def _post_upload(self, record: ParsedRecord) -> Tuple[bool, bool, HttpResult]:
        """One upload pass. Returns (settled, used_legacy, http_result)."""
        payload = record.to_payload()

        result = post_json(
            f"{self.base_url}{CANONICAL_UPLOAD_PATH}", payload, self.timeout_seconds)
        if result.ok:
            return True, False, result
        if looks_like_duplicate_failure(result.text, result.obj):
            self.logger.info("Already exists in backend, marking as done: %s",
                             record.unique_key)
            return True, False, result
        if result.status not in _FALLBACK_HTTP_STATUSES:
            return False, False, result

        # Transitional fallback only: canonical endpoint not deployed yet.
        self.logger.warning(
            "DEPRECATED fallback: canonical %s unavailable (HTTP %s), posting to legacy %s. "
            "Upgrade the backend to the unified vendor source endpoint.",
            CANONICAL_UPLOAD_PATH, result.status, LEGACY_UPLOAD_PATH,
        )
        legacy = post_json(
            f"{self.base_url}{LEGACY_UPLOAD_PATH}", payload, self.timeout_seconds)
        if legacy.ok:
            return True, True, legacy
        if looks_like_duplicate_failure(legacy.text, legacy.obj):
            self.logger.info("Already exists in backend (legacy), marking as done: %s",
                             record.unique_key)
            return True, True, legacy
        return False, True, legacy

    def _verify_record(self, record: ParsedRecord,
                       used_legacy: bool) -> Tuple[bool, bool]:
        latest_path = LEGACY_LATEST_PATH if used_legacy else CANONICAL_LATEST_PATH
        history_path = LEGACY_HISTORY_PATH if used_legacy else CANONICAL_HISTORY_PATH

        latest_ok = False
        history_ok = False

        result = post_json(
            f"{self.base_url}{latest_path}",
            {"sensorId": record.sensor_id},
            self.timeout_seconds,
        )
        if result.ok and isinstance(result.obj.get("data"), dict):
            data = result.obj["data"]
            latest_ok = (data.get("sensorId") == record.sensor_id
                         and data.get("collectTime") == record.collect_time)

        day = record.collect_time[:10]
        result = post_json(
            f"{self.base_url}{history_path}",
            {
                "sensorId": record.sensor_id,
                "startTime": f"{day} 00:00:00",
                "endTime": f"{day} 23:59:59",
                "limit": 500,
            },
            self.timeout_seconds,
        )
        if result.ok and isinstance(result.obj.get("data"), list):
            history_ok = any(
                item.get("sensorId") == record.sensor_id
                and item.get("collectTime") == record.collect_time
                for item in result.obj["data"]
                if isinstance(item, dict)
            )

        return latest_ok, history_ok
