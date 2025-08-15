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

# Evaluate arguments
TARGET=""

while getopts ":a:" opt; do
  case $opt in
    a)
      TARGET="$OPTARG"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

# Build bootstrap(s)
if [ -n "$TARGET" ]; then
  ./scripts/build-bootstraps.sh --android10 --architectures "$TARGET" &> $HOME/tmp/build.log
else
  ./scripts/build-bootstraps.sh --android10 &> $HOME/tmp/build.log
fi

# Store bootstrap(s)
cd "$dir"
mkdir -p ../../bootstraps
mv /home/builder/*.zip ../../bootstraps
