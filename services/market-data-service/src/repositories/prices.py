# src/repositories/prices.py
from typing import Dict

_state = {"last": {}, "chgPct": {}}

def prime(state: Dict):
    _state.clear()
    _state.update(state)

def get_state() -> Dict:
    return _state
