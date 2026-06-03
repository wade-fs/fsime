#!/bin/bash

# 安裝 FSIME 至 Fcitx5-Rime

echo "🚀 開始配置 FSIME (Fcitx5-Rime 版本)..."

# 檢查是否已生成字典檔
if [ ! -f "fsime.dict.yaml" ]; then
    echo "⚠️ 找不到 fsime.dict.yaml，正在重新生成..."
    go run tools/export_rime.go
    if [ $? -ne 0 ]; then
        echo "❌ 字典檔生成失敗，請確認 b.db 是否存在。"
        exit 1
    fi
fi

# Rime 配置目錄
RIME_DIR="$HOME/.local/share/fcitx5/rime"

# 確保目錄存在
mkdir -p "$RIME_DIR"

echo "📂 複製配置檔至 $RIME_DIR ..."
cp fsime.dict.yaml "$RIME_DIR/"
cp fsime.schema.yaml "$RIME_DIR/"

# 更新 default.custom.yaml 以啟用該 schema
CUSTOM_YAML="$RIME_DIR/default.custom.yaml"
if [ ! -f "$CUSTOM_YAML" ]; then
    echo "patch:" > "$CUSTOM_YAML"
    echo "  schema_list:" >> "$CUSTOM_YAML"
    echo "    - schema: fsime" >> "$CUSTOM_YAML"
else
    # 簡單檢查是否已包含
    if ! grep -q "schema: fsime" "$CUSTOM_YAML"; then
        sed -i '/schema_list:/a \    - schema: fsime' "$CUSTOM_YAML"
    fi
fi

echo "🔄 正在通知 Fcitx5 重新載入設定..."
# 嘗試重新部署 Rime
if command -v qdbus >/dev/null 2>&1; then
    qdbus org.fcitx.Fcitx5 /rime org.fcitx.Fcitx.Rime1.Deploy
    echo "✅ 部署完成！請切換至 Fcitx5 並選擇「混 (FSIME)」輸入法。"
else
    echo "⚠️ 找不到 qdbus，請手動在桌面右下角的 Fcitx5 圖示點擊「重新啟動(Restart)」或「部署(Deploy)」。"
fi

echo "🎉 安裝腳本執行完畢！"
