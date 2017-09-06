#!/bin/bash
##
# Copyright 2012-2017 by Andrew Kennedy.
# http://www.apache.org/licenses/LICENSE-2.0
#
# Iterated Function System Animator
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
JAVA_OPTS="${JAVA_OPTS} ${JAVA_MEM:--Xms1g -Xmx4g} -splash:${LIB}/splash.png \
        -Dapple.laf.useScreenMenuBar=true \
        -Dapple.awt.antialiasing=on \
        -Dapple.awt.textantialiasing=on \
        -Dapple.awt.graphics.UseQuartz=true \
        -Dcom.apple.mrj.application.apple.menu.about.name=IFSAnimator \
        -Xdock:name=IFSAnimator"

##
# Exexcute Java
##
java -cp ".:${LIB}/*" ${JAVA_OPTS} iterator.Animator "$@" 2>> animator.log
