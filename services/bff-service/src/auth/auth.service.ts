import {
  Injectable,
  HttpException,
  HttpStatus,
  UnauthorizedException,
  Logger,
} from '@nestjs/common';
import { HttpService } from '../common/http/http.service';
import { SignupRequestDto } from './dto/signup-request.dto';
import { LoginRequestDto } from './dto/login-request.dto';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(private readonly http: HttpService) {}

  async signupAndLogin(signupDto: SignupRequestDto) {
    this.logger.log(`회원가입 시도: ${signupDto.email}`);

    try {
      const response = await this.http.post('/user-service/signup', signupDto);

      if (response.status !== 201) {
        this.logger.warn(
          `회원가입 실패 - 잘못된 응답 상태: ${response.status}, 이메일: ${signupDto.email}`,
        );
        throw new HttpException('회원가입 실패', HttpStatus.BAD_REQUEST);
      }

      this.logger.log(`회원가입 성공, 자동 로그인 시도: ${signupDto.email}`);

      const { accessToken, userId } = await this.login({
        email: signupDto.email,
        password: signupDto.pwd,
      });

      this.logger.log(`회원가입 + 자동로그인 완료: userId=${userId}`);
      return { accessToken, userId };
    } catch (error: any) {
      this.logger.error(`회원가입 실패: ${signupDto.email}`, error.stack);
      throw new HttpException('회원가입 실패', HttpStatus.BAD_REQUEST);
    }
  }

  async login(
    loginDto: LoginRequestDto,
  ): Promise<{ accessToken: string; userId: string }> {
    this.logger.log(`로그인 시도: ${loginDto.email}`);

    try {
      const response = await this.http.post('/user-service/login', loginDto, {
        validateStatus: () => true,
      });

      if (response.status !== 200) {
        this.logger.warn(
          `로그인 실패 - 잘못된 인증 정보: ${loginDto.email}, 상태코드: ${response.status}`,
        );
        throw new UnauthorizedException('Invalid credentials');
      }

      const accessToken = response.headers['token'];
      const userId = response.headers['userid'];

      if (!accessToken || !userId) {
        this.logger.error(
          `로그인 실패 - 토큰 또는 userId 누락: ${loginDto.email}`,
        );
        throw new UnauthorizedException(
          'Invalid login response from user-service',
        );
      }

      this.logger.log(`로그인 성공: userId=${userId}`);
      return { accessToken, userId };
    } catch (error) {
      this.logger.error(
        `로그인 실패: ${loginDto.email}`,
        error instanceof Error ? error.stack : String(error),
      );
      throw new UnauthorizedException('Login failed');
    }
  }
}
