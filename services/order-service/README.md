# Order Service

주식 모의매매 시스템의 주문 관리 서비스

## 📋 목차
- [개요](#개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [API 명세](#api-명세)
- [이벤트 명세](#이벤트-명세)
- [데이터베이스 설계](#데이터베이스-설계)
- [테스트](#테스트)
- [배포](#배포)
- [모니터링](#모니터링)

---

## 개요

Order Service는 주식 모의매매 시스템에서 **주문 생명주기 관리**를 담당하는 마이크로서비스입니다.

### 핵심 책임
- 주문 접수 및 상태 관리 (PENDING → ACCEPTED → FILLED/PARTIALLY_FILLED/EXPIRED/CANCELLED)
- 체결 이력 관리 (Order_Trades 테이블)
- 시간 기반 자동 처리 (예약 주문 활성화, 지정가 만료)
- 이벤트 발행 (OrderPlaced, OrderCancelled, OrderExpired)

### 아키텍처 패턴
- **Outbox Pattern + CDC**: Debezium CDC를 통한 트랜잭셔널 이벤트 발행
- **SAGA Choreography**: Portfolio, Execution과 이벤트 기반 협업
- **Eventually Consistent**: 분산 환경에서의 최종 일관성 보장

---

## 주요 기능

### 1. 주문 유형별 처리

#### 시장가 주문 (MARKET)
- **장시간 내**: 즉시 체결 시도 → FILLED/REJECTED
- **장시간 외**: RESERVED → 장 개장시 PENDING → 즉시 체결

#### 지정가 주문 (LIMIT)
- **장시간 내**: PENDING → ACCEPTED → 24시간 체결 대기
- **장시간 외**: RESERVED → 장 개장시 PENDING → 24시간 만료
- **만료 처리**: expires_at = 생성시점 + 24시간

### 2. 체결 이력 관리
- Order_Trades 테이블로 부분 체결 상세 이력 저장
- 평균 체결가 자동 계산
- TradeExecuted 이벤트 집계 데이터 제공

### 3. 시간 기반 자동 처리
- **예약 주문 활성화**: 매일 09:00, RESERVED → PENDING
- **지정가 만료**: 5분마다, expires_at 경과 주문 EXPIRED 처리

---

## 기술 스택

| 카테고리 | 기술                                   |
|---------|--------------------------------------|
| Language | Java 21                              |
| Framework | Spring Boot 3.5.6                    |
| Database | PostgreSQL 16                        |
| ORM | Spring Data JPA, Hibernate           |
| Messaging | Confluent Kafka 7.8.0                |
| CDC | Debezium                             |
| Monitoring | Prometheus v3.0.1, Grafana 11.4.0, Spring Actuator |
| Testing | JUnit 5, MockMvc, H2                 |
| Build Tool | Gradle 8.x                           |

---

## 프로젝트 구조

```
.
├── build.gradle
├── docker-compose.yml
├── gradlew
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── tradingsystem
    │   │           └── order
    │   │               ├── OrderServiceApplication.java
    │   │               ├── config
    │   │               │   └── JpaConfig.java
    │   │               ├── controller
    │   │               │   └── HealthController.java
    │   │               ├── domain
    │   │               │   ├── BaseTimeEntity.java
    │   │               │   ├── Order.java
    │   │               │   ├── OrderSide.java
    │   │               │   ├── OrderStatus.java
    │   │               │   ├── OrderTrade.java
    │   │               │   ├── OrderType.java
    │   │               │   └── OutboxEvent.java
    │   │               └── repository
    │   │                   ├── OrderRepository.java
    │   │                   ├── OrderTradeRepository.java
    │   │                   └── OutboxEventRepository.java
    │   └── resources
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── application.yml
    └── test
        └── java
```

---

## 시작하기

### 필수 요구사항
- Java 21 이상
- Docker & Docker Compose
- Gradle 8.x

### 로컬 개발 환경 설정

#### 1. 환경 변수 설정
```bash
cp .env.example .env
# .env 파일 편집하여 필요한 환경 변수 설정
```
#### 2. Docker Compose로 인프라 실행
```bash
docker-compose up -d postgres kafka
```
#### 3. 애플리케이션 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
#### 4. Health Check 확인
```bash
curl http://localhost:8100/health
```
   예상 응답:
```json
{
   "status": "ok"
}
```

---

## API 명세

### Health Check

#### `GET /health`
- **설명**: 서비스의 상태를 확인합니다.
- **응답**:
    - `200 OK`
    ```json
    {
        "status": "ok"
    }
    ```

---

## 이벤트 명세

(추가 예정)

---

## 데이터베이스 설계

### `orders`
- 주문의 핵심 정보를 저장합니다.
- **주요 컬럼**: `id`, `userId`, `clientOrderId`, `symbol`, `side`, `type`, `price`, `quantity`, `filledQuantity`, `status`, `expiresAt`, `createdAt`, `updatedAt`

### `order_trades`
- 주문의 체결 내역을 저장합니다.
- **주요 컬럼**: `id`, `orderId`, `tradeId`, `symbol`, `executedPrice`, `executedQuantity`, `side`, `createdAt`

### `outbox_events`
- Outbox Pattern을 위해 발행할 이벤트를 저장합니다.
- **주요 컬럼**: `id`, `eventId`, `eventType`, `aggregateId`, `payload`, `published`, `publishedAt`, `createdAt`

---

## 테스트

(추가 예정)

---

## 배포

(추가 예정)

---

## 모니터링

(추가 예정)