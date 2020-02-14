#!/bin/bash
set -e

cd "$( dirname "${BASH_SOURCE[0]}" )"

mvn compile
mvn dependency:build-classpath -Dmdep.outputFile=classpath
sed -i "1s;^;$PWD/target/classes/:;" classpath

mvn -Dscijava.app.directory="$HOME/Fiji.app"
echo OK
