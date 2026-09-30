#!/usr/bin/env python3
"""Check real query responses from an already-running SYNTHETIC demo backend.

Stdlib only. No upload, DB setup, mock server, or fallback data. Use a dedicated
local demo database containing exactly the six supplied synthetic samples.
PASS proves these four query responses agree; it does not certify device
acquisition, MySQL internals, physical accuracy, or browser rendering.
"""

from __future__ import annotations

import argparse
from decimal import Decimal
from http.client import HTTPException
import ipaddress
import json
import math
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener


DEFAULT_BASE_URL = "http://127.0.0.1:8080"
SOURCE_FILE = "SYNTHETIC_demo_通道2.txt"
ROW_COUNT = 6
START_TIME = "2025-01-01 00:00:00"
END_TIME = "2025-01-01 00:00:05"
ENDPOINTS = (
    "/api/data/os265/history", "/api/data/os265/latest",
    "/api/data/strain/history", "/api/data/strain/latest",
)
IDENTITY = {
    "sourceType": "OS265", "deviceNo": "OS-265", "channelNo": "CH2",
    "sensorId": "FBG-STRAIN-CH2", "moduleKey": "strain",
    "sourceFile": SOURCE_FILE,
}
MAX_RESPONSE_BYTES = 262144


class VerificationError(ValueError):
    """An observed response does not satisfy the synthetic demo contract."""


class RejectRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise HTTPError(req.full_url, code, "HTTP redirects are refused", headers, fp)


def loopback_origin(value: str) -> str:
    """Allow only a plain HTTP(S) loopback origin; never resolve arbitrary DNS."""
    if not value or any(char.isspace() or ord(char) < 32 for char in value):
        raise VerificationError("base URL must be a plain loopback HTTP(S) origin")
    try:
        parts = urlsplit(value)
        port = parts.port
        hostname = parts.hostname
    except ValueError as error:
        raise VerificationError("invalid base URL") from error
    if (parts.scheme not in ("http", "https") or not hostname
            or parts.username is not None or parts.password is not None
            or parts.path not in ("", "/") or parts.query or parts.fragment
            or "\\" in value or "%" in hostname):
        raise VerificationError("base URL must be a plain loopback HTTP(S) origin")
    # Pin localhost to a literal so a hosts-file change cannot redirect queries.
    if hostname == "localhost":
        hostname = "127.0.0.1"
    try:
        address = ipaddress.ip_address(hostname)
    except ValueError as error:
        raise VerificationError("only localhost or literal loopback IPs are allowed") from error
    if not address.is_loopback:
        raise VerificationError("non-loopback URLs are refused")
    if port is not None and port == 0:
        raise VerificationError("port must be between 1 and 65535")
    host = f"[{address}]" if address.version == 6 else str(address)
    return f"{parts.scheme}://{host}" + (f":{port}" if port is not None else "")


def query_payload(endpoint: str) -> dict:
    if endpoint not in ENDPOINTS:
        raise VerificationError("only the four read-only query endpoints are allowed")
    payload = {key: IDENTITY[key] for key in ("sensorId", "deviceNo", "channelNo")}
    if "/os265/" in endpoint:
        payload.update(sourceType="OS265", moduleKey="strain")
    if endpoint.endswith("/history"):
        # Request one extra row so a duplicate cannot hide behind LIMIT 6.
        payload.update(startTime=START_TIME, endTime=END_TIME, limit=ROW_COUNT + 1)
    return payload


def reject_nonfinite(value: str):
    raise VerificationError(f"non-finite JSON number: {value}")


def reject_duplicate_keys(pairs: list) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise VerificationError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def decode_response(body: bytes):
    try:
        return json.loads(body.decode("utf-8"), parse_float=Decimal,
                          parse_constant=reject_nonfinite,
                          object_pairs_hook=reject_duplicate_keys)
    except (UnicodeDecodeError, json.JSONDecodeError, RecursionError) as error:
        raise VerificationError("response is not valid UTF-8 JSON") from error


def unwrap_response(response):
    if not isinstance(response, dict):
        raise VerificationError("response must be a Result object")
    if type(response.get("code")) is not int or response["code"] != 200:
        raise VerificationError("Result.code must be integer 200")
    if "data" not in response or response["data"] is None:
        raise VerificationError("Result.data is missing or null")
    return response["data"]


def require_decimal(row: dict, field: str, expected: Decimal, location: str):
    value = row.get(field)
    if isinstance(value, bool) or not isinstance(value, (int, float, Decimal)):
        raise VerificationError(f"{location}.{field} must be a JSON number")
    number = Decimal(str(value))
    if not number.is_finite() or number != expected:
        raise VerificationError(f"{location}.{field} does not equal {expected}")


def verify_row(row, index: int, location: str):
    if not isinstance(row, dict):
        raise VerificationError(f"{location} must be an object")
    for field, expected in IDENTITY.items():
        if row.get(field) != expected:
            raise VerificationError(f"{location}.{field} does not match the SYNTHETIC fixture")
    timestamp = f"2025-01-01 00:00:{index:02d}"
    if row.get("collectTime") != timestamp:
        raise VerificationError(f"{location}.collectTime must be {timestamp}")
    if type(row.get("sourceLine")) is not int or row["sourceLine"] != index + 1:
        raise VerificationError(f"{location}.sourceLine must be {index + 1}")
    primary = Decimal("10.0") + Decimal("0.25") * index
    for field in ("rawValue", "measuredValue", "intensity"):
        require_decimal(row, field, primary, location)
    require_decimal(row, "wavelength", Decimal("1550") + Decimal("0.001") * index, location)


def verify_history(response, location: str):
    rows = unwrap_response(response)
    if not isinstance(rows, list) or len(rows) != ROW_COUNT:
        raise VerificationError(f"{location} must contain exactly {ROW_COUNT} rows")
    for index, row in enumerate(rows):
        verify_row(row, index, f"{location}[{index}]")
    return rows


def verify_latest(response, location: str):
    row = unwrap_response(response)
    verify_row(row, ROW_COUNT - 1, location)
    return row


def run_verification(base_url: str = DEFAULT_BASE_URL, timeout: float = 5.0) -> dict:
    report = {
        "dataset": "SYNTHETIC", "scope": "query API responses only",
        "status": "FAIL", "responses_received": 0, "checks": [],
    }
    try:
        origin = loopback_origin(base_url)
        if not math.isfinite(timeout) or not 0 < timeout <= 30:
            raise VerificationError("timeout must be greater than 0 and at most 30 seconds")
        report["base_url"] = origin
        # Ignore environment proxies and refuse redirects, including local redirects.
        opener = build_opener(ProxyHandler({}), RejectRedirects())
        results = {}
        for endpoint in ENDPOINTS:
            check = {"endpoint": endpoint, "status": "FAIL"}
            report["checks"].append(check)
            request = Request(origin + endpoint,
                              data=json.dumps(query_payload(endpoint)).encode("utf-8"),
                              headers={"Content-Type": "application/json", "Accept": "application/json"},
                              method="POST")
            try:
                with opener.open(request, timeout=timeout) as response:
                    report["responses_received"] += 1
                    check["http_status"] = response.status
                    if response.status != 200:
                        raise VerificationError(f"expected HTTP 200, received {response.status}")
                    body = response.read(MAX_RESPONSE_BYTES + 1)
                    if len(body) > MAX_RESPONSE_BYTES:
                        raise VerificationError("response exceeds size limit")
            except HTTPError as error:
                report["responses_received"] += 1
                check["http_status"] = error.code
                error.close()
                raise VerificationError(f"HTTP {error.code}: {error.reason}") from error
            parsed = decode_response(body)
            validator = verify_history if endpoint.endswith("/history") else verify_latest
            results[endpoint] = validator(parsed, endpoint)
            check["status"] = "PASS"
        canonical = results[ENDPOINTS[0]]
        if canonical != results[ENDPOINTS[2]]:
            raise VerificationError("canonical and strain history responses differ")
        if canonical[-1] != results[ENDPOINTS[1]] or canonical[-1] != results[ENDPOINTS[3]]:
            raise VerificationError("latest responses differ from the final history row")
        report.update(status="PASS", rows=ROW_COUNT, last_timestamp=END_TIME,
                      primary_last=11.25, auxiliary_last=1550.005)
    except (VerificationError, URLError, HTTPException, OSError, TimeoutError) as error:
        report["error"] = str(error)[:240]
    return report


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL,
                        help="dedicated demo backend loopback origin (default: %(default)s)")
    parser.add_argument("--timeout", type=float, default=5.0,
                        help="per-request timeout in seconds, > 0 and <= 30 (default: %(default)s)")
    args = parser.parse_args(argv)
    report = run_verification(args.base_url, args.timeout)
    print(json.dumps(report, ensure_ascii=True, separators=(",", ":")))
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
