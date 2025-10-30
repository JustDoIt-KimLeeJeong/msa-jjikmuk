import { Module } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { HttpService } from '../common/http/http.service';
import {
  makeCounterProvider,
  makeHistogramProvider,
} from '@willsoto/nestjs-prometheus';

@Module({
  controllers: [AuthController],
  providers: [
    AuthService,
    HttpService,
    makeCounterProvider({
      name: 'auth_requests_total',
      help: 'Total number of auth requests',
      labelNames: ['method', 'endpoint'],
    }),
    makeHistogramProvider({
      name: 'auth_request_duration_seconds',
      help: 'Duration of auth requests in seconds',
      labelNames: ['method', 'endpoint'],
    }),
  ],
})
export class AuthModule {}
