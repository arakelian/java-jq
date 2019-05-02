SCRIPT_PATH=$( cd "$(dirname "$0")" ; pwd -P )
PROJECT=$(basename "${SCRIPT_PATH}")
JQ_VERSION=1.6

set -e

# build linux version
docker build --tag ${PROJECT} ${SCRIPT_PATH}/.

# get file
mkdir -p src/main/resources/lib/linux-x86_64
docker run --rm -v $(pwd -P)/src/main/resources/lib/linux-x86_64:/mnt java-jq cp /root/build/jq-${JQ_VERSION}/.libs/libjq.so /mnt
echo "Linux JQ build successfully."
