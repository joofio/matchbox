FROM adoptopenjdk/openjdk11:latest
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 8080
VOLUME /tmp
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-Xmx2G", "-Xms1G","-cp","app:app/lib/*","ch.ahdis.matchbox.MatchboxApplication"]



