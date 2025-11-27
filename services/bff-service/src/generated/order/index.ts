import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

// --- Types ---

export interface CreateOrderRequest {
  correlationId: string;
  clientOrderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'MARKET' | 'LIMIT';
  price?: number | null;
  quantity: number;
}

export interface OrderResponse {
  orderId: number;
  correlationId: string;
  clientOrderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'MARKET' | 'LIMIT';
  price?: number | null;
  quantity: number;
  filledQuantity: number;
  remainingQuantity: number;
  status: 'PENDING' | 'ACCEPTED' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'REJECTED';
  statusDisplayName: string;
  expiresAt?: string | null;
  createdAt: string;
  updatedAt: string;
  active: boolean;
  filledPercentage: number;
  fullyFilled: boolean;
  partiallyFilled: boolean;
}

export interface OrderListResponse {
  content: OrderResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  first: boolean;
  last: boolean;
}

export interface CancelOrderResponse extends OrderResponse {}

export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
}

// --- API Client ---

export class OrderApiClient {
  private client: AxiosInstance;

  constructor(baseURL: string) {
    this.client = axios.create({ baseURL });
  }

  async createOrder(request: CreateOrderRequest, config?: AxiosRequestConfig): Promise<OrderResponse> {
    const response = await this.client.post<OrderResponse>('/api/orders', request, config);
    return response.data;
  }

  async getOrders(page: number = 0, size: number = 20, config?: AxiosRequestConfig): Promise<OrderListResponse> {
    const response = await this.client.get<OrderListResponse>('/api/orders', {
      ...config,
      params: { page, size, ...config?.params },
    });
    return response.data;
  }

  async getOrderById(orderId: number, config?: AxiosRequestConfig): Promise<OrderResponse> {
    const response = await this.client.get<OrderResponse>(`/api/orders/${orderId}`, config);
    return response.data;
  }

  async cancelOrder(orderId: number, config?: AxiosRequestConfig): Promise<CancelOrderResponse> {
    const response = await this.client.delete<CancelOrderResponse>(`/api/orders/${orderId}`, config);
    return response.data;
  }
}
