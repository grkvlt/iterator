#!/bin/bash
##
# Copyright 2012-2017 by Andrew Kennedy.
# http://www.apache.org/licenses/LICENSE-2.0
#
# Iterated Function System Explorer
#
# andrew.international+iterator@gmail.com
##
#set -x # debug

##
# Setup
##
BIN=$(dirname $0)
PARENT=$(echo "${BIN}/.." | sed -e "s/\/bin\/..//")
LIB="${PARENT}/lib"
JAVA_OPTS="${JAVA_OPTS} -splash:${LIB}/splash.png \
        -Dapple.laf.useScreenMenuBar=true \
        -Dapple.awt.antialiasing=on \
        -Dapple.awt.textantialiasing=on \
        -Dapple.awt.graphics.UseQuartz=true \
        -Dcom.apple.mrj.application.apple.menu.about.name=IFSExplorer \
        -Xdock:name=IFSExplorer"

##
# Exexcute Java
##
java -cp ".:${LIB}/*" ${JAVA_OPTS} iterator.Explorer "$@" 2>> explorer.log
