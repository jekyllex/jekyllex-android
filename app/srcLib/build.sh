#!/usr/bin/env bash
set -e

# Setup environment
dir=$(pwd)
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
./scripts/build-bootstraps.sh --android10

# Store bootstraps
cd "$dir"
mkdir -p ../../bootstraps
mv /home/builder/*.zip ../../bootstraps
