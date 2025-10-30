import { Injectable, Logger } from '@nestjs/common';
import { FastifyReply } from 'fastify';
import {
  NotificationEventType,
  NotificationData,
} from './dto/notification.dto';

@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name);

  /**
   * SSE 스트림 시작
   */
  startSseStream(reply: FastifyReply): void {
    this.logger.log('SSE 스트림 시작');

    // SSE 헤더 설정
    reply.raw.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
    });

    // 하트비트 전송 (15초 간격)
    const heartbeatInterval = setInterval(() => {
      reply.raw.write(':hb\n\n');
      this.logger.debug('하트비트 전송');
    }, 15000);

    // 연결 종료 감지
    reply.raw.on('close', () => {
      clearInterval(heartbeatInterval);
      this.logger.log('클라이언트 연결 종료');
    });

    // 데모: 5초 후 첫 번째 이벤트 전송
    setTimeout(() => {
      this.sendEvent(reply, NotificationEventType.TRADE_EXECUTED, {
        title: '주문 체결',
        body: '005930.KS 10주 @ 74,000원 체결',
        createdAt: new Date().toISOString(),
      });
    }, 5000);

    // 데모: 10초 후 두 번째 이벤트 전송
    setTimeout(() => {
      this.sendEvent(reply, NotificationEventType.ORDER_CANCELLED, {
        title: '주문 취소',
        body: '005930.KS 주문이 취소되었습니다.',
        createdAt: new Date().toISOString(),
      });
    }, 10000);

    // 데모: 15초 후 세 번째 이벤트 전송
    setTimeout(() => {
      this.sendEvent(reply, NotificationEventType.ORDER_REJECTED, {
        title: '주문 거절',
        body: '005930.KS 주문이 거절되었습니다.(증거금 부족)',
        createdAt: new Date().toISOString(),
      });
    }, 15000);
  }

  /**
   * SSE 이벤트 전송
   */
  private sendEvent(
    reply: FastifyReply,
    eventType: NotificationEventType,
    data: NotificationData,
  ): void {
    const event = `event: ${eventType}\ndata: ${JSON.stringify(data)}\n\n`;
    reply.raw.write(event);
    this.logger.log(`이벤트 전송: ${eventType}`);
  }
}
