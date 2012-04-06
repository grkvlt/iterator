#!/bin/bash

CLASSPATH=./target/iterator-1.0.0-SNAPSHOT.jar
CLASSPATH=${CLASSPATH}:${HOME}/.m2/repository/org/slf4j/slf4j-api/1.6.1/slf4j-api-1.6.1.jar
CLASSPATH=${CLASSPATH}:${HOME}/.m2/repository/org/slf4j/slf4j-log4j12/1.6.1/slf4j-log4j12-1.6.1.jar
CLASSPATH=${CLASSPATH}:${HOME}/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar
CLASSPATH=${CLASSPATH}:${HOME}/.m2/repository/com/google/guava/guava/11.0.2/guava-11.0.2.jar

JAVA_OPTS="-Dapple.laf.useScreenMenuBar=true"

java -cp ${CLASSPATH} iterator.Explorer $*
