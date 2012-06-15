#!/bin/sh
mkdir -p javabuild
cd java
find . -name "*.java" > javasrc.txt
javac -d ../javabuild @javasrc.txt
rm javasrc.txt
cd ..
echo "Manifest-Version: 1.0" > manifest.txt
echo "Created-By: 1.6.0 (Sun Microsystems Inc.)" >> manifest.txt
echo "Main-Class: com.xoba.ngaro.NGaroVM" >> manifest.txt
cd javabuild
jar cfm ../retro.jar ../manifest.txt .
cd ..
rm manifest.txt
rm -rf javabuild
