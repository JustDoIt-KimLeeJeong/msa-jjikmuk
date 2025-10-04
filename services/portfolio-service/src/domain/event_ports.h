#pragma once
#include <time.h>


// API 제공 스키마
typedef struct {
    int available;
    int reserved;
} Balances;

typedef struct {
    char * symbol;
    int qty;
    int avg_price ;
    int realized_pnl;
} Position;

typedef struct {
    int user_id;
    Balances balances;
    Position *positions;
    size_t position_cnt;
} Portfolio_API;

// Kafka 제공 스키마
typedef struct {
    int eventId;
    time_t occurredAt;
    char * orderId;
    int userID;
    char * symbol;
    int qty;

} OrderReserved;

typedef struct {
    int eventId;
    time_t occurredAt;
    char * orderId;
    int userID;
    char *reason_code;

} OrderRejected;

typedef struct {
    int eventId;
    time_t occurredAt;
    char * orderId;
    int userID;
} SellReserved;


typedef struct {
    int eventId;
    time_t occurredAt;
    char * orderId;
    int userID;
    char *reason_code;
    int qty;

} sellReject;

// Kafka 제공 스키마, fail event 들 

typedef struct {
    int userID;
    char * symbol;
    int qty;
    char * reason_code;

} PositionUpdateFailed;


typedef struct {
    int userID;
    char * symbol;
    char * reason_code;

} PriceSyncDegraded;


typedef struct {
    int userID;
    char * reason_code;

} ValuationRecalcFailed;


typedef struct {
    int userID;
    char * reason_code;
} IdempotencyConflict;


typedef struct {
    int eventId;
    time_t occurredAt;
    char * orderId;
    int userID;

} ExecutionFail;



// redis 제공 스키마 
