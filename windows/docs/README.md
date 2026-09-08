# 混瞎輸入法 (FsimeIME) - 程式架構說明

FsimeIME Windows 版採用了「**前/後端分離**」的微服務式架構，將沉重的 UI / 系統事件攔截與核心的選字邏輯解耦。

## 🏛 架構總覽

整體運作分為三個主要元件：

1. **TSF 前端 DLL (`FsimeIME.dll`, C++)**：負責攔截 Windows 系統的按鍵，以及渲染選字視窗。
2. **IPC 通訊橋樑 (Named Pipes)**：前端透過 Windows 具名管道 (Named Pipe) 將按鍵事件送給後端。
3. **輸入法後端引擎 (`fsime-server.exe`, Go)**：負責邏輯運算、查表、組字，並回傳結果。

---

### 1. 前端：TSF Text Service (C++)
**位置：** `windows/frontend/`

前端是一隻非常輕量的 C++ 動態連結函式庫 (DLL)，實作了微軟的 Text Services Framework (TSF)。
為了追求「極簡且無依賴」，我們沒有使用 PIME 或龐大的 COM 框架包裝，而是直接手刻最精簡的實作：

- **`Register.cpp`**: 負責處理 TSF 與 COM 的註冊機制，寫入 Windows 登錄檔 (Registry)，讓系統在語言列認得「混瞎輸入法」。
- **`FsimeTextService.cpp`**: 核心介面實作。
  - 實作 `ITfTextInputProcessor` (控制輸入法生命週期) 與 `ITfKeyEventSink` (攔截按鍵)。
  - 內含 `CandidateWindow`：一個極簡的 Win32 無邊框浮動視窗 (`WS_EX_TOOLWINDOW`)。它會呼叫 `GetGUIThreadInfo` 動態捕捉使用者的輸入游標位置，並透過 GDI 繪製目前的注音/字根與候選字清單。
- **`PipeClient.cpp`**: 透過 Windows Named Pipe (`\\.\pipe\FsimeServer`) 與後端進行 JSON-Lines 格式的非同步通訊。

### 2. IPC 通訊協定 (JSON-Lines)

前後端之間採用非常簡單直覺的 JSON 通訊：

- **前端 -> 後端 (Request)**
  - 輸入字母：`{"type": "key", "key": "a"}`
  - 按下退格：`{"type": "key", "key": "BackSpace"}`
  - 數字選字：`{"type": "select", "index": 0}`
  - 取消輸入：`{"type": "reset"}`

- **後端 -> 前端 (Response)**
  - `{"commit": "這", "composition": "這", "candidates": ["1.這", "2.著"]}`
  - 前端收到後，如果有 `commit` 字串，就會用 `SendInput` (或 TSF InsertText) 將文字送進應用程式；如果有 `composition` 或 `candidates`，就會更新游標下方的 `CandidateWindow` 畫面。

### 3. 後端：Go 語言引擎
**位置：** `windows/backend/`

後端是完全獨立的 Go 執行檔，優點是可以**免 CGO 跨平台編譯**，並輕鬆引入各種高階的邏輯與套件。

- **`server.go` / `main.go`**: 負責建立 Named Pipe Server，監聽前端連線，並負責解析 JSON。
- **`engine/processor.go`**: 狀態機與組字邏輯。這部分**完全對齊 Android 版本的 `InputProcessor.kt`**。
- **`db/db.go`**: 負責查詢 `b.db`。使用純 Go 的 SQLite 驅動 (`modernc.org/sqlite`)，確保交叉編譯順利。

#### 🌟 核心亮點：原生混瞎輸入

這個架構天然繼承了 Android 的「混瞎」特性。當使用者敲擊實體鍵盤的字母 (例如 `a`)：
1. 字母被傳到後端的 `processor.go`。
2. 在 `boshiamy` (嘸蝦米) 模式下，`db.go` 的 `GetWord` 函式會**同時查閱 5 個資料表**：
   - `boshiamy` (嘸蝦米表)
   - `sym` (符號表)
   - `ji` (注音表)
   - `cj` (倉頡表)
   - `stroke` (筆畫)
3. 查閱依據為資料表內的 `eng` 欄位。由於注音符號 (如 `ㄇ`) 在資料庫裡對應的英文按鍵正是 `a`，因此**打字時完全不用切換模式**，後端會自動把「嘸蝦米字、注音字、倉頡字」全部混入同一個 `candidates` 陣列回傳給前端。

這使得 Windows 版不用寫任何複雜的鍵盤佈局對應表，就能完美重現 Android `keyboard_full.xml` 的強大混輸功能！
