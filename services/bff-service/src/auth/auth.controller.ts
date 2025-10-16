import {
  Controller,
  Post,
  Body,
  HttpCode,
  ValidationPipe,
  Res,
} from '@nestjs/common';
import { FastifyReply } from 'fastify';
import { AuthService } from './auth.service';
import { SignupRequestDto } from './dto/signup-request.dto';
import { LoginRequestDto } from './dto/login-request.dto';

@Controller('/api/auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('signup')
  @HttpCode(201)
  async signup(
    @Body() dto: SignupRequestDto,
    @Res({ passthrough: true }) res: FastifyReply,
  ) {
    const { accessToken, userId } = await this.authService.signupAndLogin(dto);

    // JWT 토큰을 httpOnly 쿠키로 설정
    (res as any).setCookie('access_token', accessToken, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: 60 * 60 * 24 * 10, // 10일 (초 단위)
    });

    res.send({ userId }); // 사용자 정보 일부 응답
  }

  @Post('login')
  @HttpCode(200)
  async login(
    @Body() loginDto: LoginRequestDto,
    @Res({ passthrough: true }) res: FastifyReply,
  ) {
    const { accessToken, userId } = await this.authService.login(loginDto);

    // JWT 토큰을 httpOnly 쿠키로 설정
    (res as any).setCookie('access_token', accessToken, {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: 60 * 60 * 24 * 10, // 10일 (초 단위)
    });

    res.send({ userId }); // 사용자 정보 일부 응답
  }

  @Post('logout')
  @HttpCode(200)
  async logout(@Res({ passthrough: true }) res: FastifyReply) {
    // JWT 토큰 쿠키 삭제 (Max-Age=0으로 만료)
    (res as any).setCookie('access_token', '', {
      httpOnly: true,
      sameSite: 'lax',
      secure: process.env.NODE_ENV === 'production',
      path: '/',
      maxAge: 0, // 즉시 만료
    });

    res.send({ message: 'Logged out' });
  }
}
