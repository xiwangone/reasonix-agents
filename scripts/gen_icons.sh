#!/usr/bin/env bash
# 生成主题图标包（批 B-13）：Material 蓝 + 品牌深色两套变体
# 依赖：ImageMagick convert
set -euo pipefail

RES="app/src/main/res"

echo "== 生成 logo 前景变体 =="
convert "$RES/drawable/logo.png" -modulate 100,108,-120 -colorspace sRGB "$RES/drawable/logo_material.png"
convert "$RES/drawable/logo.png" -evaluate multiply 0.68 -colorspace sRGB "$RES/drawable/logo_brand_dark.png"

for DPI in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
    DIR="${DPI%%:*}"
    SIZE="${DPI##*:}"
    SRC="$RES/mipmap-$DIR/ic_launcher.png"
    echo "== $DIR (${SIZE}px) =="
    # Material 蓝变体（含 round）
    convert "$SRC" -modulate 100,108,-120 -colorspace sRGB "$RES/mipmap-$DIR/ic_launcher_material.png"
    convert "$SRC" -modulate 100,108,-120 -colorspace sRGB "$RES/mipmap-$DIR/ic_launcher_round_material.png"
    # 品牌深色变体（含 round）
    convert "$SRC" -evaluate multiply 0.68 -colorspace sRGB "$RES/mipmap-$DIR/ic_launcher_brand_dark.png"
    convert "$SRC" -evaluate multiply 0.68 -colorspace sRGB "$RES/mipmap-$DIR/ic_launcher_round_brand_dark.png"
done

echo "== 生成 adaptive icon XML =="
for NAME in ic_launcher_material ic_launcher_brand_dark; do
    if [[ "$NAME" == *material* ]]; then
        BG="ic_launcher_background_material"
        FG="logo_material"
    else
        BG="ic_launcher_background_brand_dark"
        FG="logo_brand_dark"
    fi
    cat > "$RES/mipmap-anydpi-v26/$NAME.xml" << EOF
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/$BG" />
    <foreground android:drawable="@drawable/$FG" />
</adaptive-icon>
EOF
    cat > "$RES/mipmap-anydpi-v26/${NAME}_round.xml" << EOF
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/$BG" />
    <foreground android:drawable="@drawable/$FG" />
</adaptive-icon>
EOF
done

echo "== 完成 =="
ls -la "$RES/drawable/logo_material.png" "$RES/drawable/logo_brand_dark.png"
ls "$RES/mipmap-xxxhdpi/" | grep -E "material|brand_dark"
