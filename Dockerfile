FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD build/libs/base-node.jar /base-node.jar
ADD build/libs/base-node.jar1 /base-node.jar1
ENTRYPOINT  exec java $JAVA_OPTS -jar /base-node.jar
