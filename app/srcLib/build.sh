#!/usr/bin/env bash
set -e

dir=$(pwd)
cp -r bootstrap/* termux-packages /home/builder/
cd /home/builder
mv *.sh termux-packages/scripts/
mv termux-packages/* .
patch -p1 < patches/properties.patch
for package in patches/*; do
  [ -d "$package" ] || continue
  cp -r "$package" packages
done
find packages/git -type f -name "*subpackage*" -exec rm {} +
find packages/libxml2 -type f -name "*python*" -exec rm {} +
find packages/ruby -type f \( -name 'process.c.patch' -o -name 'yjit-src-*.patch' -o -name 'lib-rubygems-install_update_options.rb.patch' \) -exec rm {} +

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
