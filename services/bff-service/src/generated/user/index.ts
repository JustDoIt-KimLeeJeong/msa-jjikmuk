import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

// --- Types ---

export interface SignUpRequest {
  email: string;
  pwd: string;
  name: string;
}

export interface SignUpResponse {
  userId: string;
}

export interface LogInRequest {
  email: string;
  pwd: string;
}

export interface LogInResponse {
  accessToken: string;
}

// --- API Client ---

export class UserApiClient {
  private client: AxiosInstance;

  constructor(baseURL: string) {
    this.client = axios.create({ baseURL });
  }

  async signUp(request: SignUpRequest, config?: AxiosRequestConfig): Promise<SignUpResponse> {
    const response = await this.client.post<SignUpResponse>('/api/auth/signup', request, config);
    return response.data;
  }

  async logIn(request: LogInRequest, config?: AxiosRequestConfig): Promise<LogInResponse> {
    const response = await this.client.post<LogInResponse>('/api/auth/login', request, config);
    return response.data;
  }

  async logOut(config?: AxiosRequestConfig): Promise<void> {
    await this.client.post('/api/auth/logout', {}, config);
  }
}
