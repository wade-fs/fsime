   1. 統一狀態管理： 將 shiftMode 和 ctrlMode 的邏輯完全移入 Keyboard 或獨立的 StateManager。              
   2. 簡化 KeyboardView： 移除 KeyboardView 中關於按鍵具體意義的判斷，只傳遞「按下了哪個 Key」。           
   3. 封裝 Key 屬性： 修復我們正在處理的 isShiftable 邏輯，確保它在 XML 解析時就已經確定，且之後不可更改。 
