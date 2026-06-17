set -e

# Build internal mod
echo "Building internal mod..."
cd mod

chmod +x ./gradlew
./gradlew clean runDatagen
./gradlew build

# Build pack
echo "Building pack..."
cd ..

rm -rf build
mkdir -p build
cp -r mods index.toml pack.toml build
cp mod/build/libs/* build/mods

cd build
mkdir -p ../out
packwiz modrinth export -o ../out/silly_cars.mrpack
packwiz refresh --build