JJQ_VERSION=0.0.1
ONIGURUMA_VERSION=5.9.6
JQ_VERSION=1.5
PLATFORM=$$(printf "$$(uname)-$$(uname -m)" | tr "[A-Z]" "[a-z]")

.PHONY: fetch install build clean

build: target/jjq-$(JJQ_VERSION)-SNAPSHOT.jar
	
fetch: build/make/onig-$(ONIGURUMA_VERSION).tar.gz build/make/jq-$(JQ_VERSION).tar.gz

build/make/onig-$(ONIGURUMA_VERSION).tar.gz:
	mkdir -p build/make && \
	cd build/make && \
	wget https://github.com/kkos/oniguruma/releases/download/v$(ONIGURUMA_VERSION)/onig-$(ONIGURUMA_VERSION).tar.gz

build/make/jq-$(JQ_VERSION).tar.gz:
	mkdir -p build/make && \
	cd build/make && \
	wget https://github.com/stedolan/jq/releases/download/jq-$(JQ_VERSION)/jq-$(JQ_VERSION).tar.gz

build/make/lib/libonig.so: build/make/onig-$(ONIGURUMA_VERSION).tar.gz
	cd build/make && \
	tar xvf onig-$(ONIGURUMA_VERSION).tar.gz && \
	cd onig-$(ONIGURUMA_VERSION) && \
	./configure --prefix $$(cd .. && pwd -P) && \
	make && \
	make install

build/make/lib/libjq.so: build/make/jq-$(JQ_VERSION).tar.gz build/make/lib/libonig.so
	cd build/make && \
	tar xvf jq-$(JQ_VERSION).tar.gz && \
	cd jq-$(JQ_VERSION) && \
	./configure --disable-maintainer-mode --prefix $$(cd .. && pwd -P) --with-oniguruma=$$(cd .. && pwd -P) && \
	sed -i.bak 's/LIBS =  -lonig/LIBS = /' Makefile && \
	sed -i.bak "s/libjq_la_LIBADD = -lm/libjq_la_LIBADD = -lm $$(find ../onig-$(ONIGURUMA_VERSION) -name '*.lo' | xargs echo | sed 's/\//\\\//g')/" Makefile && \
	sed -i.bak "s/jq_LDADD = libjq.la -lm/jq_LDADD = libjq.la -lm $$(find ../onig-$(ONIGURUMA_VERSION) -name '*.lo' | xargs echo | sed 's/\//\\\//g')/" Makefile && \
	make && \
	make install

src/main/resources/lib/: build/make/lib/libjq.so build/make/lib/libonig.so
	mkdir -p src/main/resources/lib/$(PLATFORM)
	cd src/main/resources/lib/$(PLATFORM) && \
	find ../../../../../build/make/lib \( -name 'libjq.so' -o -name 'libjq.dylib' \) -exec cp -L \{} . \;

target/jjq-$(JJQ_VERSION)-SNAPSHOT.jar: src/main/resources/lib/
	echo "Built"

install: build
	echo "Installed"

clean:
	rm -rf build/make src/main/resources/lib target
