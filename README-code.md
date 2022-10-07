- mPredictionOn 決定要不要顯示候選區, 自創的輸入法才需要
	if (mKeyboardState >= R.integer.keyboard_boshiamy) { ... }
- mComposing 是未送出前的輸入字串
	- 清空時
```java
	mComposing.setLength(0);
	updateCandidates(0);
```
- setSuggestions() 用來設定候選區內容，只負責打開，不負責關閉
	清空：setSuggestions(null, false, false)
- commitTyped() 送出選字
	- 按空白時如下
```java
	InputConnection ic = getCurrentInputConnection();
	mComposing.append((char)27);
	ic.commitText(mComposing, 0);
	commitTyped(getCurrentInputConnection());
	mComposing.setLength(0);
	ic.finishComposingText();
```
- updateCandidates() 更新候選區，
	- 內容是由 mComposing 從資料庫產生
	- 第一筆就是輸入組字
	- 呼叫時機: 初始化輸入框、打字、送出選字
