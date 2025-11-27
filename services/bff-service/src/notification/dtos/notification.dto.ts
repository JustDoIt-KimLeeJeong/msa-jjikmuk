// 알림 이벤트 타입
export enum NotificationEventType {
  TRADE_EXECUTED = 'trade.executed',
  ORDER_CANCELLED = 'order.cancelled',
  ORDER_CANCEL_REJECTED = 'order.cancel_rejected',
  ORDER_EXPIRED = 'order.expired',
  ORDER_REJECTED = 'order.rejected',
}

// 알림 데이터 구조
export interface NotificationData {
  title: string;
  body: string;
  createdAt: string; // ISO8601
}
