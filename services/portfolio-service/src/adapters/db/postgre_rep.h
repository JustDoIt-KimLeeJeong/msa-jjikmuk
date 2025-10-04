#include <libpq-fe.h>

typedef struct {
    char PG_HOST[256];              
    char PG_PORT[16]; 
    char PG_DB_NAME[256]; 
    char PG_CONNECTION_TIMEOUT[16]; 
    char PG_PASSWORD[256];
    char PG_USER[256];
} PG_ENV;



PG_ENV * load_from_env(void);
int connection(void);