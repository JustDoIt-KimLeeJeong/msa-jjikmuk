# src/repositories/symbols.py
from typing import Dict, Optional

_name_map: Dict[str, str] = {}

def prime(name_map: Dict[str, str]) -> None:
    _name_map.clear()
    _name_map.update(name_map)

def get_name(symbol: str) -> Optional[str]:
    return _name_map.get(symbol)

def snapshot() -> Dict[str, str]:
    return dict(_name_map)
