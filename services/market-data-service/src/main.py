from fastapi import FastAPI
from deps import lifespan
from routers import health, symbols

app = FastAPI(lifespan=lifespan)
app.include_router(health.router)
app.include_router(symbols.router)