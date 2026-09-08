# 混瞎輸入法 (FsimeIME) - Windows 桌面版

這是一個與 Android 核心共用資料庫與邏輯的 Windows 桌面版輸入法。透過將核心邏輯抽離至 Go 後端，並在前端使用極簡的 C++ DLL 對接 TSF (Text Services Framework)，我們能夠在 Windows 上實現一模一樣的「混瞎（注音、倉頡、嘸蝦米）」輸入體驗。

## 🛠 編譯環境需求

本專案所有的編譯工作都可以且建議在 **Ubuntu (Linux)** 下完成（透過交叉編譯）。您不需要安裝龐大的 Visual Studio。

請確認您的 Ubuntu 環境已安裝以下依賴：

```bash
sudo apt update
sudo apt install golang-go mingw-w64 zip wine64
```

## 🚀 一鍵編譯與打包

在 `windows` 目錄下，您只需要執行：

```bash
make all
```

這個指令會自動執行以下完整的流程：
1. **編譯後端 (`make backend`)**：使用 Go 將伺服器交叉編譯為 Windows 可執行的 `fsime-server.exe`。
2. **編譯前端 (`make frontend`)**：使用 MinGW-w64 交叉編譯 C++ TSF 元件為 `FsimeIME.dll`。
3. **處理資料庫 (`make db`)**：將 Android 專案的 `b.db` 複製到 Windows 發布資料夾。
4. **產生安裝包 (`make installer`)**：透過 Wine 執行 Inno Setup，把所有執行檔與 DLL 打包成一個便於在 Windows 上點擊安裝的 `setup.exe`。

## 📁 輸出目錄

執行完畢後，您會在 `windows` 目錄下看到以下產出：

- `output/FsimeIME-1.0.0-setup.exe` 🌟 **(最終安裝檔，將這個檔案丟到 Windows 測試即可)**
- `dist/fsime-server.exe` (Go 核心引擎)
- `dist/FsimeIME.dll` (TSF 前端模組)
- `dist/b.db` (共用詞庫)

## 🐛 測試與除錯

1. 將 `output/FsimeIME-1.0.0-setup.exe` 複製到您的 Windows 虛擬機 (VMware) 中。
2. 雙擊安裝，安裝程式會自動把檔案放到 `C:\Program Files\FsimeIME` 並替您註冊 `FsimeIME.dll` (呼叫 `regsvr32`)。
3. 按下 `Win + 空白鍵` 切換到「混瞎輸入法」。
4. 隨便開啟一個記事本，輸入字母即可測試。

> **如果需要修改架構或深入了解運作原理，請參考 [程式架構說明 (docs/README.md)](docs/README.md)**。
