# =============================================================================
# windows/Makefile — FsimeIME Windows Build System
#
# 在 Ubuntu 交叉編譯 Windows 輸入法元件
#
# 架構：
#   後端 (Go)  → fsime-server.exe   (純 Go，CGO_ENABLED=0)
#   前端 (C++) → FsimeIME.dll       (TSF thin DLL，MinGW-w64 交叉編譯)
#   資料庫     → b.db               (從 Android assets 複製)
#
# 依賴 (Ubuntu)：
#   sudo apt install golang-go mingw-w64
#   cd backend && go mod download
#
# 使用方式：
#   make all          — 編譯後端 + 前端 + 複製 DB
#   make backend      — 只編譯 Go 後端
#   make frontend     — 只編譯 C++ DLL (需要 mingw-w64)
#   make db           — 複製 b.db 到 dist/
#   make dist         — 打包所有輸出到 dist/
#   make clean        — 清除編譯產物
#   make check-deps   — 檢查編譯環境
# =============================================================================

# ---------------------------------------------------------------------------
# 路徑設定
# ---------------------------------------------------------------------------
ROOT_DIR        := $(abspath ..)
BACKEND_DIR     := $(CURDIR)/backend
FRONTEND_DIR    := $(CURDIR)/frontend
DIST_DIR        := $(CURDIR)/dist

# b.db 來源 (Android assets)
DB_SRC          := $(ROOT_DIR)/app/src/main/assets/databases/b.db

# ---------------------------------------------------------------------------
# 工具鏈設定
# ---------------------------------------------------------------------------
# Go 交叉編譯設定 (純 Go，免 CGO)
GO              := go
GOOS            := windows
GOARCH          := amd64
CGO_ENABLED     := 0
GOFLAGS         := -trimpath -ldflags="-s -w"

# MinGW-w64 交叉編譯器 (前端 DLL)
CXX             := x86_64-w64-mingw32-g++
WINDRES         := x86_64-w64-mingw32-windres
STRIP           := x86_64-w64-mingw32-strip

# ---------------------------------------------------------------------------
# 輸出檔案
# ---------------------------------------------------------------------------
SERVER_EXE      := $(DIST_DIR)/fsime-server.exe
DLL_OUT         := $(DIST_DIR)/FsimeIME.dll
DB_OUT          := $(DIST_DIR)/b.db

# ---------------------------------------------------------------------------
# 前端編譯旗標
# ---------------------------------------------------------------------------
CXXFLAGS        := -std=c++17 -O2 -Wall -DUNICODE -D_UNICODE \
                   -D_WIN32_WINNT=0x0601 -DWINVER=0x0601

LDFLAGS         := -shared \
                   -Wl,--subsystem,windows \
                   -Wl,--kill-at \
                   -lole32 -loleaut32 -luuid \
                   $(FRONTEND_DIR)/FsimeIME.def

# Inno Setup
INNO_DIR        := $(CURDIR)/inno-setup-6.4.0
INNO_ISCC       := $(INNO_DIR)/ISCC.exe
INNO_SCRIPT     := $(CURDIR)/scripts/fsime.iss
INSTALLER_OUT   := $(CURDIR)/output
WINE            := wine
WINEDEBUG       ?= -all

FRONTEND_SRCS   := $(FRONTEND_DIR)/FsimeTextService.cpp \
                   $(FRONTEND_DIR)/PipeClient.cpp

# ---------------------------------------------------------------------------
# 預設目標
# ---------------------------------------------------------------------------
.PHONY: all backend frontend db installer clean check-deps

all: check-go $(DIST_DIR) backend frontend db installer
	@echo ""
	@echo "✅ 所有編譯與打包已完成！"
	@echo "輸出目錄："
	@ls -lh $(DIST_DIR)/
	@echo "安裝包目錄："
	@ls -lh $(INSTALLER_OUT)/FsimeIME-*-setup.exe 2>/dev/null || true

# ---------------------------------------------------------------------------
# 後端 (Go server)
# ---------------------------------------------------------------------------
backend: $(SERVER_EXE)

$(SERVER_EXE): $(DIST_DIR) $(shell find $(BACKEND_DIR) -name '*.go' 2>/dev/null)
	@echo ">>> [後端] 交叉編譯 Go server → windows/amd64"
	cd $(BACKEND_DIR) && \
	CGO_ENABLED=$(CGO_ENABLED) GOOS=$(GOOS) GOARCH=$(GOARCH) \
	$(GO) build $(GOFLAGS) -o $(SERVER_EXE) .
	@echo "    輸出: $(SERVER_EXE)"

# 初始化 go.sum (首次執行需要網路)
$(BACKEND_DIR)/go.sum:
	@echo ">>> [後端] 下載 Go 相依套件"
	cd $(BACKEND_DIR) && $(GO) mod tidy

# ---------------------------------------------------------------------------
# 前端 (C++ DLL)
# ---------------------------------------------------------------------------
frontend: $(DLL_OUT)

$(DLL_OUT): $(DIST_DIR) $(FRONTEND_SRCS) $(FRONTEND_DIR)/FsimeIME.def
	@command -v $(CXX) >/dev/null 2>&1 || { \
		echo "❌ 缺少 mingw-w64: sudo apt install mingw-w64"; exit 1; }
	@echo ">>> [前端] 交叉編譯 TSF DLL → $(DLL_OUT)"
	$(CXX) $(CXXFLAGS) $(FRONTEND_SRCS) $(LDFLAGS) -o $(DLL_OUT)
	$(STRIP) $(DLL_OUT)
	@echo "    輸出: $(DLL_OUT)"

# ---------------------------------------------------------------------------
# 資料庫複製
# ---------------------------------------------------------------------------
db: $(DB_OUT)

$(DB_OUT): $(DIST_DIR) $(DB_SRC)
	@echo ">>> [DB] 複製 b.db → dist/"
	cp -v $(DB_SRC) $(DB_OUT)

# ---------------------------------------------------------------------------
# 建立 dist 目錄
# ---------------------------------------------------------------------------
$(DIST_DIR):
	mkdir -p $(DIST_DIR)

# ---------------------------------------------------------------------------
# 安裝包 (Inno Setup via Wine)
# ---------------------------------------------------------------------------
installer: backend frontend db $(INSTALLER_OUT)
	@command -v $(WINE) >/dev/null 2>&1 || { \
		echo "❌ 缺少 wine: sudo apt install wine64"; exit 1; }
	@test -f $(INNO_ISCC) || { \
		echo "❌ 找不到 ISCC.exe: $(INNO_ISCC)"; exit 1; }
	@echo ">>> [installer] 使用 Wine + Inno Setup 6.4.0 打包..."
	WINEDEBUG=$(WINEDEBUG) $(WINE) $(INNO_ISCC) \
		/O"Z:$(INSTALLER_OUT)" \
		"Z:$(INNO_SCRIPT)"
	@echo ""
	@echo "✅ 安裝包輸出："
	@ls -lh $(INSTALLER_OUT)/FsimeIME-*-setup.exe 2>/dev/null || \
		{ echo "⚠  找不到 setup.exe，請確認 ISCC 編譯成功"; exit 1; }

$(INSTALLER_OUT):
	mkdir -p $(INSTALLER_OUT)

# ---------------------------------------------------------------------------
# 環境檢查
# ---------------------------------------------------------------------------
.PHONY: check-go check-deps

check-go:
	@command -v $(GO) >/dev/null 2>&1 || { \
		echo "❌ 缺少 Go: sudo apt install golang-go"; exit 1; }
	@echo "✓ Go: $$($(GO) version)"

check-deps: check-go
	@command -v $(CXX) >/dev/null 2>&1 && \
		echo "✓ MinGW-w64: $$($(CXX) --version | head -1)" || \
		echo "⚠ MinGW-w64 未安裝（前端 DLL 需要）: sudo apt install mingw-w64"
	@command -v zip >/dev/null 2>&1 && \
		echo "✓ zip 已安裝" || \
		echo "⚠ zip 未安裝: sudo apt install zip"
	@command -v $(WINE) >/dev/null 2>&1 && \
		echo "✓ Wine: $$($(WINE) --version 2>/dev/null | head -1)" || \
		echo "⚠ Wine 未安裝（安裝包需要）: sudo apt install wine64"
	@test -f $(INNO_ISCC) && \
		echo "✓ Inno Setup ISCC.exe: $(INNO_ISCC)" || \
		echo "⚠ 找不到 ISCC.exe: $(INNO_ISCC)"
	@test -f $(DB_SRC) && \
		echo "✓ b.db 存在: $(DB_SRC)" || \
		echo "❌ b.db 不存在: $(DB_SRC)"

# ---------------------------------------------------------------------------
# 清除
# ---------------------------------------------------------------------------
clean:
	rm -rf $(DIST_DIR) $(INSTALLER_OUT)
	@echo "✅ 清除完成"
