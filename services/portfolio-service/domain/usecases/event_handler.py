# from app.db.engine import session
import adapters.outbound.db.sqlalchemy_query as query
from sqlalchemy.orm import Session
import domain.model.event_dto as DTO

async def trade_executed(session : Session, payload : dict) : 
    # 체결 성공 시 있던 order를 검색해서 다시 원복함. 
    # 로직 : 
    # 1. orderId를 확인 (이거 input시에 값이 ㅇㅆ는지 확인 필요)
    # 2. 얼마가 체결되었는지 확인. 실제 팔린 qty랑 실제로 처리된 비용을 확인 
    # 3. DB에 반영하고, 처리 완료를 발행함
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
            DTO.Order(
                payload.event_id,
                payload.order_id,
                payload.user_id,
                payload.symbol,
                payload.qty
            )  
            # 여기서 serilizer 불러야 함.
            


    elif payload.side == "sell": 
        query.trade_execution_buy(session ,  payload.user_id,  payload.symbol, payload.price, payload.qty)
        # 산거 반영 완료 퍼블리시 필요

        
    else : 
        # 이것도 payload가 잘못된거라서 그냥 넘어가면 안되는거임. 이것도 예외처리 동일하게 이번트 발행하줘야 함. DB 롤백 필요.
        pass
    
    pass


async def order_cancelled(session : Session, payload : dict) :

    # 원상복귀 로직 필요함. 
    query.cancel_order(payload.user_id , payload.order_id,session) 


async def order_palced(session : Session, payload : dict) :
    # 생각해보니까 비용도 같이 줘야하네.
    query.update_order(payload.user_id, payload.order_id , payload.symbol, payload.qty, payload.price, payload.side,payload.reserved_balance, payload.reserved_qty, session :Session) :
    


# 테스트 코드 
async def test() : 
    print("yeap, the usecase did worked well. good for you.")


