# Execution Service

## 개요

주문 체결을 담당하는 마이크로서비스입니다. 다른 서비스로부터 신규 주문이나 주문 취소 요청을 받아 처리하며, 실시간 시장 가격에 따라 미체결된 주문들의 체결을 시도합니다.

## 핵심 기능: 주문 체결

Execution Service는 **새로운 주문 유입**과 **실시간 시장가 변동**이라는 두 가지 핵심 트리거를 기반으로 주문을 체결합니다.

### 1. 주문 vs 주문 매칭 (Order-vs-Order Matching)

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

- **핵심:** **새로운 주문이** 들어오면 **기존 주문들**과 매칭을 시도합니다.

### 2. 주문 vs 시장가 매칭 (Order-vs-Market Price Matching)

실시간으로 변하는 시장의 최우선 호가(Tick)를 기준으로, 오더북에 있는 주문들이 체결될 수 있는지 확인하는 방식입니다. 예를 들어, 100원에 매수하겠다는 주문이 오더북에 있는데 시장가 매도 호가가 100원 이하로 내려오면 체결시키는 식입니다.

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

- **핵심:** **시장 가격이** 변하면 **기존 주문들**과 매칭을 시도합니다.

## 주요 컴포넌트

-   `ExecutionFacade`: 서비스의 핵심 로직으로 들어오는 진입점(Entrypoint) 역할을 합니다.
-   `MatchingEngine`: 실제 주문 매칭 로직을 담고 있는 도메인 서비스입니다.
-   `OrderEventsConsumer`: `order-service`로부터 주문 관련 이벤트를 수신하는 Kafka 컨슈머입니다.
-   `MarketDataTicksConsumer`: `market-data-service`로부터 시장가(Tick) 이벤트를 수신하는 Kafka 컨슈머입니다.
-   `ExecutionRepository`: 오더북, 거래 내역 등 체결 관련 데이터를 관리하는 리포지토리 인터페이스입니다.
-   `OutboxRelayScheduler`: Outbox 패턴을 통해 도메인 이벤트(`TradeExecuted` 등)를 안정적으로 발행하는 스케줄러입니다.
