import { Module } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { HttpService } from '../common/http/http.service';

@Module({
  controllers: [AuthController],
  providers: [AuthService, HttpService],
})
export class AuthModule {}