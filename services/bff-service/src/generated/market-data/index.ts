import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

// --- Types ---

export interface Symbol {
  symbol: string;
  name: string;
  market: 'KOSPI' | 'KOSDAQ';
  isin: string;
  currency: string;
  tickSize: number;
  priceLimitPct: number;
  lotSize: number;
  displayPrecision: number;
  active: boolean;
}

export interface SymbolsResponse {
  version: number;
  updatedAt: string;
  rules: {
    maxSymbols: number;
    defaultWatchlist: string[];
    intervals: string[];
    defaultQuoteLevel: number;
  };
  symbols: Symbol[];
}

export interface Price {
  symbol: string;
  name: string;
  last: number;
  chgPct: number;
  ts: number;
}

export interface PricesResponse {
  items: Price[];
  errors?: { symbol: string; code: string }[];
}

export interface Quote {
  symbol: string;
  name: string;
  bid: number;
  ask: number;
  bidSize: number;
  askSize: number;
  ts: number;
}

export interface Candle {
  ts: string; // ISO 8601
  o: number;
  h: number;
  l: number;
  c: number;
  v: number;
}

export interface CandlesResponse {
  symbol: string;
  interval: '1m' | '1d' | '1w';
  items: Candle[];
  errors?: { symbol: string; code: string }[];
}

export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
}

// --- API Client ---

export class MarketDataApiClient {
  private client: AxiosInstance;

  constructor(baseURL: string) {
    this.client = axios.create({ baseURL });
  }

  async getSymbols(config?: AxiosRequestConfig): Promise<SymbolsResponse> {
    const response = await this.client.get<SymbolsResponse>('/api/v1/market/symbols', config);
    return response.data;
  }

  async getPrices(symbols: string, config?: AxiosRequestConfig): Promise<PricesResponse> {
    const response = await this.client.get<PricesResponse>('/api/v1/market/prices', {
      ...config,
      params: { symbols, ...config?.params },
    });
    return response.data;
  }

  async getPriceBySymbol(symbol: string, config?: AxiosRequestConfig): Promise<Price> {
    const response = await this.client.get<Price>(`/api/v1/market/prices/${symbol}`, config);
    return response.data;
  }

  async getQuote(symbol: string, config?: AxiosRequestConfig): Promise<Quote> {
    const response = await this.client.get<Quote>(`/api/v1/market/quotes/${symbol}`, config);
    return response.data;
  }

  async getCandles(
    symbol: string,
    interval: '1m' | '1d' | '1w',
    from?: number,
    to?: number,
    limit?: number,
    config?: AxiosRequestConfig
  ): Promise<CandlesResponse> {
    const response = await this.client.get<CandlesResponse>(`/api/v1/market/candles/${symbol}`, {
      ...config,
      params: { interval, from, to, limit, ...config?.params },
    });
    return response.data;
  }
}
