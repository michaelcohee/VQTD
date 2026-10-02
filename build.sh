#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
kotlinc src/*.kt -include-runtime -d build/vqtd.jar
printf 'Built %s\n' "$(pwd)/build/vqtd.jar"
printf 'Run with: java -jar build/vqtd.jar\n'
printf 'Self-test: java -Djava.awt.headless=true -jar build/vqtd.jar --self-test\n'
