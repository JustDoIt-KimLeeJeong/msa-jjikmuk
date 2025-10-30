import { Injectable, Logger } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class HttpService {
  private readonly logger = new Logger(HttpService.name);
  private readonly client: AxiosInstance;

  constructor(private readonly configService: ConfigService) {
    this.client = axios.create({
      baseURL: this.configService.get('USER_SERVICE_URL'),
      timeout: 3000,
    });

    // 요청 인터셉터: 요청 로깅
    this.client.interceptors.request.use(
      (config) => {
        this.logger.log(
          `HTTP 요청: ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`,
        );
        return config;
      },
      (error) => {
        this.logger.error('HTTP 요청 생성 실패', error);
        return Promise.reject(error);
      },
    );

    // 응답 인터셉터: 응답 로깅
    this.client.interceptors.response.use(
      (response) => {
        this.logger.log(
          `HTTP 응답: ${response.config.method?.toUpperCase()} ${response.config.url} - 상태코드: ${response.status}`,
        );
        return response;
      },
      (error) => {
        if (error.response) {
          this.logger.error(
            `HTTP 응답 에러: ${error.config?.method?.toUpperCase()} ${error.config?.url} - 상태코드: ${error.response.status}`,
            error.stack,
          );
        } else if (error.request) {
          this.logger.error(
            `HTTP 요청 전송 실패: ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
            error.stack,
          );
        } else {
          this.logger.error('HTTP 요청 설정 에러', error.stack);
        }
        return Promise.reject(error);
      },
    );
  }

  get(url: string, config?: any) {
    return this.client.get(url, config);
  }

  post(url: string, data?: any, config?: any) {
    return this.client.post(url, data, config);
  }
}
