import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { AppModule } from './app.module';
import cookie from '@fastify/cookie';
import { ValidationPipe } from '@nestjs/common';
import { WinstonModule } from 'nest-winston';
import * as winston from 'winston';

async function bootstrap() {
  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter(),
    {
      logger: WinstonModule.createLogger({
        format: winston.format.combine(
          winston.format.timestamp(),
          winston.format.json(),
        ),
        transports: [
          new winston.transports.Console({
            format: winston.format.combine(
              winston.format.colorize(),
              winston.format.printf(
                ({ timestamp, level, message, context, ...meta }) => {
                  return `${timestamp} [${context || 'Application'}] ${level}: ${message} ${
                    Object.keys(meta).length ? JSON.stringify(meta) : ''
                  }`;
                },
              ),
            ),
          }),
          new winston.transports.File({
            filename: 'logs/error.log',
            level: 'error',
          }),
          new winston.transports.File({
            filename: 'logs/application.log',
          }),
        ],
      }),
    },
  );

  // Fastify 쿠키 플러그인 등록
  await app.register(cookie as any);

  // 유효성 검사 파이프 등록
  app.useGlobalPipes(new ValidationPipe({ whitelist: true }));

  const port = process.env.PORT || 8080;

  await app.listen(port, '0.0.0.0');
  console.log(`BFF Service is running on port ${port}`);
}

bootstrap();
