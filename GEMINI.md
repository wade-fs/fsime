# Gemini Agent 專案指南：FS Input Method (混瞎輸入法)

歡迎來到 FSIME 專案。本專案是一個高度自定義的 Android 輸入法，支援倉頡、嘸蝦米、混合輸入、數學解析及手寫辨識。

## 📱 技術棧 (Technical Stack)
- **Language:** 100% Kotlin (JVM 17)
- **Architecture:** 分層架構 (Layered Architecture) / 半套 MVVM
  - **Logic:** `com.wade.fsime.engine.InputProcessor` (核心邏輯與狀態管理)
  - **State:** `com.wade.fsime.engine.KeyboardState` (UI 反應式狀態)
  - **Service:** `com.wade.fsime.service.FsimeService` (系統接口)
- **UI:** XML Layouts + Custom Views (`KeyboardView`, `CandidatesView`, `HandwritingView`)
- **Database:** SQLite (`BDatabase` + `user_learning` 學習機制)
- **Math Engine:** 自研 `MathParser` 模組 (位於 `com.wade.fsime.math`)

## 💻 常用開發指令
請利用專案內建的 Gradle Wrapper 進行驗證：
- **完整編譯 (Release):** `./gradlew assembleRelease`
- **程式碼檢查:** `./gradlew lint`
- **清理專案:** `./gradlew clean`

## 🎨 核心開發規範 (Core Guidelines)
1.  **邏輯分離 (SoC):** 嚴禁在 `FsimeService` 中直接撰寫複雜的組字或候選字計算邏輯。所有業務邏輯必須實作在 `InputProcessor` 中。
2.  **狀態驅動 (State-Driven):** UI 的更新應透過觀察 `KeyboardState` 的變化來達成。當需要改變介面時，應修改 `InputProcessor` 中的 `state`。
3.  **空安全與類型:** 嚴格遵守 Kotlin 空安全規範。對於資料庫回傳的 `String?` 應妥善處理。
4.  **不變性 (Immutability):** `KeyboardState` 必須是不可變的 (Data Class with `val`)，更新時請使用 `.copy()`。

## 🔧 專案特定規則
- **鍵盤擴充:** 所有的鍵盤定義位於 `app/src/main/res/xml/`。若要新增鍵盤類型，需在 `InputProcessor.computeCandidateList` 中定義對應的資料查詢邏輯。
- **學習機制:** 使用者選字後會觸發 `BDatabase.updateUsage`。維護資料庫查詢時，必須確保 `LEFT JOIN user_learning` 的邏輯正確，以維持「選愈多次愈靠前」的功能。
- **數學解析:** `math` 套件下的類別嚴禁與 Android UI 元件產生依賴，保持其作為純工具模組的獨立性。

## 🤖 代理人自動化工作流
- **重構建議:** 在修改 `FsimeService` 前，先思考該邏輯是否能抽離至 `engine` 或 `util`。
- **品質保證:** 每次修改邏輯或搬移檔案後，必須執行 `./gradlew assembleRelease` 確保 R 類別引用及套件路徑正確。
