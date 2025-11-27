# 필요 없는 파일이니 삭제 가능


from typing import Any, Mapping, Callable, Dict, Union

KeyFn = Callable[[dict], str]
def _key_user_symbol(m: dict) -> str: return f"{m['userId']}:{m.get('symbol','')}"
def _key_user_order(m: dict) -> str:  return f"{m['userId']}:{m.get('orderId','')}"

EVENT_ROUTE: Dict[str, dict] = {
    # Reservation / Order
    "PurchaseOrderReserved": {"topic": "portfolio.PurchaseOrderReserved.v1", "key_fn": _key_user_order},
    "PurchaseOrderRejected": {"topic": "portfolio.PurchaseOrderRejected.v1", "key_fn": _key_user_order},
    "SellOrderReserved":     {"topic": "portfolio.SellOrderReserved.v1",     "key_fn": _key_user_order},
    "SellOrderRejected":     {"topic": "portfolio.SellOrderRejected.v1",     "key_fn": _key_user_order},
    "OrderPlaced":           {"topic": "portfolio.OrderPlaced.v1",           "key_fn": _key_user_order},
    "OrderCancelled":        {"topic": "portfolio.OrderCancelled.v1",        "key_fn": _key_user_order},
    "OrderExpired":          {"topic": "portfolio.OrderExpired.v1",          "key_fn": _key_user_order},

    # Execution 반영
    "PurchaseReserved":      {"topic": "portfolio.PurchaseReserved.v1",      "key_fn": _key_user_symbol},
    "PurchaseRejected":      {"topic": "portfolio.PurchaseRejected.v1",      "key_fn": _key_user_order},
    "SellReserved":          {"topic": "portfolio.SellReserved.v1",          "key_fn": _key_user_order},
    "SellRejected":          {"topic": "portfolio.SellRejected.v1",          "key_fn": _key_user_order},
    "ExecutionFail":         {"topic": "portfolio.ExecutionFail.v1",         "key_fn": _key_user_order},

    # 알람/시스템
    "PositionUpdateFailed":  {"topic": "portfolio.PositionUpdateFailed.v1",  "key_fn": _key_user_symbol},
    "PriceSyncDegraded":     {"topic": "portfolio.PriceSyncDegraded.v1",     "key_fn": lambda m: str(m["userId"])},
    "ValuationRecalcFailed": {"topic": "portfolio.ValuationRecalcFailed.v1", "key_fn": lambda m: str(m["userId"])},
    "IdempotencyConflict":   {"topic": "portfolio.IdempotencyConflict.v1",   "key_fn": lambda m: str(m["userId"])},
}