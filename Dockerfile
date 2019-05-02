# java-jq

FROM  ubuntu:18.04
LABEL maintainer="Greg Arakelian <GREG@ARAKELIAN.COM>"

# install required software
RUN apt update && \
    apt install -y build-essential git autoconf automake libtool emacs-nox wget bash valgrind

# copy script
COPY build-jq.sh /usr/local/bin

# build jq
RUN cd ~ && \
    chmod 755 /usr/local/bin/*.sh && \
    build-jq.sh
