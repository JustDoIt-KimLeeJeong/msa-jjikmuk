import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { AppModule } from './app.module';
import * as cookie from '@fastify/cookie';
import { ValidationPipe } from '@nestjs/common';

async function bootstrap() {
  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter(),
  );

  // Fastify 쿠키 플러그인 등록
  await app.register(cookie);

  // 유효성 검사 파이프 등록
  app.useGlobalPipes(new ValidationPipe({ whitelist: true }));

  const port = process.env.PORT || 8080;

  await app.listen(port, '0.0.0.0');
  console.log(`BFF Service is running on port ${port}`);
}

bootstrap();
