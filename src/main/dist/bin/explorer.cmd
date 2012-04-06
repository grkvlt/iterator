@ECHO OFF
REM
REM Copyright 2012 by Andrew Kennedy; All Rights Reserved
REM
REM Iterated Function System Explorer
REM
REM Author: andrew.international@gmail.com
REM Last Modified: 2012-04-06
REM

REM
REM Setup
REM
SET BIN=%~p0%
SET LIB=%BIN:bin=lib%
SET ETC=%BIN:bin=etc%
SET ETC=%ETC:\=/%

REM
REM Exexcute Java
REM
java -cp ".;%LIB%\*" %JAVA_OPTS% iterator.Explorer %*
