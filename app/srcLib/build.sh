#!/usr/bin/env bash
set -e

# Setup environment
mkdir tmp
cp -r bootstrap/* termux-packages tmp/
cd tmp
mv termux-packages/* .
mv properties.sh run-docker.sh build-bootstraps.sh scripts/
for package in patches/*; do cp -r "$package" packages; done
find packages/git -type f -name "*subpackage*" -exec rm {} +
find packages/libxml2 -type f -name "*python*" -exec rm {} +

# Build bootstraps for each architecture
./scripts/build-bootstraps.sh --android10 --architectures arm

# Store bootstrap
mkdir -p ../../../bootstraps
mv *.zip ../../../bootstraps
