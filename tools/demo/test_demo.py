"""Stdlib regression tests with a loopback HTTP recorder, not MySQL or E2E.

Run from the repository root:
    python -B -m unittest discover -s tools/demo -p test_demo.py -v

The existing collector Python files are copied byte-for-byte to a temporary
directory before its CLI is launched, isolating its fixed log location.
No application source is patched. Input, logs, and state are temporary.
"""

from __future__ import annotations

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import threading
import unittest
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, HTTPServer
from unittest.mock import patch


sys.dont_write_bytecode = True
DEMO_DIR = Path(__file__).resolve().parent
REPOSITORY_ROOT = DEMO_DIR.parents[1]
COLLECTOR_DIR = (
    REPOSITORY_ROOT / "shm-backend-fable5-copy" / "tools" / "collector" / "os265"
)
sys.path.insert(0, str(DEMO_DIR))
sys.path.insert(0, str(COLLECTOR_DIR))

import generate_synthetic
from os265_collector.file_discovery import complete_lines_from_tail, list_candidate_files
from os265_collector.line_parser import parse_channel_line


FILENAME = "SYNTHETIC_demo_通道2.txt"
EXPECTED_TEXT = (
    "2025-01-01-00:00:00 1 1 1550.000 10.00\n"
    "2025-01-01-00:00:01 2 1 1550.001 10.25\n"
    "2025-01-01-00:00:02 3 1 1550.002 10.50\n"
    "2025-01-01-00:00:03 4 1 1550.003 10.75\n"
    "2025-01-01-00:00:04 5 1 1550.004 11.00\n"
    "2025-01-01-00:00:05 6 1 1550.005 11.25\n"
)


def python_environment() -> dict[str, str]:
    env = os.environ.copy()
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    env["PYTHONUTF8"] = "1"
    env["NO_PROXY"] = "127.0.0.1,localhost"
    env["no_proxy"] = "127.0.0.1,localhost"
    return env


@contextmanager
def recording_server():
    """Record HTTP requests only; the 200 reply does not simulate persistence."""
    requests = []

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            requests.append((self.path, payload))
            response = b'{"code":200,"data":null}'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(response)))
            self.end_headers()
            self.wfile.write(response)

        def log_message(self, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(
        target=server.serve_forever, kwargs={"poll_interval": 0.02}, daemon=True
    )
    thread.start()
    try:
        yield f"http://127.0.0.1:{server.server_address[1]}", requests
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)


class DemoRegressionTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="shm-synthetic-test-")
        self.addCleanup(self.temporary.cleanup)
        self.work = Path(self.temporary.name)

    def run_python(self, *args):
        result = subprocess.run(
            [sys.executable, "-B", *map(str, args)], cwd=self.work,
            env=python_environment(), text=True, encoding="utf-8",
            capture_output=True, timeout=20,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        return result

    def copy_collector(self):
        destination = self.work / "collector"
        for source in COLLECTOR_DIR.rglob("*.py"):
            target = destination / source.relative_to(COLLECTOR_DIR)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            self.assertEqual(target.read_bytes(), source.read_bytes())
        entrypoint = destination / "os265_file_to_mysql_collector.py"
        self.assertTrue(entrypoint.is_file())
        return entrypoint

    def run_collector(self, entrypoint, data_dir, state_file, base_url):
        return self.run_python(
            entrypoint, "--data-dir", data_dir, "--state-file", state_file,
            "--base-url", base_url, "--once", "--no-verify",
            "--quiet-seconds", "3600", "--timeout", "2",
        )

    def test_generator_cli_is_deterministic_and_labels_synthetic_data(self):
        output = self.work / "input" / FILENAME
        result = self.run_python(DEMO_DIR / "generate_synthetic.py", "--output", output)
        self.assertEqual(output.read_bytes(), EXPECTED_TEXT.encode("utf-8"))
        self.assertIn("SYNTHETIC", result.stdout)
        self.assertIn("not calibrated physical measurements", result.stdout)
        self.run_python(DEMO_DIR / "generate_synthetic.py", "--output", output)
        self.assertEqual(output.read_bytes(), EXPECTED_TEXT.encode("utf-8"))
        self.assertEqual(
            generate_synthetic.DEFAULT_OUTPUT,
            REPOSITORY_ROOT / "sample-data" / "os265" / FILENAME,
        )

    def test_generator_preserves_different_existing_content(self):
        output = self.work / FILENAME
        output.write_bytes(b"existing unrelated content\n")
        with self.assertRaisesRegex(FileExistsError, "Refusing to overwrite"):
            generate_synthetic.write_synthetic(output)
        self.assertEqual(output.read_bytes(), b"existing unrelated content\n")

    def test_real_cli_uploads_six_payloads_and_second_process_does_not_repeat(self):
        entrypoint = self.copy_collector()
        data_dir = self.work / "input"
        data_file = generate_synthetic.write_synthetic(data_dir / FILENAME)
        state_file = self.work / "state" / "offsets.json"
        with recording_server() as (base_url, requests):
            self.run_collector(entrypoint, data_dir, state_file, base_url)
            self.assertEqual(len(requests), 6)
            offset = 0
            for index, (path, payload) in enumerate(requests):
                with self.subTest(record=index + 1):
                    self.assertEqual(path, "/api/sensor/os265/raw/upload")
                    self.assertEqual(payload, {
                        "sourceType": "OS265", "sensorId": "FBG-STRAIN-CH2",
                        "deviceNo": "OS-265", "fiberNo": "CH2", "channelNo": "CH2",
                        "rawValue": 10.0 + 0.25 * index,
                        "measuredValue": 10.0 + 0.25 * index,
                        "intensity": 10.0 + 0.25 * index,
                        "wavelength": float(f"1550.{index:03d}"),
                        "wavelengthShift": 0,
                        "collectTime": f"2025-01-01 00:00:0{index}",
                        "sourceFile": FILENAME, "sourceOffset": offset,
                        "sourceLine": index + 1, "moduleKey": "strain",
                    })
                offset += len(EXPECTED_TEXT.splitlines(keepends=True)[index].encode("utf-8"))
            state_before = state_file.read_bytes()
            state = json.loads(state_before)
            self.assertEqual(set(state["files"]), {str(data_file)})
            self.assertEqual(state["files"][str(data_file)]["offset"], data_file.stat().st_size)
            self.assertEqual(state["files"][str(data_file)]["line"], 6)
            self.assertEqual(set(state["posted_keys"]), {
                f"FBG-STRAIN-CH2|2025-01-01 00:00:0{i}|{FILENAME}|{i + 1}"
                for i in range(6)
            })
            second = self.run_collector(entrypoint, data_dir, state_file, base_url)
            self.assertEqual(len(requests), 6, "Second CLI process uploaded duplicate data")
            self.assertEqual(state_file.read_bytes(), state_before)
            self.assertIn("uploaded=0", second.stdout + second.stderr)

    def test_real_cli_resumes_partial_tail_and_skips_malformed_record(self):
        entrypoint = self.copy_collector()
        data_dir = self.work / "input"
        data_dir.mkdir()
        data_file = data_dir / FILENAME
        first, second, third = EXPECTED_TEXT.splitlines(keepends=True)[:3]
        partial = second[:-3]
        data_file.write_bytes((first + partial).encode("utf-8"))
        state_file = self.work / "state.json"
        with recording_server() as (base_url, requests):
            self.run_collector(entrypoint, data_dir, state_file, base_url)
            self.assertEqual(len(requests), 1)
            state = json.loads(state_file.read_bytes())
            self.assertEqual(state["files"][str(data_file)]["offset"], len(first.encode("utf-8")))
            with data_file.open("ab") as handle:
                handle.write((second[-3:] + "MALFORMED\n" + third).encode("utf-8"))
            self.run_collector(entrypoint, data_dir, state_file, base_url)
            self.assertEqual(len(requests), 3)
            self.assertEqual([body["sourceLine"] for _, body in requests], [1, 2, 4])
            self.assertEqual([body["measuredValue"] for _, body in requests], [10.0, 10.25, 10.5])
            state = json.loads(state_file.read_bytes())
            self.assertEqual(state["files"][str(data_file)]["offset"], data_file.stat().st_size)
            self.assertEqual(state["files"][str(data_file)]["line"], 4)
            self.assertEqual(len(state["posted_keys"]), 3)

    def test_incomplete_tail_waits_until_completed(self):
        path = self.work / FILENAME
        path.write_bytes(b"complete\npartial")
        with patch("os265_collector.file_discovery.time.time", return_value=path.stat().st_mtime + 1):
            lines, offset = complete_lines_from_tail(path, 0, quiet_seconds=60)
        self.assertEqual(lines, [(0, "complete")])
        self.assertEqual(offset, len(b"complete\n"))
        with path.open("ab") as handle:
            handle.write(b" done\n")
        lines, final_offset = complete_lines_from_tail(path, offset, quiet_seconds=60)
        self.assertEqual(lines, [(offset, "partial done")])
        self.assertEqual(final_offset, path.stat().st_size)

    def test_malformed_lines_are_rejected(self):
        for text in (
            "# SYNTHETIC comment, not a reading", "MALFORMED",
            "bad-time 1 1 1550.000 10.0",
            "2025-01-01-00:00:00 1 1 invalid 10.0",
            "2025-01-01-00:00:00 1 1 1550.000 invalid",
        ):
            with self.subTest(text=text):
                self.assertIsNone(parse_channel_line(self.work / FILENAME, FILENAME, text, 0, 1))

    def test_only_channel_txt_files_are_selected_and_physical_exclusion_wins(self):
        names = [FILENAME, "物理量CH2_通道2.txt", "ordinary_CH2.txt", "SYNTHETIC_通道2.csv"]
        for name in names:
            (self.work / name).write_bytes(b"unused\n")
        (self.work / "directory_通道2.txt").mkdir()
        self.assertEqual(list_candidate_files(self.work), [self.work / FILENAME])


if __name__ == "__main__":
    unittest.main()
