FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD build/libs/base-node.jar /base-node.jar
ENTRYPOINT ["java","-jar","/base-node.jar"]
