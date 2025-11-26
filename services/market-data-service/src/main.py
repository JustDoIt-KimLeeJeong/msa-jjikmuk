from fastapi import FastAPI
from config import Settings
from deps import lifespan
from routers import health, symbols, prices, candles, dev_kis
 
app =FastAPI(lifespan=lifespan)
API_PREFIX = "/api"
settings = Settings()

app.include_router(health.router, prefix=API_PREFIX)
app.include_router(symbols.router, prefix=API_PREFIX)
app.include_router(prices.router, prefix=API_PREFIX)
# app.include_router(quotes.router, prefix=API_PREFIX)
app.include_router(candles.router, prefix=API_PREFIX)
app.include_router(dev_kis.router)