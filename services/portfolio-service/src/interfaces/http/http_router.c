#include <microhttpd.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "http_router.h"


#define ROUTE_MAX 128
static int route_count = 0;
// 라우트 전역 변수 설정 
static RouteEntry routes[ROUTE_MAX];
HttpHandler httphandler;

// router전역 변수에 등록.
int router_add(const RouteEntry *r){
    if(route_count <= ROUTE_MAX){
        routes[route_count] = *r;
        route_count++;
        return 1;
    }else{
        return 0;
    }
}

// router 관리 함수. 
int router_register(){
    char *prefix = "/api/v1";
    router_add("GET", strcat(prefix, "/portfolio"), httphandler);
    return 0;
}

int http_response(struct MHD_Connection *conn, const char *message, const int resp_code){
    
    // const char* error_msg = "{ \"error\": \"url not_found\" }";
    enum MHD_Result result;
    struct MHD_Response *response;

    response = MHD_create_response_from_buffer(
        strlen(message), 
        (void*) message, 
        MHD_RESPMEM_PERSISTENT
    );

    MHD_add_response_header(response, "Content-Type", "application/json");
    // MHD_add_response_header(response, "Access-Control-Allow-Origin", "*");
    
    result = MHD_queue_response(conn, resp_code, response);
    
    //메모리 정리 차원에서 response 삭제 
    MHD_destroy_response(response);

    return result;
}


enum MHD_Result answer_to_connection (void *cls, struct MHD_Connection *connection, 
                                const char *url, 
                                const char *method, const char *version, 
                                const char *upload_data, 
                                size_t *upload_data_size, void **req_cls){
    // 메서드가 GET인지를 확인하고, 데이터베이스에서 가져오고, 이걸 json화 해서 MHD_NO를 반환함.
    char * message = "";

    for(int i=0; i<route_count; i++){
        if(
            strcmp(routes[i].method, method) == 0 &&
            strcmp(routes[i].pattern, url) == 0
        ){
            // message = "{}";
            // http_response(connection, message, MHD_HTTP_OK);
            return routes[i].httphandler(connection, url, req_cls);
        }
    }

    // url 이 없어서 404가 뜨는 경우
    message = "{ \"error\": \"not_found\", \"message\": \"The requested resource was not found\" }";
    return http_response(connection, message, MHD_HTTP_NOT_FOUND);

}
