all: clean_build build_app

build_app:
	-javac -Xdiags:verbose -g -d build/ *.java

clean_build:
	-rm -rf build/nbm
