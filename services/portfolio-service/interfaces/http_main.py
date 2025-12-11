from fastapi import FastAPI
from interfaces.http_router import router 

app= FastAPI(title = "Portfolio")


app.include_router(router)

