# from app.db.engine import session
import adapters.outbound.db.sqlalchemy_query as query
from sqlalchemy.orm import Session

async def trade_executed(session : Session, event_type : str, payload : dict, headers : dict) : 
    # 체결 성공 시 있던 order를 검색해서 다시 원복함. 
    # 로직 : 
    # 1. orderId를 확인 (이거 input시에 값이 ㅇㅆ는지 확인 필요)
    # 2. 얼마가 체결되었는지 확인. 실제 팔린 qty랑 실제로 처리된 비용을 확인 
    # 3. DB에 반영하고, 처리 완료를 발행함
    try : 
        order = query.search_order(payload.user_id, payload.order_id, payload.symbol, payload.side)
        if not order : 
            #예외 처리
            print(f"no order id {payload.order_id}")
            # 여기서 카프카 예외 이벤트 발행해줘야 함. db 롤백 해달라고 요청 해야 함, 로그 그라파나랑 로키에 띄워줘야 함.
        if  payload.side == "buy" : 
            # 산거니까 기존 order 확인해서 값을 반영 
            try :
                query.trade_execution_buy(session, payload.user_id, payload.symbol, payload.price, payload.qty)
            # 샀는거 반영 완료 퍼블리시 해줌 
            except :
                # 반영 에러 
                pass
            finally : 
                query.outbox_event_save(payload.user_id, event_type, payload, headers, session)
                # 여기서 publish로 정상 반영 되었음을 공지해줘야 함.             

        elif payload.side == "sell": 
            query.trade_execution_buy(session ,  payload.user_id,  payload.symbol, payload.price, payload.qty)
            # 산거 반영 완료 퍼블리시 필요
            
        else : 
            # 이것도 payload가 잘못된거라서 그냥 넘어가면 안되는거임. 이것도 예외처리 동일하게 이번트 발행하줘야 함. DB 롤백 필요.
            pass
        
        pass
    except : 
        pass
    finally : 
        query.outbox_event_save(payload.user_id, event_type, payload, headers, session)


async def order_cancelled(session : Session, event_type : str, payload : dict, headers : dict) :

    # 원상복귀 로직 필요함. 
    try : 
        query.cancel_order(payload.user_id , payload.order_id,session) 
        
    except :
        print("order cancell except error") 
        pass

    finally : 
        query.outbox_event_save(payload.user_id, event_type, payload, headers, session)
    


async def order_placed(session : Session, event_type : str, payload : dict, headers : dict) :
    # 생각해보니까 비용도 같이 줘야하네.
    print("strart order placed.")
    print(event_type, payload, headers)
    try : 
        # print(f"payload user id is {payload.user_id}")
        await query.update_order(payload.user_id, payload.order_id , payload.symbol, payload.qty, payload.price, payload.side, payload.reserved_balance, payload.reserved_qty, session)
        print("saved Order, now on saving outbox")
        await query.outbox_event_save(payload.user_id, event_type, payload.model_dump(), headers, session)
    except Exception as e:
        print(f"order_placed except error : {e}") 


# 테스트 코드 
async def test(payload : dict) : 
    print("yeap, the usecase did worked well. good for you.")


