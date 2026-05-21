  1. 更新 README.md                                                                                                    
  我已在 README.md 最上方加入了最新的版本標籤與 APK 下載按鈕。按鈕會自動指向 GitHub 的最新 Release 頁面。              
                                                                                                                       
  2. GitHub 自動化發佈 (GitHub Actions)                                                                                
  我建立了 .github/workflows/release.yml，這是一個自動化腳本，具備以下功能：
   * 觸發條件：當您在 git 推送一個以 v 開頭的標籤（例如 v1.0, v1.1）時，GitHub 會自動啟動編譯。
   * 自動編譯：在雲端虛擬機環境中自動執行 ./gradlew assembleRelease。
   * 自動建立 Release：編譯成功後，GitHub 會自動建立一個新的 Release 版本，並將生成的 APK 檔案上傳作為附件。

  3. 如何在 GitHub 上使用自動化？
  為了讓雲端編譯能夠正確簽署（Sign）您的 APK，您需要在 GitHub 專案設定中加入以下 Secrets (Settings -> Secrets and
  variables -> Actions)：
   * SIGNING_KEY_BASE64: 您的 .jks 金鑰檔的 Base64 字串（可用 base64 -w0 your_key.jks 產生）。
   * KEY_ALIAS: 金鑰別名
   * KEY_PASSWORD: 金鑰密碼
   * STORE_PASSWORD: 儲存庫密碼

  > 提示：我已修改了 app/build.gradle，讓它優先讀取環境變數，這樣即使在 GitHub 上編譯，也不會因為找不到路徑
  /home/wade/.ssh/... 而失敗。

  如何發佈新版本？
  只要在您的終端機輸入：

   1 git tag v1.0
   2 git push github v1.0
  GitHub 就會開始自動幫您編譯並上傳 APK 到 Release 頁面了！

