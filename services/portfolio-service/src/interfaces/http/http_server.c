#include <stdio.h>
#include "http_router.h"
#include <microhttpd.h>
#include <time.h>
#include <errno.h>

#define PORT 8888

// 전역 변수
static struct MHD_Daemon *daemon = NULL;
static int restart_count = 0;
static time_t last_restart = 0;

void check_running(){
    
}


struct MHD_Daemon * start_server(){

    daemon = MHD_start_daemon(MHD_USE_INTERNAL_POLLING_THREAD | MHD_USE_ERROR_LOG, PORT, NULL, NULL, 
                             &answer_to_connection, NULL, MHD_OPTION_END); 
    if(NULL == daemon){
        printf("Failed to start server on port %d: %s\n", PORT, strerror(errno));
        return NULL;
    }
    
    print("server successfully started on port %d", PORT);
    return daemon;

}

int run_server(void){

    const int MAX_RESTARTS = 10;          
    const int RESTART_COOLDOWN = 5;
    int restart_cnt = 0;

    while (restart_count < MAX_RESTARTS) {

        // 너무 빠른 재시작 방지
        time_t now = time(NULL);
        if (restart_cnt > 0 && (now - last_restart) < RESTART_COOLDOWN) {
            printf("Waiting %d seconds before restart...\n", RESTART_COOLDOWN);
            sleep(RESTART_COOLDOWN);
        }
        
        // 서버 시작
        daemon = start_server(PORT);
        if (daemon == NULL) {
            restart_cnt++;
            last_restart = time(NULL);
            printf("Server start failed. Retrying... (%d/%d)\n", restart_cnt, MAX_RESTARTS);
            continue;
        }
        
        printf("Server running. Press Ctrl+C to stop.\n");
        restart_cnt = 0; // 성공하면 카운터 리셋

        if (restart_cnt >= MAX_RESTARTS) {
            printf("Maximum restart attempts reached. Exiting.\n");
            return 1;
        }
        
        printf("서버 중지.\n");

        return 0;

    }

    
    
}



// int main(){
    

//     router_register(); // 라우터 등록 후 시작
//     run_server();
    
// }
