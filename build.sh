#!/bin/bash
cd "$( dirname "${BASH_SOURCE[0]}" )"

mvn compile
mvn dependency:build-classpath -Dmdep.outputFile=classpath
sed -i "1s;^;$PWD/target/classes/:;" classpath
