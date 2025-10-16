import {
  Injectable,
  HttpException,
  HttpStatus,
  UnauthorizedException,
} from '@nestjs/common';
import { HttpService } from '../common/http/http.service';
import { SignupRequestDto } from './dto/signup-request.dto';
import { LoginRequestDto } from './dto/login-request.dto';

@Injectable()
export class AuthService {
  constructor(private readonly http: HttpService) {}

  async signupAndLogin(signupDto: SignupRequestDto) {
    try {
      const response = await this.http.post('/user-service/signup', signupDto);

      if (response.status !== 201) {
        throw new HttpException('회원가입 실패', HttpStatus.BAD_REQUEST);
      }

      const { accessToken, userId } = await this.login({
        email: signupDto.email,
        password: signupDto.pwd,
      });

      return { accessToken, userId };
    } catch (error: any) {
      console.log(error);
      throw new HttpException('회원가입 실패', HttpStatus.BAD_REQUEST);
    }
  }

  async login(
    loginDto: LoginRequestDto,
  ): Promise<{ accessToken: string; userId: string }> {
    try {
      const response = await this.http.post('/user-service/login', loginDto, {
        validateStatus: () => true,
      });

      if (response.status !== 200) {
        throw new UnauthorizedException('Invalid credentials');
      }

      const accessToken = response.headers['token'];
      const userId = response.headers['userid'];

      if (!accessToken || !userId) {
        throw new UnauthorizedException(
          'Invalid login response from user-service',
        );
      }

      return { accessToken, userId };
    } catch (error) {
      throw new UnauthorizedException('Login failed');
    }
  }
}
