from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings) :
    APP_NAME = "portfolio"

    PG_HOST : str
    PG_PORT : str
    PG_DB_NAME : str
    PG_PASSWORD : str
    PG_USER : str

    config = SettingsConfigDict(env_file='.env')

    @property
    def PG_DSN(self) -> str: 
        return f"postgresql+asyncpg://{self.PG_USER}:{self.PG_PASSWORD}@{self.PG_HOST}:{self.PG_PASSWORD}/{self.PG_DB_NAME}"