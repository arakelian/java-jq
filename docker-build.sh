SCRIPT_PATH=$( cd "$(dirname "$0")" ; pwd -P )
PROJECT=$(basename "${SCRIPT_PATH}")
JQ_VERSION=1.6

set -e

# build linux version
docker build --tag ${PROJECT} ${SCRIPT_PATH}/.

# get file
mkdir -p src/main/resources/lib/linux-x86_64
docker run --rm -v $(pwd -P)/src/main/resources/lib/linux-x86_64:/mnt ${PROJECT} cp /root/build/jq-${JQ_VERSION}/.libs/libjq.so /mnt
echo "Linux-x86_64 JQ build successfully."


# build aarch64 linux version
docker build --platform=linux/arm64 --tag ${PROJECT}_aarch64 -f Dockerfile.aarch64 ${SCRIPT_PATH}/.

# get file
mkdir -p src/main/resources/lib/linux-aarch64
docker run --rm -v $(pwd -P)/src/main/resources/lib/linux-aarch64:/mnt ${PROJECT}_aarch64 cp /home/libjq.so /mnt
echo "Linux-aarch64 JQ build successfully."
