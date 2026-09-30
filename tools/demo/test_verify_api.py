#!/usr/bin/env python3
"""UNIT tests of the verifier, with invented response fixtures and mocked I/O.

Passing these tests is NOT a live backend, MySQL, device, or E2E verification.
Run verify_api.py separately against a dedicated, already-running demo backend.
"""

import copy
from decimal import Decimal
from email.message import Message
import io
import json
import unittest
from unittest.mock import patch
from urllib.error import HTTPError, URLError

import verify_api as verifier


def invented_row(index):
    # Independent fixed fixture: these are not observations from any backend.
    return {
        "sourceType": "OS265", "deviceNo": "OS-265", "channelNo": "CH2",
        "fiberNo": "CH2", "sensorId": "FBG-STRAIN-CH2", "moduleKey": "strain",
        "sourceFile": "SYNTHETIC_demo_通道2.txt", "sourceLine": index + 1,
        "collectTime": f"2025-01-01 00:00:{index:02d}",
        "rawValue": [10, 10.25, 10.5, 10.75, 11, 11.25][index],
        "measuredValue": [10, 10.25, 10.5, 10.75, 11, 11.25][index],
        "intensity": [10, 10.25, 10.5, 10.75, 11, 11.25][index],
        "wavelength": [1550, 1550.001, 1550.002, 1550.003, 1550.004, 1550.005][index],
        "wavelengthShift": None,
    }


def invented_response(data):
    return {"code": 200, "message": "success", "data": data}


class VerifierTests(unittest.TestCase):
    def setUp(self):
        self.rows = [invented_row(index) for index in range(6)]

    def test_valid_fixture_shape_only(self):
        result = verifier.verify_history(invented_response(self.rows), "UNIT fixture")
        self.assertEqual(len(result), 6)
        verifier.verify_latest(invented_response(self.rows[-1]), "UNIT fixture")

    def test_business_errors_and_bad_wrappers_rejected(self):
        for response in (None, [], {}, {"code": 500, "data": self.rows},
                         {"code": "200", "data": self.rows},
                         {"code": 200}, {"code": 200, "data": None}):
            with self.subTest(response=response):
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_history(response, "UNIT fixture")

    def test_missing_extra_and_nonlist_rows_rejected(self):
        for rows in ([], self.rows[:-1], self.rows + [self.rows[-1]], self.rows[0]):
            with self.subTest(rows=rows):
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_history(invented_response(rows), "UNIT fixture")

    def test_descending_duplicate_and_out_of_order_rows_rejected(self):
        candidates = [self.rows[::-1], [self.rows[0]] * 6,
                      [self.rows[1], self.rows[0]] + self.rows[2:]]
        for rows in candidates:
            with self.subTest(rows=rows):
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_history(invented_response(rows), "UNIT fixture")

    def test_missing_required_fields_rejected(self):
        for key in self.rows[0]:
            if key in ("fiberNo", "wavelengthShift"):
                continue
            rows = copy.deepcopy(self.rows)
            del rows[0][key]
            with self.subTest(field=key):
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_history(invented_response(rows), "UNIT fixture")

    def test_wrong_sensor_channel_file_and_line_rejected(self):
        for field, value in (("sensorId", "another-sensor"), ("channelNo", "2"),
                             ("sourceType", "OTHER"), ("deviceNo", "OS-999"),
                             ("moduleKey", "stress"), ("sourceFile", "real-device.txt"),
                             ("sourceLine", 0), ("sourceLine", True)):
            rows = copy.deepcopy(self.rows)
            rows[0][field] = value
            with self.subTest(field=field, value=value):
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_history(invented_response(rows), "UNIT fixture")

    def test_wrong_and_swapped_numeric_values_rejected(self):
        for field in ("rawValue", "measuredValue", "intensity", "wavelength"):
            for value in (None, True, "10.0", float("nan"), float("inf"), -1,
                          10 if field == "wavelength" else 1550):
                rows = copy.deepcopy(self.rows)
                rows[0][field] = value
                with self.subTest(field=field, value=value):
                    with self.assertRaises(verifier.VerificationError):
                        verifier.verify_history(invented_response(rows), "UNIT fixture")

    def test_stale_or_list_latest_rejected(self):
        for latest in (None, self.rows, self.rows[-2]):
            with self.subTest(latest=latest):
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_latest(invented_response(latest), "UNIT fixture")

    def test_malformed_nonfinite_and_duplicate_json_rejected(self):
        for body in (b"<html>error</html>", b"{", b"\xff", b'{"code":200,"code":500}',
                     b'{"data":NaN}', b'{"data":Infinity}'):
            with self.subTest(body=body):
                with self.assertRaises(verifier.VerificationError):
                    verifier.decode_response(body)
        decoded = verifier.decode_response(b'{"value":1550.001}')
        self.assertEqual(decoded["value"], Decimal("1550.001"))

    def test_loopback_origin_accepted_and_localhost_pinned(self):
        self.assertEqual(verifier.loopback_origin("http://localhost:8080/"),
                         "http://127.0.0.1:8080")
        self.assertEqual(verifier.loopback_origin("https://127.0.0.1:8443"),
                         "https://127.0.0.1:8443")
        self.assertEqual(verifier.loopback_origin("http://[::1]:8080"),
                         "http://[::1]:8080")

    def test_nonloopback_and_ambiguous_urls_refused_before_io(self):
        urls = ["http://example.com", "http://192.0.2.1", "http://0.0.0.0",
                "http://localhost.example.com", "http://127.0.0.1@evil.example",
                "http://127.0.0.1:8080/path", "http://127.0.0.1?x=1",
                "http://127.0.0.1#fragment", "file:///tmp/demo", "http://[::]",
                "http://[::1%25zone]", "http://127.0.0.1:0", "http://127.1",
                "http://user:secret@127.0.0.1", "http://127.0.0.1:99999",
                "http://127.0.0.1\n", "http://localhost\\@example.com"]
        with patch.object(verifier, "build_opener") as opener:
            for value in urls:
                with self.subTest(url=value):
                    report = verifier.run_verification(value)
                    self.assertEqual(report["status"], "FAIL")
                    self.assertEqual(report["responses_received"], 0)
            opener.assert_not_called()

    def test_queries_are_read_only_and_detect_extra_rows(self):
        for endpoint in verifier.ENDPOINTS:
            payload = verifier.query_payload(endpoint)
            self.assertEqual(payload["sensorId"], "FBG-STRAIN-CH2")
            if endpoint.endswith("/history"):
                self.assertEqual(payload["limit"], 7)
                self.assertEqual(payload["startTime"], "2025-01-01 00:00:00")
                self.assertEqual(payload["endTime"], "2025-01-01 00:00:05")
            else:
                self.assertNotIn("startTime", payload)
            if "/strain/" in endpoint:
                self.assertNotIn("moduleKey", payload)
                self.assertNotIn("sourceType", payload)
        with self.assertRaises(verifier.VerificationError):
            verifier.query_payload("/api/sensor/os265/upload")

    def test_connection_failure_cannot_pass(self):
        with patch.object(verifier, "build_opener") as factory:
            factory.return_value.open.side_effect = URLError("UNIT simulated offline")
            report = verifier.run_verification()
        self.assertEqual(report["status"], "FAIL")
        self.assertEqual(report["responses_received"], 0)
        self.assertNotIn("rows", report)
        self.assertIn("offline", report["error"])

    def test_http_error_cannot_pass(self):
        with patch.object(verifier, "build_opener") as factory:
            factory.return_value.open.side_effect = HTTPError(
                "http://127.0.0.1:8080", 503, "UNIT unavailable", Message(), io.BytesIO())
            report = verifier.run_verification()
        self.assertEqual(report["status"], "FAIL")
        self.assertEqual(report["responses_received"], 1)
        self.assertEqual(report["checks"][0]["http_status"], 503)

    def test_invalid_timeout_refused_before_io(self):
        with patch.object(verifier, "build_opener") as factory:
            for timeout in (0, -1, 31, float("nan"), float("inf")):
                self.assertEqual(verifier.run_verification(timeout=timeout)["status"], "FAIL")
            factory.assert_not_called()

    def test_redirect_handler_never_follows_location(self):
        request = verifier.Request("http://127.0.0.1:8080/api/data/os265/history")
        with self.assertRaises(HTTPError) as caught:
            verifier.RejectRedirects().redirect_request(
                request, None, 302, "UNIT redirect", Message(), "http://example.com")
        self.assertEqual(caught.exception.code, 302)
        caught.exception.close()

    def test_cross_endpoint_mismatch_cannot_pass(self):
        bodies = [invented_response(copy.deepcopy(self.rows)),
                  invented_response(copy.deepcopy(self.rows[-1])),
                  invented_response(copy.deepcopy(self.rows)),
                  invented_response(copy.deepcopy(self.rows[-1]))]
        # A field outside the individual row assertions must still match.
        bodies[2]["data"][0]["wavelengthShift"] = 99
        replies = []
        for body in bodies:
            reply = io.BytesIO(json.dumps(body).encode("utf-8"))
            reply.status = 200
            replies.append(reply)
        with patch.object(verifier, "build_opener") as factory:
            factory.return_value.open.side_effect = replies
            report = verifier.run_verification()
            requests = factory.return_value.open.call_args_list
        self.assertEqual(report["status"], "FAIL")
        self.assertEqual(report["responses_received"], 4)
        self.assertIn("history responses differ", report["error"])
        self.assertEqual([call.args[0].method for call in requests], ["POST"] * 4)
        self.assertEqual([call.args[0].full_url for call in requests],
                         [verifier.DEFAULT_BASE_URL + path for path in verifier.ENDPOINTS])


if __name__ == "__main__":
    unittest.main()
