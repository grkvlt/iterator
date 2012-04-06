#!/bin/bash
##
# Copyright 2012 by Andrew Kennedy; All Rights Reserved
#
# Iterated Function System Explorer
#
# Author: andrew.international@gmail.com
# Last Modified: 2012-04-06
##
#set -x # debug

##
# Setup
##
BIN=$(dirname $0)
PARENT=$(echo "${BIN}/.." | sed -e "s/\/bin\/..//")
LIB="${PARENT}/lib"
ETC="${PARENT}/etc"
JAVA_OPTS="${JAVA_OPTS} -Dapple.laf.useScreenMenuBar=true -Xdock:name=IFSExplorer"

##
# Exexcute Java
##
java -cp ".:${LIB}/*" ${JAVA_OPTS} iterator.Explorer $*