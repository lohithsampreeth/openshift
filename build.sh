#!/bin/bash
buildpath=$1
#git clone $url
cd $buildpath
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export M2_HOME=/opt/maven
export MAVEN_HOME=/opt/maven
export PATH=${M2_HOME}/bin:${PATH}

#/opt/maven/bin/mvn  clean install
#/opt/maven/bin/mvn  clean deploy sonar:sonar
mvn clean deploy sonar:sonar
## Building Docker
currentdirect=`pwd`
#dockerfile="/orover/"
docker build -t spring-boot/docker-java-jar-build-demo:2.0-SNAPSHOT $currentdirect
