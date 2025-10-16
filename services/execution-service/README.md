# Execution Service

## 📘 개요

Execution Service는 주식 거래 시스템에서 주문 체결(Execution) 을 담당하는 마이크로서비스입니다.<br>
Kafka를 통해 다른 서비스로부터 주문 이벤트와 시장 시세 이벤트를 수신하고,<br>
이를 기반으로 오더북(Order Book) 에 등록된 주문들의 체결을 처리합니다.
- Order Service → 주문 생성(OrderAccepted), 주문 취소(OrderCancelled)
- Market Data Service → 실시간 시세(MarketDataTick)

## 🚀 핵심 기능: 주문 체결

Execution Service는 두 가지 트리거를 기반으로 주문을 체결합니다.
1. 새로운 주문 유입(Order-vs-Order Matching)
2. 시장 시세 변동(Order-vs-Market Price Matching)

### 1. 주문 vs 주문 매칭 (Order-vs-Order Matching)
---

새로운 주문이 들어왔을 때, 오더북(미체결 주문 목록)에 있는 반대 주문과 조건을 비교하여 체결하는 가장 일반적인 방식입니다.

```
[Order Service]            [Execution Service]
      |                           |
      |--- (1) OrderAccepted --->|
      |      (Kafka Event)        |
      |                           |--- (2) OrderEventsConsumer 수신
      |                           |
      |                           |--- (3) ExecutionFacade.handleOrderAccepted 호출
      |                           |
      |                           |--- (4) 오더북에서 가장 유리한 반대 주문 조회
      |                           |
      |                           |--- (5) MatchingEngine.match 호출 (주문 vs 주문)
      |                           |
      |                           |--- (6) 체결 발생 시
      |                           |     - Fill(체결) 정보 생성
      |                           |     - Trade(거래) 정보 업데이트
      |                           |     - Outbox 테이블에 TradeExecuted 이벤트 저장
      |                           |
      |<-- (7) TradeExecuted ----|
      |      (Kafka Event)        |
      |                           |
      |                           |--- (8) 미체결 수량은 오더북에 저장/업데이트
```

💡 핵심 요약
- 새로운 주문이 들어오면 기존 반대 주문들과 즉시 매칭을 시도합니다.
- 매수호가 ≥ 매도호가일 경우 체결 발생
- 체결 가격은 기존 주문(Maker) 의 가격을 따름
- 양쪽 주문의 잔량(leavesQty)을 업데이트
- 시장가 주문의 미체결 잔량은 즉시 취소됩니다.

### 2. 주문 vs 시장가 매칭 (Order-vs-Market Price Matching)
---
실시간으로 변하는 시장의 최우선 호가(Tick)를 기준으로, 오더북에 있는 주문들이 체결될 수 있는지 확인하는 방식입니다. <br>
예를 들어, 100원에 매수하겠다는 주문이 오더북에 있는데 시장가 매도 호가가 100원 이하로 내려오면 체결시키는 식입니다.

```
[Market-Data Service]      [Execution Service]
      |                           |
      |---- (1) Market Tick ---->|
      |      (Kafka Event)        |
      |                           |--- (2) MarketDataTicksConsumer 수신
      |                           |
      |                           |--- (3) ExecutionFacade.processMarketDataTick 호출
      |                           |
      |                           |--- (4) 오더북에서 "모든" 주문 조회
      |                           |
      |                           |--- (5) MatchingEngine.processTick 호출 (주문 vs 시장가)
      |                           |
      |                           |--- (6) 체결 조건 만족하는 주문에 대해 체결 처리
      |                           |     - Fill(체결) 정보 생성
      |                           |     - Trade(거래) 정보 업데이트
      |                           |     - Outbox 테이블에 TradeExecuted 이벤트 저장
      |                           |
      |<-- (7) TradeExecuted ----|
             (Kafka Event)
```

💡 핵심 요약
- 시장 가격이 변하면, 기존 주문들을 시세 기준으로 재평가합니다.
- 체결 조건 만족 시 자동으로 Fill/Trade를 생성하고 Outbox에 저장합니다.
<br>

| 상황                                    | 결과        |
| ------------------------------------- | --------- |
| 매수 주문: 10,000원<br>시장 매도호가: 9,900원     | ❌ 체결되지 않음 |
| 매수 주문: 10,000원<br>시장 매도호가: 10,000원 이하 | ✅ 체결 발생   |


### 3. Outbox 기반 이벤트 발행
---
모든 체결 결과(TradeExecuted)는 Outbox 패턴을 통해 안전하게 발행됩니다.
```
[Execution Service]
     ↓
(1) 트랜잭션 내 Outbox 테이블에 이벤트 저장
     ↓
(2) OutboxRelayScheduler 주기 실행
     ↓
(3) ExecutionEventsProducer → Kafka (execution-events 토픽 발행)
```
💡 **Outbox 패턴의 장점**
- 트랜잭션과 이벤트 발행의 원자성(Atomicity) 보장
- Kafka 장애 시에도 이벤트 유실 없이 재처리 가능
- 서비스 간 데이터 일관성(Consistency) 유지


## 🏗️ 주요 컴포넌트

| 컴포넌트                        | 역할                                                               |
| --------------------------- | ---------------------------------------------------------------- |
| **ExecutionFacade**         | 서비스의 핵심 진입점. 주문/시세 이벤트를 오케스트레이션                                  |
| **MatchingEngine**          | 주문 매칭 로직 담당 (가격, 수량, 조건 비교 및 체결 생성)                              |
| **OrderEventsConsumer**     | `order-events` 토픽에서 주문 이벤트(`OrderAccepted`, `OrderCancelled`) 수신 |
| **MarketDataTicksConsumer** | `market-data-ticks` 토픽에서 실시간 시세(Tick) 이벤트 수신                     |
| **ExecutionRepository**     | 오더북, 체결 내역, 거래 기록 등의 데이터 관리                                      |
| **OutboxRelayScheduler**    | Outbox → Kafka 발행 담당 스케줄러                                        |

---
## 🧱 아키텍처 개요
- 헥사고날 아키텍처 (Ports & Adapters)
  + domain: 비즈니스 핵심 로직 (매칭 엔진, 체결 도메인)
  + application: 서비스 흐름 조율 및 트랜잭션 관리
  + infrastructure: Kafka, DB, Scheduler 등 외부 연동 계층
- Outbox 패턴
  + 트랜잭션 내 이벤트 저장
  + Scheduler를 통해 비동기로 Kafka에 발행

## 💾 기술 스택
| 항목         | 기술                                                                   |
| ---------- | -------------------------------------------------------------------- |
| 언어 / 프레임워크 | Java 21, Spring Boot 3.x                                               |
| 메시징        | Apache Kafka                                                         |
| 데이터베이스     | H2 (인메모리), MySQL                                   |
| ORM        | JPA / Hibernate                                                      |
