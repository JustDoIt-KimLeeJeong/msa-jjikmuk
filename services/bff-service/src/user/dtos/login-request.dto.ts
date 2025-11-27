import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

export class LoginRequestDto {
  @IsNotEmpty()
  @IsEmail()
  @MinLength(2)
  email: string;

  @IsNotEmpty()
  @IsString()
  @MinLength(8)
  password: string;
}