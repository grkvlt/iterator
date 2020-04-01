#!/bin/bash
##
# Copyright 2012-2020 by Andrew Kennedy.
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
JAVA_OPTS="${JAVA_OPTS} ${JAVA_MEM:--Xms8g -Xmx8g}
        -Dapple.awt.graphics.UseQuartz=true
        -Xdock:name=IFSAnimator"

##
# Exexcute Java
##
java -cp ".:${LIB}/*" ${JAVA_OPTS} iterator.Animator "$@" 2>> animator.log
