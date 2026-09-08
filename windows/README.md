# FsimeIME — Windows 中文輸入法

本目錄包含在 Windows 上執行的 FsimeIME 輸入法，與 Android App 共用同一套 `b.db` 詞庫。

## 架構總覽

```
Windows 應用程式
      │ TSF API
      ▼
FsimeIME.dll  (C++ 薄外殼)
  - 向 Windows 登錄 TSF Text Service
  - 攔截鍵盤事件
  - 透過 Named Pipe 傳送 JSON 請求
      │ \\.\pipe\FsimeServer  (JSON Lines)
      ▼
fsime-server.exe  (純 Go 後端)
  - 接收按鍵事件
  - 查詢 b.db (boshiamy/ji/cj/stroke/sym/ngram 表)
  - user_learning 自學 (選愈多次愈靠前)
  - 回傳候選字清單 + 上屏字串
      │
      ▼
b.db  (SQLite，與 Android assets/ 同一個檔案)
```

## 目錄結構

```
windows/
├── Makefile                  ← 頂層建置入口
├── README.md                 ← 本文件
├── backend/                  ← Go 後端 (在 Ubuntu 交叉編譯)
│   ├── go.mod
│   ├── main.go               ← 程式進入點，啟動 Named Pipe server
│   ├── db/db.go              ← SQLite 存取層 (mirrors BDatabase.kt)
│   ├── engine/processor.go   ← InputProcessor (mirrors InputProcessor.kt)
│   ├── ipc/protocol.go       ← JSON 通訊協定定義
│   └── server/server.go      ← Named Pipe server + request dispatch
├── frontend/                 ← C++ TSF DLL (薄外殼)
│   ├── FsimeTextService.cpp  ← ITfTextInputProcessor 實作
│   ├── PipeClient.h/.cpp     ← Named Pipe 客戶端
│   └── FsimeIME.def          ← DLL 匯出定義
└── dist/                     ← 編譯輸出 (git ignored)
    ├── fsime-server.exe
    ├── FsimeIME.dll
    └── b.db
```

## 快速上手

### 環境需求 (Ubuntu)

```bash
# Go (後端)
sudo apt install golang-go

# MinGW-w64 (C++ DLL，可選，也可在 Windows 上用 MSVC 編譯)
sudo apt install mingw-w64

# zip (打包用)
sudo apt install zip
```

### 編譯

```bash
cd windows/

# 檢查環境
make check-deps

# 下載 Go 相依套件
cd backend && go mod tidy && cd ..

# 編譯後端 + 複製 DB
make all

# 同時編譯 C++ DLL (需 mingw-w64)
make frontend

# 打包成 zip 準備發送到 VM
make dist
```

### 在 Windows VM 測試

1. 把 `dist/` 內容複製到 VM (e.g. `C:\FsimeIME\`)
2. 啟動後端：
   ```bat
   fsime-server.exe -db C:\FsimeIME\b.db
   ```
3. 安裝 DLL：
   ```bat
   regsvr32 C:\FsimeIME\FsimeIME.dll
   ```
4. 在「語言設定」啟用 FsimeIME

## IPC 協定

所有訊息為換行分隔的 JSON (JSON Lines)，透過 `\\.\pipe\FsimeServer`。

### DLL → Server (Request)

| type     | 說明               | 額外欄位             |
|----------|--------------------|----------------------|
| `key`    | 使用者按下按鍵     | `key`: 字元或特殊鍵名 |
| `select` | 選擇候選字         | `index`: 候選索引    |
| `reset`  | 清除輸入緩衝       |                      |
| `commit` | 確認上屏 (學習用)  | `prev`: 前一字       |
| `phrase` | 要求聯想字         | `prev`: 前一字       |
| `mode`   | 切換輸入法模式     | `mode`: 模式名稱     |

### Server → DLL (Response)

```json
{
  "composition": "bx",
  "candidates": ["不", "佈", "部"],
  "commit": "",
  "mode": "boshiamy",
  "error": ""
}
```

## 與 Android 的差異

| 功能               | Android (Kotlin)          | Windows (Go)              |
|--------------------|---------------------------|---------------------------|
| DB 存取            | `BDatabase.kt`            | `backend/db/db.go`        |
| 輸入處理           | `InputProcessor.kt`       | `backend/engine/processor.go` |
| UI                 | `KeyboardView`            | TSF Composition/Candidate |
| 學習機制           | `user_learning` 同        | 相同 SQL                  |
| 數學解析           | `MathParser`              | 待實作 (placeholder)      |
| 繁簡轉換           | `TS.kt`                   | 待實作                    |

## 後續待辦

- [ ] TSF Composition 視窗 (顯示注音串)
- [ ] TSF Candidate 視窗 (候選字 UI)
- [ ] 數學解析器 (`evalMath` in processor.go)
- [ ] 繁簡轉換 (ts_mapping table)
- [ ] 注音/拼音前端解析
- [ ] DLL 自動登錄腳本 (Register.cpp)
- [ ] 安裝程式 (NSIS 或 WiX)
- [ ] 考慮借用 [PIME](https://github.com/EasyIME/PIME) 的 TSF 外殼取代自製 DLL
