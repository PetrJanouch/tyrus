export MVN_OPTS="-Dmaven.compiler.fork -Dmaven.compiler.executable=/Library/Java/JavaVirtualMachines/jdk-9/Contents/Home/bin/javac"
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-9/Contents/Home"
mvn -X clean install -Dmaven.test.skip=true

#$JAVA_HOME/bin/javac -d /Users/petr/Oracle/Tyrus/tyrus/spi/target/classes -modulepath /Users/petr/Oracle/Tyrus/tyrus/spi/target/classes:/Users/petr/.m2/repository/javax/websocket/javax.websocket-api/1.1/javax.websocket-api-1.1.jar: -sourcepath /Users/petr/Oracle/Tyrus/tyrus/spi/src/main/java:/Users/petr/Oracle/Tyrus/tyrus/spi/target/generated-sources/annotations: -s /Users/petr/Oracle/Tyrus/tyrus/spi/target/generated-sources/annotations -g -nowarn -target 9 -source 9 -encoding UTF-8 $(find . -name "*.java")
