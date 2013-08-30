@ECHO OFF
REM
REM Copyright 2012-2013 by Andrew Kennedy; All Rights Reserved
REM
REM Iterated Function System Explorer
REM
REM Author: andrew.international@gmail.com
REM Last Modified: 2013-08-30
REM

REM
REM Setup
REM
SET BIN=%~p0%
SET LIB=%BIN:bin=lib%
SET JAVA_OPTS=%JAVA_OPTS%

REM
REM Exexcute Java
REM
java -cp ".;%LIB%\*" %JAVA_OPTS% iterator.Explorer %*
