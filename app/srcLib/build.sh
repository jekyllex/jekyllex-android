#!/usr/bin/env bash
set -e

# Setup environment
dir=$(pwd)
echo "Current directory: $dir"
exit 1
sudo mkdir -p /data /home/builder
sudo chown $USER:$USER /data /home/builder
cp -r bootstrap/* termux-packages /home/builder/
cd /home/builder
mv *.sh termux-packages/scripts/
mv termux-packages/* .
for package in patches/*; do cp -r "$package" packages; done
find packages/git -type f -name "*subpackage*" -exec rm {} +
find packages/libxml2 -type f -name "*python*" -exec rm {} +

if [[ -z "$ANDROID_HOME" || -z "$NDK" ]]; then
  ./scripts/setup-android-sdk.sh
fi

# Build bootstraps for each architecture
./scripts/build-bootstraps.sh --android10 --architectures aarch64

# Store bootstrap
cd "$dir"
mkdir -p ../../../bootstraps
mv /home/builder/*.zip ../../../bootstraps
