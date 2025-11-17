#io.py
import json, pathlib
def load_json(p: pathlib.Path):
    with open(p, "r", encoding="utf-8") as f:
        return json.load(f)