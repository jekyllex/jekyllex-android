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

# Build bootstraps for each architecture in parallel
for target_arch in aarch64 arm i686 x86_64; do
  (
    ./scripts/run-docker.sh ./clean.sh
    ./scripts/run-docker.sh ./scripts/build-bootstraps.sh --android10 --architectures "$target_arch"
  ) &
done

# Wait for all background builds to finish
wait

# Store bootstrap
mkdir -p ../../../bootstraps
mv *.zip ../../../bootstraps
