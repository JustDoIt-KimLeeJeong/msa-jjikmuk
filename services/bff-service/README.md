# BFF Service (Backend For Frontend)

MSA 찍먹 프로젝트의 BFF(Backend For Frontend) 서비스입니다. 프론트엔드와 백엔드 마이크로서비스들 사이의 중간 계층 역할을 담당합니다.

## 🚀 기술 스택

- **Language:** TypeScript
- **Runtime:** Node.js 20 LTS
- **Framework:** NestJS 10.x (+ `@nestjs/platform-fastify`)
- **패키지 매니저:** pnpm

## 📋 주요 기능

- 클라이언트 요청의 라우팅 및 집계
- 여러 마이크로서비스 API 통합
- 인증 및 권한 관리 (쿠키 기반 세션)
- 실시간 데이터 스트리밍 (SSE)
- 응답 데이터 변환 및 최적화

## 🏃‍♂️ 실행 방법

### 개발 환경

```bash
PORT=8080 pnpm install && pnpm start:dev
```

### 운영 환경

```bash
pnpm install --frozen-lockfile
pnpm build
NODE_ENV=production PORT=8080 pnpm start:prod
```

## 📁 프로젝트 구조

```
src/
├── main.ts                        # 애플리케이션 진입점
├── app.module.ts                   # 루트 모듈
├── config/                         # 환경변수/설정
│   ├── config.module.ts
│   └── config.service.ts
├── common/                         # 공통 유틸리티, 가드, 인터셉터
│   ├── guards/
│   │   └── auth.guard.ts          # 쿠키 access_token 검증
│   ├── interceptors/
│   │   ├── timeout.interceptor.ts
│   │   ├── cache.interceptor.ts
│   │   └── correlation.interceptor.ts
│   ├── filters/
│   │   └── http-exception.filter.ts
│   ├── dto/
│   │   └── pagination.dto.ts
│   ├── types/
│   │   └── index.ts
│   └── utils/
│       └── sse.ts                 # SSE 공통 유틸
├── http/                          # 내부 마이크로서비스 호출
│   ├── http.module.ts
│   ├── http.service.ts            # 공통 HTTP 클라이언트
│   └── clients/
│       ├── orders.client.ts       # ORDER 서비스 API
│       ├── portfolio.client.ts    # PORTFOLIO 서비스 API
│       ├── market.client.ts       # MARKET-DATA 서비스 API
│       ├── notifications.client.ts # 알림 서비스 API
│       └── user.client.ts         # USER 서비스 API
├── auth/                          # 인증 관리
│   ├── auth.controller.ts         # 회원가입/로그인/로그아웃
│   ├── auth.service.ts
│   └── auth.module.ts
├── notifications/                 # 알림 SSE 프록시
│   ├── notifications.controller.ts
│   ├── notifications.service.ts
│   └── notifications.module.ts
├── orders/                        # 주문 관리
│   ├── orders.controller.ts       # 주문 생성/조회/취소
│   ├── orders.service.ts
│   ├── dto/
│   │   ├── create-order.dto.ts
│   │   └── list-orders.dto.ts
│   └── orders.module.ts
├── market/                        # 시장 데이터 + 실시간 스트림
│   ├── symbols.controller.ts      # 심볼 목록
│   ├── prices.controller.ts       # 가격 조회
│   ├── prices.stream.controller.ts # 가격 실시간 스트림
│   ├── quotes.controller.ts       # 호가 조회
│   ├── quotes.stream.controller.ts # 호가 실시간 스트림
│   ├── candles.controller.ts      # 캔들 조회
│   ├── candles.stream.controller.ts # 캔들 실시간 스트림
│   ├── market.service.ts
│   └── market.module.ts
└── portfolio/                     # 포트폴리오 관리
    ├── me.controller.ts           # 내 정보 조회
    ├── portfolio.service.ts
    └── portfolio.module.ts
```

## 🌐 API 엔드포인트

### 인증 API

- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃

### 사용자 정보 API

- `GET /api/me` - 내 계정 정보 조회

### 주문 관리 API

- `POST /api/orders` - 주문 생성
- `GET /api/orders` - 주문 목록 조회
- `GET /api/orders/{orderId}` - 특정 주문 조회
- `DELETE /api/orders/{orderId}` - 주문 취소

### 시장 데이터 API

- `GET /api/symbols` - 거래 가능한 심볼 목록
- `GET /api/prices` - 전체 가격 정보
- `GET /api/prices/{symbol}` - 특정 심볼 가격
- `GET /api/quotes/{symbol}` - 특정 심볼 호가
- `GET /api/candles/{symbol}` - 특정 심볼 캔들 데이터

### 실시간 스트림 API (SSE)

- `GET /api/notifications/stream` - 알림 실시간 스트림
- `GET /api/prices/stream` - 가격 실시간 스트림
- `GET /api/quotes/stream` - 호가 실시간 스트림
- `GET /api/candles/stream` - 캔들 실시간 스트림

## 🔗 연결되는 마이크로서비스

- **User Service** - 사용자 관리 및 인증
- **Order Service** - 주문 처리
- **Portfolio Service** - 포트폴리오 관리
- **Market Data Service** - 시장 데이터 제공
- **Notification Service** - 알림 서비스
- **Execution Service** - 주문 실행

## 🔧 스크립트

| 명령어             | 설명                         |
| ------------------ | ---------------------------- |
| `pnpm start`       | 애플리케이션 실행            |
| `pnpm start:dev`   | 개발 모드로 실행 (핫 리로드) |
| `pnpm start:debug` | 디버그 모드로 실행           |
| `pnpm start:prod`  | 프로덕션 모드로 실행         |
| `pnpm build`       | 애플리케이션 빌드            |
| `pnpm test`        | 단위 테스트 실행             |
| `pnpm test:e2e`    | E2E 테스트 실행              |
| `pnpm test:cov`    | 테스트 커버리지 실행         |
| `pnpm lint`        | 코드 린팅                    |
| `pnpm format`      | 코드 포맷팅                  |

## 🔒 보안 및 인증

- **JWT 기반 인증**: 쿠키를 통한 access_token 관리
- **CORS 설정**: 허용된 오리진만 접근 가능
- **Rate Limiting**: API 요청 빈도 제한
- **Input Validation**: 모든 입력 데이터 검증
- **Correlation ID**: 요청 추적을 위한 고유 ID 전파

## 📊 모니터링 및 로깅

- **Health Check**: `/health` - 서비스 상태 확인
- **Metrics**: `/metrics` - 애플리케이션 메트릭
- **Structured Logging**: 구조화된 로그 출력
- **Error Tracking**: 에러 추적 및 알림

## 🚀 성능 최적화

- **HTTP Keep-Alive**: 연결 재사용으로 성능 향상
- **Response Caching**: 자주 요청되는 데이터 캐싱
- **Connection Pooling**: 데이터베이스 연결 풀링
- **Circuit Breaker**: 서비스 장애 전파 방지
- **Timeout Management**: 적절한 타임아웃 설정

## 🧪 테스트

```bash
# 단위 테스트
pnpm test

# E2E 테스트
pnpm test:e2e

# 테스트 커버리지
pnpm test:cov
```

## 📝 개발 가이드라인

1. **코드 스타일**: ESLint + Prettier 사용
2. **커밋 메시지**: Conventional Commits 규칙 준수
3. **타입 안정성**: TypeScript strict 모드 사용
4. **테스트**: 단위 테스트 및 E2E 테스트 작성 필수
5. **모듈화**: 도메인별 모듈 분리 및 의존성 주입 활용

## 🔄 실시간 데이터 처리

BFF 서비스는 Server-Sent Events(SSE)를 통해 실시간 데이터를 제공합니다:

- **가격 스트림**: 실시간 가격 변동 정보
- **호가 스트림**: 실시간 매수/매도 호가 정보
- **캔들 스트림**: 실시간 차트 데이터
- **알림 스트림**: 사용자별 개인화된 알림

각 스트림은 하트비트(10-15초)를 통해 연결 상태를 유지하며, 클라이언트 연결 해제 시 자동으로 리소스를 정리합니다.

---

💡 **참고**: 이 서비스는 MSA 아키텍처의 일부로, 다른 마이크로서비스들과 함께 동작합니다.
