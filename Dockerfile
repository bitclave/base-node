FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD base-node.jar /base-node.jar
ENTRYPOINT  exec java $JAVA_OPTS -jar /base-node.jar
