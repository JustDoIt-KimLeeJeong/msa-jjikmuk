#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "postgre_rep.h"


PG_ENV * load_from_env(void){
    PG_ENV * env = malloc(sizeof(PG_ENV));
    if(!env){
        fprintf(stderr, "ENV Memory allocation fail");
        return NULL;
    }
    // 환경변수 로딩
    char * PG_HOST = getenv("PG_HOST");
    char * PG_PORT = getenv("PG_PORT");
    char * PG_DB_NAME = getenv("PG_DB_NAME");
    char * PG_CONNECTION_TIMEOUT = getenv("PG_CONNECTION_TIMEOUT");
    char * PG_PASSWORD = getenv("PG_PASSWORD");
    char * PG_USER = getenv("PG_USER");
    
    if (!PG_HOST || !PG_PORT || !PG_DB_NAME || !PG_USER || !PG_PASSWORD) {
        fprintf(stderr, "ERROR: Required environment variables not set!\n");
        return NULL;
    }
    

    // 디버깅 출력

    printf("PG_HOST: %s\n", PG_HOST ? PG_HOST : "NOT SET");
    printf("PG_PORT: %s\n", PG_PORT ? PG_PORT : "NOT SET");
    printf("PG_DB_NAME: %s\n", PG_DB_NAME ? PG_DB_NAME : "NOT SET");
    printf("PG_USER: %s\n", PG_USER ? PG_USER : "NOT SET");
    printf("PG_PASSWORD: %s\n", PG_PASSWORD ? "****" : "NOT SET");
    printf("PG_CONNECTION_TIMEOUT: %s\n", PG_CONNECTION_TIMEOUT ? PG_CONNECTION_TIMEOUT : "NOT SET");
    
    // 환경변수 체크
    
    strncpy(env->PG_HOST, PG_HOST, sizeof(env->PG_HOST)-1);
    strncpy(env->PG_PORT, PG_PORT, sizeof(env->PG_PORT)-1);
    strncpy(env->PG_DB_NAME, PG_DB_NAME, sizeof(env->PG_DB_NAME)-1);
    strncpy(env->PG_PASSWORD, PG_PASSWORD, sizeof(env->PG_PASSWORD)-1);
    strncpy(env->PG_USER, PG_USER, sizeof(env->PG_USER)-1);
    strncpy(env->PG_CONNECTION_TIMEOUT, PG_CONNECTION_TIMEOUT, sizeof(env->PG_CONNECTION_TIMEOUT)-1);
    
    printf("2");
    
    
    env->PG_HOST[sizeof(env->PG_HOST)-1] = '\0';
    env->PG_PORT[sizeof(env->PG_PORT)-1] = '\0';
    env->PG_DB_NAME[sizeof(env->PG_DB_NAME)-1] = '\0';
    env->PG_PASSWORD[sizeof(env->PG_PASSWORD)-1] = '\0';
    env->PG_USER[sizeof(env->PG_USER)-1] = '\0';
    env->PG_CONNECTION_TIMEOUT[sizeof(env->PG_CONNECTION_TIMEOUT)-1] = '\0';

    return env;
    
}

void free_env(PG_ENV * env){
    if(env){
        free(env);
    }
}

int connection(){
    char buffer[1024];
    PG_ENV * ENV = NULL; 
    ENV = load_from_env();
    

    snprintf(buffer, sizeof(buffer), "host=%s port=%s dbname=%s user=%s password=%s", ENV->PG_HOST, ENV->PG_PORT, ENV->PG_DB_NAME, ENV->PG_USER, ENV->PG_PASSWORD);
    printf("%s\n",buffer);
    // PGres 연결
    // PGconn *conn = PQconnectStart(buffer);
    PGconn *conn = PQconnectdb(buffer);


    if (PQstatus(conn) != CONNECTION_OK){
        printf("Error while connection to Postgre %s\n", PQerrorMessage(conn));
        PQfinish(conn);
        exit(1);
    }
    
    printf("Connection Established\n");
    printf("Port: %s\n", PQport(conn));
    printf("Host: %s\n", PQhost(conn));
    printf("DBName: %s\n", PQdb(conn));




    //


    //PG 연결 
    PQfinish(conn);

    return 0;

}


// 테스트용 main 문. 추후 삭제 예정

int main(){
    connection();
}

