#!/bin/bash
##
# Copyright 2012-2020 by Andrew Kennedy.
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
JAVA_OPTS="${JAVA_OPTS} ${JAVA_MEM:--Xms4g -Xmx4g}
        -splash:${LIB}/splash.png
        -Dapple.laf.useScreenMenuBar=true
        -Dapple.awt.antialiasing=on
        -Dapple.awt.textantialiasing=on
        -Dapple.awt.graphics.UseQuartz=true
        -Dcom.apple.mrj.application.apple.menu.about.name=IFSExplorer
        -Xdock:name=IFSExplorer"

##
# Exexcute Java
##
if [ "${DEBUG}" ] ; then
	java -cp ".:${LIB}/*" ${JAVA_OPTS} -Dexplorer.debug=true iterator.Explorer "$@" 2>&1 | tee -a explorer.log
else
	java -cp ".:${LIB}/*" ${JAVA_OPTS} iterator.Explorer "$@" 2>> explorer.log | tee -a explorer.log
fi
