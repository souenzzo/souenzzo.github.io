#!/usr/bin/env sh

set -e

ORIG="me_orig.jpg"

TARGET_512="me512.png"
TARGET_500="me.jpg"
TARGET_192="me192.png"

magick "${ORIG}" \
  -gravity west \
  -define png:exclude-chunk=EXIF,iCCP,iTXt,sRGB,tEXt,zCCP,zTXt,date \
  -extent "%[fx:h<w?h:w]x%[fx:h<w?h:w]" \
  -resize 512x512 \
  "${TARGET_512}"

magick "${ORIG}" \
  -gravity west \
  -define png:exclude-chunk=EXIF,iCCP,iTXt,sRGB,tEXt,zCCP,zTXt,date \
  -extent "%[fx:h<w?h:w]x%[fx:h<w?h:w]" \
  -resize 192x192 \
  "${TARGET_192}"

magick "${ORIG}" \
  -gravity west \
  -extent "%[fx:h<w?h:w]x%[fx:h<w?h:w]" \
  -resize 500x500 \
  -strip \
  -quality 80 \
  "${TARGET_500}"
