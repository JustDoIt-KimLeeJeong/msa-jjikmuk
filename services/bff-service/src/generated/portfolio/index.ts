import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

// --- Types ---

export interface Balance {
  available: number;
  reserved: number;
}

export interface Position {
  symbol: string;
  qty: number;
  avgPrice: number;
  realizedPnl: number;
}

export interface PortfolioSnapshotResponse {
  userId: number;
  balances: Balance;
  positions: Position[];
}

export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
}

// --- API Client ---

export class PortfolioApiClient {
  private client: AxiosInstance;

  constructor(baseURL: string) {
    this.client = axios.create({ baseURL });
  }

  async getPortfolioSnapshot(userId: number, config?: AxiosRequestConfig): Promise<PortfolioSnapshotResponse> {
    const response = await this.client.get<PortfolioSnapshotResponse>(`/api/portfolio/${userId}`, config);
    return response.data;
  }
}
