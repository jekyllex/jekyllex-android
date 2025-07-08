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

./scripts/setup-ubuntu.sh

# Build bootstraps for each architecture in parallel
pids=()
for target_arch in aarch64 arm i686 x86_64; do
  (
    ./clean.sh
    ./scripts/build-bootstraps.sh --android10 --architectures "$target_arch"
  ) &
  pids+=($!)
done

# Wait and stop on first failure
for pid in "${pids[@]}"; do
  if ! wait "$pid"; then
    echo "Build failed for PID $pid - stopping remaining builds"
    for remaining_pid in "${pids[@]}"; do
      kill "$remaining_pid" 2>/dev/null || true
    done
    exit 1
  fi
done

# Store bootstrap
mkdir -p ../../../bootstraps
mv *.zip ../../../bootstraps
