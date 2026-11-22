#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out
find src/main/java src/test/java -name '*.java' > out/sources.txt
javac --release 21 -d out @out/sources.txt
java -cp out com.example.ytdownloader.BasicTests
