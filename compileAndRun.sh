#!/bin/bash
JAVA_HOME = "/Applications/Android\ Studio\ Preview.app/Contents/jre/Contents/Home/"
export JAVA_HOME
./gradlew tui-browser:build && java -jar tui-browser/build/libs/tui-browser-all.jar
#./gradlew log-sample-app:build && java -jar log-sample-app/build/libs/log-sample-app-all.jar