"""Offset and posted-key state persistence for the OS265 collector."""

from __future__ import annotations

import json
import os
from collections import OrderedDict
from pathlib import Path
from typing import Dict

from .config import MAX_POSTED_KEYS


class StateManager:
    """Persists per-file offsets and recently posted record keys."""

    def __init__(self, state_file: Path):
        self.state_file = state_file
        self.state: Dict[str, object] = self._load()

    def _load(self) -> Dict[str, object]:
        if not self.state_file.exists():
            return {"files": {}, "posted_keys": OrderedDict()}

        try:
            with self.state_file.open("r", encoding="utf-8") as fh:
                state = json.load(fh, object_pairs_hook=OrderedDict)
        except Exception:
            return {"files": {}, "posted_keys": OrderedDict()}

        if not isinstance(state, dict):
            state = {}
        state.setdefault("files", {})
        state.setdefault("posted_keys", OrderedDict())
        if not isinstance(state["files"], dict):
            state["files"] = {}
        if not isinstance(state["posted_keys"], dict):
            state["posted_keys"] = OrderedDict()
        return state

    @property
    def files(self) -> Dict[str, object]:
        return self.state["files"]  # type: ignore[return-value]

    @property
    def posted_keys(self) -> Dict[str, str]:
        return self.state["posted_keys"]  # type: ignore[return-value]

    def save(self) -> None:
        posted = self.state.get("posted_keys", OrderedDict())
        if isinstance(posted, dict) and len(posted) > MAX_POSTED_KEYS:
            self.state["posted_keys"] = OrderedDict(
                list(posted.items())[-MAX_POSTED_KEYS:])

        # Atomic write: dump to a temp file in the same directory,
        # flush + fsync, then os.replace() onto the real state file.
        # The previous valid state stays intact if anything fails
        # before the replace; a crash can never leave a partial file.
        self.state_file.parent.mkdir(parents=True, exist_ok=True)
        temp_path = self.state_file.with_suffix(".tmp")
        try:
            with temp_path.open("w", encoding="utf-8") as fh:
                json.dump(self.state, fh, ensure_ascii=False, indent=2)
                fh.flush()
                os.fsync(fh.fileno())
            os.replace(temp_path, self.state_file)
        except Exception:
            try:
                temp_path.unlink(missing_ok=True)
            except OSError:
                pass
            raise
