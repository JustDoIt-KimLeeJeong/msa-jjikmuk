# PostgreSQL 경로
PG_INCLUDE = C:/msys64/mingw64/include
PG_LIB = C:/msys64/mingw64/lib

# 컴파일 옵션
CFLAGS = -Wall -I./include -I$(PG_INCLUDE) -DTEST_MODE
LDFLAGS = -L$(PG_LIB) -lpq -lws2_32 -lsecur32

SRC = src/adapters/db/postgre_rep.c
TARGET = build/test_pg.exe

all: test-pg

build:
	@mkdir -p build

test-pg: build
	@echo "Building..."
	gcc $(CFLAGS) $(SRC) -o $(TARGET) $(LDFLAGS)
	@echo "Build complete: $(TARGET)"

run: test-pg
	@bash run.sh

clean:
	rm -rf build

.PHONY: all test-pg run clean build