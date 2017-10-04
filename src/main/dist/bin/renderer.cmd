@ECHO OFF
REM
REM Copyright 2012-2017 by Andrew Kennedy.
REM http://www.apache.org/licenses/LICENSE-2.0
REM
REM Iterated Function System Renderer
REM
REM andrew.international+iterator@gmail.com
REM

REM
REM Setup
REM
SET BIN=%~p0%
SET LIB=%BIN:bin=lib%
IF NOT DEFINED JAVA_MEM SET JAVA_MEM="-Xms1g -Xmx4g"
SET JAVA_OPTS="%JAVA_OPTS% %JAVA_MEM%"

REM
REM Exexcute Java
REM
java -cp ".;%LIB%\*" %JAVA_OPTS% iterator.Renderer %* 2>> renderer.log
