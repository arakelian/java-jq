#!/bin/bash

mkdir -p build
cd build

ONIGURUMA_VERSION=5.9.6
JQ_VERSION=1.6

PLATFORM=$(printf "$(uname)-$(uname -m)" | tr "[A-Z]" "[a-z]")

wget https://github.com/kkos/oniguruma/releases/download/v${ONIGURUMA_VERSION}/onig-${ONIGURUMA_VERSION}.tar.gz && \
   tar xvf onig-${ONIGURUMA_VERSION}.tar.gz && \
   rm onig-${ONIGURUMA_VERSION}.tar.gz && \
   cd onig-${ONIGURUMA_VERSION} && \
   ./configure --prefix $(cd .. && pwd -P) && \
   make && \
   make install && \
   cd ..

wget https://github.com/stedolan/jq/releases/download/jq-${JQ_VERSION}/jq-${JQ_VERSION}.tar.gz && \
   tar xvf jq-${JQ_VERSION}.tar.gz && \
   rm jq-${JQ_VERSION}.tar.gz && \
   cd jq-${JQ_VERSION} && \
   ./configure --disable-maintainer-mode --prefix $(cd .. && pwd -P) --with-oniguruma=$(cd .. && pwd -P) --disable-docs && \ 
   sed -i.bak 's/LIBS = -lonig/LIBS = /' Makefile && \
   sed -i.bak "s/libjq_la_LIBADD = -lm/libjq_la_LIBADD = -lm $(find ../onig-${ONIGURUMA_VERSION} -name '*.lo' | xargs echo | sed 's/\//\\\//g')/" Makefile && \
   sed -i.bak "s/jq_LDADD = libjq.la -lm/jq_LDADD = libjq.la -lm $(find ../onig-${ONIGURUMA_VERSION} -name '*.lo' | xargs echo | sed 's/\//\\\//g')/" Makefile && \
   make && \
   make install && \
   cd ..
