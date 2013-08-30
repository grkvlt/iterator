#!/bin/bash
##
# Copyright 2012-2013 by Andrew Kennedy; All Rights Reserved
#
# Iterated Function System Explorer
#
# Author: andrew.international@gmail.com
# Last Modified: 2013-08-30
##
#set -x # debug

##
# Setup
##
BIN=$(dirname $0)
PARENT=$(echo "${BIN}/.." | sed -e "s/\/bin\/..//")
LIB="${PARENT}/lib"
JAVA_OPTS="${JAVA_OPTS} -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true"

##
# Exexcute Java
##
java -cp ".:${LIB}/*" ${JAVA_OPTS} iterator.Explorer $*
