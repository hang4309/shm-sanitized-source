"""Record model and canonical payload builder for OS265 readings."""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
from typing import Dict, Optional

from .config import DEVICE_NO, SOURCE_TYPE, VALIDATED_MODULE_BY_CHANNEL


def decimal_to_json(value: Decimal) -> object:
    text = format(value, "f")
    if "." not in text:
        return int(text)
    return float(text)


@dataclass(frozen=True)
class ParsedRecord:
    """One normalized OS265 channel reading.

    measured_value is the OS265 intensity/energy column (the primary
    business value, around -10.x); wavelength is auxiliary only
    (around 1563.x) and is never the primary metric.
    """

    source_file: str
    source_offset: int
    source_line: int
    channel: str
    measured_value: Decimal
    wavelength: Decimal
    collect_time: str

    @property
    def sensor_id(self) -> str:
        return f"FBG-STRAIN-CH{self.channel}"

    @property
    def channel_no(self) -> str:
        return f"CH{self.channel}"

    @property
    def module_key(self) -> Optional[str]:
        # Only the validated demo mapping (CH2 -> strain) is sent.
        # All other channels stay unassigned so the backend mapping
        # table is the single source of truth for module assignment.
        return VALIDATED_MODULE_BY_CHANNEL.get(self.channel)

    @property
    def unique_key(self) -> str:
        return (f"{self.sensor_id}|{self.collect_time}"
                f"|{self.source_file}|{self.source_line}")

    def to_payload(self) -> Dict[str, object]:
        payload: Dict[str, object] = {
            "sourceType": SOURCE_TYPE,
            "sensorId": self.sensor_id,
            "deviceNo": DEVICE_NO,
            "fiberNo": self.channel_no,
            "channelNo": self.channel_no,
            "rawValue": decimal_to_json(self.measured_value),
            "measuredValue": decimal_to_json(self.measured_value),
            "intensity": decimal_to_json(self.measured_value),
            "wavelength": decimal_to_json(self.wavelength),
            "wavelengthShift": 0,
            "collectTime": self.collect_time,
            # Source metadata is mandatory for OS265 file-origin records.
            "sourceFile": self.source_file,
            "sourceOffset": self.source_offset,
            "sourceLine": self.source_line,
        }
        if self.module_key is not None:
            payload["moduleKey"] = self.module_key
        return payload
