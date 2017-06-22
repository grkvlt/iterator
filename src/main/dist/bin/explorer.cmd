@ECHO OFF
REM
REM Copyright 2012-2017 by Andrew Kennedy.
REM http://www.apache.org/licenses/LICENSE-2.0
REM
REM Iterated Function System Explorer
REM
REM andrew.international+iterator@gmail.com
REM

REM
REM Setup
REM
SET BIN=%~p0%
SET LIB=%BIN:bin=lib%
SET JAVA_OPTS="%JAVA_OPTS% -splash:%LIB%\splash.png"

REM
REM Exexcute Java
REM
java -cp ".;%LIB%\*" %JAVA_OPTS% iterator.Explorer %* 2>> explorer.log
