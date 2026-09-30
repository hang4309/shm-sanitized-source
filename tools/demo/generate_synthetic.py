#!/usr/bin/env python3
"""Generate six deterministic SYNTHETIC OS265-format records using only stdlib.

These invented values are for software demonstrations and regression tests.
They are not device measurements, calibrated strain, or other physical values.
The file contains no header or blank lines: sourceLine remains exactly 1..6.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timedelta
from decimal import Decimal
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_OUTPUT = (
    REPOSITORY_ROOT / "sample-data" / "os265" / "SYNTHETIC_demo_通道2.txt"
)


def synthetic_bytes() -> bytes:
    """Return fixed UTF-8/LF content; column 5 is primary, column 4 auxiliary."""
    start = datetime(2025, 1, 1)
    lines = []
    for index in range(6):
        timestamp = (start + timedelta(seconds=index)).strftime("%Y-%m-%d-%H:%M:%S")
        wavelength = Decimal("1550.000") + Decimal("0.001") * index
        primary = Decimal("10.0") + Decimal("0.25") * index
        lines.append(f"{timestamp} {index + 1} 1 {wavelength:.3f} {primary:.2f}\n")
    return "".join(lines).encode("utf-8")


def write_synthetic(output: Path) -> Path:
    """Create the fixture, reuse identical content, and refuse other overwrites."""
    output = output.expanduser().resolve()
    content = synthetic_bytes()
    output.parent.mkdir(parents=True, exist_ok=True)
    try:
        with output.open("xb") as handle:
            handle.write(content)
    except FileExistsError:
        if not output.is_file() or output.read_bytes() != content:
            raise FileExistsError(
                f"Refusing to overwrite different existing content: {output}"
            ) from None
    return output


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output", type=Path, default=DEFAULT_OUTPUT,
        help="output file (default: repository/sample-data/os265/SYNTHETIC_demo_通道2.txt)",
    )
    args = parser.parse_args()
    try:
        output = write_synthetic(args.output)
    except OSError as error:
        parser.exit(1, f"SYNTHETIC generation failed: {error}\n")
    print(f"SYNTHETIC: 6 invented records ready at {output}")
    print("For software demonstrations only; not calibrated physical measurements.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
