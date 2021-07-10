FROM openjdk:8-jdk-alpine

RUN apk update && \
    apk add apache-ant && \
    apk add ttf-dejavu && \
    apk add subversion && \
    apk add git && \
    rm /var/cache/apk/*

COPY . /josm

RUN mkdir -p /josm/test/report

CMD cd /josm && \
    ant test-html -DnoJavaFX=true
