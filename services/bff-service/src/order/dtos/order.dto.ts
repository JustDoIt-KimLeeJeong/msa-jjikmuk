// 주문 요청 데이터 구조
export interface OrderRequestDto {
  correlationId: string;
  clientOrderId: string;
  symbol: string;
  side: string;
  type: string;
  quantity: number;
}

// 주문 응답 데이터 구조
export interface OrderResponseDto {
  orderId: string;
  correlationId: string;
  clientOrderId: string;
  symbol: string;
  side: string;
  type: string;
  quantity: number;
  filledQuantity: number;
  remainingQuantity: number;
  status: string;
  statusDisplayName: string;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
  active: boolean;
  filledPercentage: number;
  fullyFilled: boolean;
  partiallyFilled: boolean;
}