"""Line parsing for OS265 channel record files.

Channel file columns (whitespace separated):
    1: collect time
    2: sequence
    3: peak index
    4: wavelength      (~1563.x, auxiliary reference only)
    5: intensity       (~-10.x, primary measured/business value)
"""

from __future__ import annotations

import re
from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Optional

from .config import TIME_PATTERNS
from .record_model import ParsedRecord

_CHANNEL_PATTERNS = (
    r"\u901a\u9053\s*(\d+)",
    r"CH\s*(\d+)",
)


def decode_bytes(data: bytes) -> str:
    for encoding in ("utf-8-sig", "utf-8", "gbk"):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="replace")


def parse_decimal(value: str) -> Optional[Decimal]:
    text = value.strip().replace(",", "")
    if not text or text == "#":
        return None
    try:
        return Decimal(text)
    except InvalidOperation:
        return None


def normalize_collect_time(value: str) -> Optional[str]:
    text = value.strip()
    for pattern in TIME_PATTERNS:
        try:
            return datetime.strptime(text, pattern).strftime("%Y-%m-%d %H:%M:%S")
        except ValueError:
            pass

    match = re.match(r"^(\d{4})-(\d{2})-(\d{2})-(\d{2}:\d{2}:\d{2})$", text)
    if match:
        return f"{match.group(1)}-{match.group(2)}-{match.group(3)} {match.group(4)}"

    return None


def channel_from_filename(path: Path) -> Optional[str]:
    for pattern in _CHANNEL_PATTERNS:
        match = re.search(pattern, path.name, flags=re.IGNORECASE)
        if match:
            return match.group(1)
    return None


def parse_channel_line(path: Path, source_file: str, line: str,
                       offset: int, line_no: int) -> Optional[ParsedRecord]:
    """Parse one channel record line into a normalized reading.

    Lines without a parseable intensity (the primary measured value)
    are skipped entirely; values are never fabricated.
    """
    channel = channel_from_filename(path)
    if not channel:
        return None

    parts = line.strip().split()
    if len(parts) < 5:
        return None

    collect_time = normalize_collect_time(parts[0])
    wavelength = parse_decimal(parts[3])
    intensity = parse_decimal(parts[4])
    if not collect_time or wavelength is None or intensity is None:
        return None

    return ParsedRecord(
        source_file=source_file,
        source_offset=offset,
        source_line=line_no,
        channel=channel,
        measured_value=intensity,
        wavelength=wavelength,
        collect_time=collect_time,
    )
