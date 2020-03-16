FROM alpine:3.10

RUN apk add --no-cache curl

ENV VERSION 1.6.2
ENV JAR jolokia-jvm-$VERSION-agent.jar
ENV MAVEN_REPOSITORY https://repo1.maven.org/maven2

RUN mkdir -p /opt/jolokia/

RUN curl -L $MAVEN_REPOSITORY/org/jolokia/jolokia-jvm/$VERSION/$JAR -o /opt/jolokia/$JAR

CMD /bin/sh