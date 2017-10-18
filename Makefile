all: clean_build build_app

build_app:
	-javac -g -d build/ *.java

clean_build:
	-rm -rf build/nbm
