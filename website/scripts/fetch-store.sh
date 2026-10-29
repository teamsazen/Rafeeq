#!/bin/sh
# One-time download of the Play Store screenshots into screens/store/ (01.png … 10.png).
# The site never loads these URLs; npm run images turns the files into public/img/store/.
set -e
cd "$(dirname "$0")/.."
mkdir -p screens/store
i=0
for u in \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516932/Slice_1_rpksya.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516933/Slice_2_wfn58l.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516933/Slice_3_g2yrr5.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516934/Slice_4_fvjhog.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516933/Slice_5_cnbhtf.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516934/Slice_6_neufj2.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516935/Slice_7_felvuf.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516937/Slice_8_ete0vi.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516938/Slice_9_in9guj.png \
  https://res.cloudinary.com/djzvkqwvx/image/upload/v1788516940/Slice_10_kd7anj.png
do
  i=$((i + 1))
  n=$(printf %02d "$i")
  curl -sSfL -o "screens/store/$n.png" "$u"
  echo "saved screens/store/$n.png"
done
