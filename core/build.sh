export MVN_OPTS="-Dmaven.compiler.fork -Dmaven.compiler.executable=/Library/Java/JavaVirtualMachines/jdk-9/Contents/Home/bin/javac"
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-9/Contents/Home"
mvn -X clean install -Dmaven.test.skip=true
