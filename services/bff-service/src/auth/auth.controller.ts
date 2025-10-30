import {
  Controller,
  Post,
  Body,
  HttpCode,
  ValidationPipe,
  Res,
  Logger,
} from '@nestjs/common';
import { FastifyReply } from 'fastify';
import { AuthService } from './auth.service';
import { SignupRequestDto } from './dto/signup-request.dto';
import { LoginRequestDto } from './dto/login-request.dto';
import { InjectMetric } from '@willsoto/nestjs-prometheus';
import { Counter, Histogram } from 'prom-client';

@Controller('/api/auth')
export class AuthController {
  private readonly logger = new Logger(AuthController.name);

  constructor(
    private readonly authService: AuthService,
    @InjectMetric('auth_requests_total')
    private readonly requestCounter: Counter<string>,
    @InjectMetric('auth_request_duration_seconds')
    private readonly requestDuration: Histogram<string>,
  ) {}

  @Post('signup')
  @HttpCode(201)
  async signup(
    @Body() dto: SignupRequestDto,
    @Res({ passthrough: true }) res: FastifyReply,
  ) {
    this.logger.log(`POST /api/auth/signup 요청 수신: ${dto.email}`);
    const end = this.requestDuration.startTimer();
    this.requestCounter.inc({ method: 'POST', endpoint: '/signup' });

    try {
      const { accessToken, userId } =
        await this.authService.signupAndLogin(dto);

      // JWT 토큰을 httpOnly 쿠키로 설정
      (res as any).setCookie('access_token', accessToken, {
        httpOnly: true,
        sameSite: 'lax',
        secure: process.env.NODE_ENV === 'production',
        path: '/',
        maxAge: 60 * 60 * 24 * 10, // 10일 (초 단위)
      });

      this.logger.log(`회원가입 + 자동로그인 응답 완료: userId=${userId}`);
      res.send({ userId }); // 사용자 정보 일부 응답
    } catch (error) {
      this.logger.error(
        `회원가입 요청 처리 실패: ${dto.email}`,
        error instanceof Error ? error.stack : String(error),
      );
      throw error;
    } finally {
      end();
    }
  }

  @Post('login')
  @HttpCode(200)
  async login(
    @Body() loginDto: LoginRequestDto,
    @Res({ passthrough: true }) res: FastifyReply,
  ) {
    this.logger.log(`POST /api/auth/login 요청 수신: ${loginDto.email}`);
    const end = this.requestDuration.startTimer();
    this.requestCounter.inc({ method: 'POST', endpoint: '/login' });

    try {
      const { accessToken, userId } = await this.authService.login(loginDto);

      // JWT 토큰을 httpOnly 쿠키로 설정
      (res as any).setCookie('access_token', accessToken, {
        httpOnly: true,
        sameSite: 'lax',
        secure: process.env.NODE_ENV === 'production',
        path: '/',
        maxAge: 60 * 60 * 24 * 10, // 10일 (초 단위)
      });

      this.logger.log(`로그인 응답 완료: userId=${userId}`);
      res.send({ userId }); // 사용자 정보 일부 응답
    } catch (error) {
      this.logger.error(
        `로그인 요청 처리 실패: ${loginDto.email}`,
        error instanceof Error ? error.stack : String(error),
      );
      throw error;
    } finally {
      end();
    }
  }

  @Post('logout')
  @HttpCode(200)
  async logout(@Res({ passthrough: true }) res: FastifyReply) {
    this.logger.log('POST /api/auth/logout 요청 수신');
    const end = this.requestDuration.startTimer();
    this.requestCounter.inc({ method: 'POST', endpoint: '/logout' });

    try {
      // JWT 토큰 쿠키 삭제 (Max-Age=0으로 만료)
      (res as any).setCookie('access_token', '', {
        httpOnly: true,
        sameSite: 'lax',
        secure: process.env.NODE_ENV === 'production',
        path: '/',
        maxAge: 0, // 즉시 만료
      });

      this.logger.log('로그아웃 완료');
      res.send({ message: 'Logged out' });
    } catch (error) {
      this.logger.error(
        '로그아웃 요청 처리 실패',
        error instanceof Error ? error.stack : String(error),
      );
      throw error;
    } finally {
      end();
    }
  }
}
