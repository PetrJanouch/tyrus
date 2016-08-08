#!/bin/bash -x

M2_REPO=/Users/petr/.m2/repository

export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-9/Contents/Home"
$JAVA_HOME/bin/javac -d /Users/petr/Oracle/Tyrus/tyrus/core/target/test-classes\
 -mp /Users/petr/Oracle/Tyrus/tyrus/core/target/classes:\
/Users/petr/.m2/repository/org/glassfish/tyrus/tyrus-spi/2.0-SNAPSHOT/tyrus-spi-2.0-SNAPSHOT.jar:\
/Users/petr/.m2/repository/javax/websocket/javax.websocket-api/1.1/javax.websocket-api-1.1.jar:\
/Users/petr/.m2/repository/org/osgi/org.osgi.core/4.2.0/org.osgi.core-4.2.0.jar:\
 -cp /Users/petr/.m2/repository/junit/junit/4.10/junit-4.10.jar:\
/Users/petr/.m2/repository/org/hamcrest/hamcrest-core/1.1/hamcrest-core-1.1.jar:\
 -sourcepath /Users/petr/Oracle/Tyrus/tyrus/core/src/main/java:\
/Users/petr/Oracle/Tyrus/tyrus/core/src/test/java:\
/Users/petr/Oracle/Tyrus/tyrus/core/target/generated-sources/rsrc-gen:\
/Users/petr/Oracle/Tyrus/tyrus/core/target/generated-sources/annotations:\
 -g -nowarn -target 9 -source 9 -encoding UTF-8 $(find src/test -name "*.java")

$JAVA_HOME/bin/java -mp $M2_REPO/org/glassfish/tyrus/tyrus-spi/2.0-SNAPSHOT/tyrus-spi-2.0-SNAPSHOT.jar:\
$M2_REPO//org/osgi/org.osgi.core/4.2.0/org.osgi.core-4.2.0.jar:\
/Users/petr/Oracle/Tyrus/tyrus/core/target/classes:\
$M2_REPO//javax/websocket/javax.websocket-api/1.1/javax.websocket-api-1.1.jar\
 -cp $M2_REPO//junit/junit/4.10/junit-4.10.jar:\
 $M2_REPO/org/hamcrest/hamcrest-core/1.1/hamcrest-core-1.1.jar:\
 /Users/petr/Oracle/Tyrus/tyrus/core/target/test-classes
  org.junit.runner.JUnitCore org.glassfish.tyrus.core.TyrusSessionTest