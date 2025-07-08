#!/usr/bin/env bash
set -e

if [ "$GITHUB_ACTIONS" = "true" ]; then
  # Setup docker
  DOCKER_VERSION=27.0.1
  curl -fsSL https://download.docker.com/linux/static/stable/x86_64/docker-$DOCKER_VERSION.tgz -o docker.tgz
  tar xzvf docker.tgz
  sudo mv docker/* /usr/local/bin/

  export DOCKER_DIR=$GITHUB_WORKSPACE/.docker
  export DOCKER_SOCKET=$GITHUB_WORKSPACE/docker.sock
  mkdir -p $DOCKER_DIR

  sudo nohup dockerd \
    --data-root=$DOCKER_DIR \
    --host=unix://$DOCKER_SOCKET \
    --pidfile=$GITHUB_WORKSPACE/dockerd.pid > $GITHUB_WORKSPACE/dockerd.log 2>&1 &

  export DOCKER_HOST=unix://$DOCKER_SOCKET
  echo "DOCKER_HOST=unix://$DOCKER_SOCKET" >> $GITHUB_ENV

  if ! timeout 20s bash -c "until docker info; do sleep 1; done"; then
    echo "Docker daemon failed to start!"
    cat "$GITHUB_WORKSPACE/dockerd.log"
    exit 1
  fi

  sudo chown -R runner:runner $DOCKER_DIR
  sudo chown runner:runner $DOCKER_SOCKET
fi

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
pids=()
for target_arch in aarch64 arm i686 x86_64; do
  (
    ./scripts/run-docker.sh ./clean.sh
    ./scripts/run-docker.sh ./scripts/build-bootstraps.sh --android10 --architectures "$target_arch"
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
