#!/usr/bin/env bash

set -e

ORIG="me_orig.jpg"

declare -a SIZES
SIZES=(70 150 152 167 180 310 192 512)

TARGET_500="me.jpg"

for SIZE in "${SIZES[@]}";
do
  magick "${ORIG}" \
    -gravity west \
    -define png:exclude-chunk=EXIF,iCCP,iTXt,sRGB,tEXt,zCCP,zTXt,date \
    -extent "%[fx:h<w?h:w]x%[fx:h<w?h:w]" \
    -resize "${SIZE}x${SIZE}" \
    "me${SIZE}.png"
done

magick "${ORIG}" \
  -gravity west \
  -extent "%[fx:h<w?h:w]x%[fx:h<w?h:w]" \
  -resize 500x500 \
  -strip \
  -quality 80 \
  "${TARGET_500}"
