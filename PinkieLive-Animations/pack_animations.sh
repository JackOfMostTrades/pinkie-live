#!/bin/bash -e

# Lyra
mkdir target
cp assets/lyra.zip target/
cat <<EOF > target/manifest.properties
id=lyra
version=1
name=Lyra
className=com.derpfish.pinkielive.animation.LyraAnimation
EOF
cd bin/classes
jar cf lib.jar com/derpfish/pinkielive/animation/LyraAnimation.class
~/Downloads/android-sdk-linux/platform-tools/dx --dex --output=../../target/lib.jar lib.jar
rm lib.jar
cd ../../target
zip -9 ../lyra.zip *
cd ..
rm -rf target

# Rarity
mkdir target
cp assets/rarity.png target/
cat <<EOF > target/manifest.properties
id=rarity
version=1
name=Rarity
className=com.derpfish.pinkielive.animation.RarityAnimation
EOF
cd bin/classes
jar cf lib.jar com/derpfish/pinkielive/animation/RarityAnimation.class
~/Downloads/android-sdk-linux/platform-tools/dx --dex --output=../../target/lib.jar lib.jar
rm lib.jar
cd ../../target
zip -9 ../rarity.zip *
cd ..
rm -rf target

