#!/bin/bash

CWD=$(cd "$(dirname "$0")" && pwd)
APP_LIB=$CWD/../lib

for i in "$APP_LIB"/*.jar; do
    CP_JARS=$CP_JARS:$i
done
CP_JARS=`echo $CP_JARS | cut -c2-`
LIB_JARS=`echo $CP_JARS | sed 's/:/,/g'`

#APP_CONFIG=$CWD/../config/<config file>

CLASSPATH=$CP_JARS:$CWD/../config/
export CLASSPATH

APP_NAME=thn.tools.Lucene3

java $APP_NAME $*
