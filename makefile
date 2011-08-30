# Makefile for the anza project
all: conf/host.key .lastdepsrun jscompress

run: all
	play run --https.port=9443

push:
	git push origin master

stage: all
	@./checkClean.sh
	git push staging staging:master
	git push origin staging:staging

deploy: all
	@./checkClean.sh
	git push prod prod:master
	git push origin prod:prod

conf/host.key:
	./genCert.sh

deps: .lastdepsrun

.lastdepsrun: conf/dependencies.yml
	play deps --sync
	play ec
	date > .lastdepsrun

jsdir=public/js/
jsfiles=${jsdir}jquery-1.6.2.min.js ${jsdir}underscore-min.js ${jsdir}scripts.js ${jsdir}jquery.mobile-1.0b2.min.js
jsall=${jsdir}all.js

jscompress: ${jsall}

${jsall}: ${jsfiles}
	cat ${jsfiles} > ${jsall}

clean:
	play clean

superclean:
	# RUN THIS AT YOUR OWN RISK, THIS WILL DELETE EVERY UNTRACKED FILE 
	git clean -dxf

