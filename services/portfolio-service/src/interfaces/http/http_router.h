#pragma once
#include <microhttpd.h>

typedef int (*HttpHandler)(struct MHD_connection* c,
                            const char* path,
                            void* req_ctx);

typedef struct {
    const char* method;
    const char* pattern;
    HttpHandler httphandler;
} RouteEntry;

// router 사용 함수 
int router_add(const RouteEntry* r);
int router_register(void);
int send_404(struct MHD_Connection *conn);
enum MHD_Result answer_to_connection (void *cls, struct MHD_Connection *connection, 
                                const char *url, 
                                const char *method, const char *version, 
                                const char *upload_data, 
                                size_t *upload_data_size, void **req_cls);