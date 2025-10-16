from fastapi import FastAPI
from config import API_PREFIX
from deps import lifespan
from routers import health, symbols, prices

app = FastAPI(lifespan=lifespan)

app.include_router(health.router, prefix=API_PREFIX)
app.include_router(symbols.router, prefix=API_PREFIX)
app.include_router(prices.router,  prefix=API_PREFIX)