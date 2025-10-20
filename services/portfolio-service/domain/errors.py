class DomainError(Exception) : 
    pass


class NotFound(DomainError):
    """리소스가 없을 때"""
    pass


class OrderNotFound(NotFound):
    def __init__(self, user_id: int, order_id: str):
        self.user_id = user_id
        self.order_id = order_id
        super().__init__(f"order not found: user={user_id}, order={order_id}")

class OrderDuplication(DomainError) : 
    def __init__(self, user_id: int, order_id: str):
        self.user_id = user_id
        self.order_id = order_id
        super().__init__(f"order duplicated: user={user_id}, order={order_id}")