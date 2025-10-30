import { Controller, Get, Res, Logger } from '@nestjs/common';
import { FastifyReply } from 'fastify';
import { NotificationsService } from './notifications.service';

@Controller('/api/notifications')
export class NotificationsController {
  private readonly logger = new Logger(NotificationsController.name);

  constructor(private readonly notificationsService: NotificationsService) {}

  @Get('stream')
  streamNotifications(@Res({ passthrough: false }) reply: FastifyReply): void {
    this.logger.log('GET /api/notifications/stream 요청 수신');

    try {
      this.notificationsService.startSseStream(reply);
    } catch (error) {
      this.logger.error(
        'SSE 스트림 시작 실패',
        error instanceof Error ? error.stack : String(error),
      );
      reply.status(500).send({ error: 'Internal Server Error' });
    }
  }
}
