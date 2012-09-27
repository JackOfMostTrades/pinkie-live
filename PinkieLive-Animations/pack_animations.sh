#!/bin/bash -e

# Usage: pack_anim('lyra_output.zip','id', version, 'name', 'com.derpfish.pinkielive.animation.LyraAnimation', 'lyra.zip', 'lyra2.zip', ...)
pack_anim() {
  mkdir target
  COUNTER=0
  for FOO in "$@"; do
    let "COUNTER+=1"
    if [ $COUNTER -lt 6 ] ; then
      continue
    fi
    cp assets/$FOO target/
  done
  cat <<EOF > target/manifest.properties
id=$2
version=$3
name=$4
className=$5
EOF
  cd bin/classes
  CLASSFILE=`echo -n "$5" | sed 's/\./\//g'`
  jar cf lib.jar $CLASSFILE*.class
  ~/Downloads/android-sdk-linux/platform-tools/dx --dex --output=../../target/lib.jar lib.jar
  rm lib.jar
  cd ../../target
  zip -9 ../$1 *
  cd ..
  rm -rf target
}

# Lyra
pack_anim 'lyra.zip' \
  'lyra' 1 'Lyra' \
  'com.derpfish.pinkielive.animation.LyraAnimation' \
  'lyra.zip'

# Rarity
pack_anim 'rarity.zip' \
  'rarity' 1 'Rarity' \
  'com.derpfish.pinkielive.animation.RarityAnimation' \
  'rarity.png'

# Twilight
pack_anim 'twilight.zip' \
  'twilight' 1 'Twilight Sparkle' \
  'com.derpfish.pinkielive.animation.TwilightAnimation' \
  'twilights.zip' 'bubbles.zip'

