FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 8080
VOLUME /tmp

ARG JAR_FILE=target/matchbox-0.6.0-SNAPSHOT.jar

COPY ${JAR_FILE} /app.jar
COPY packages /packages

RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id hl7.fhir.cda -v dev -tgz http://build.fhir.org/ig/ahdis/cda-core-2.0/branches/pullrequests/package.tgz -desc hl7.fhir.cda
RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id hl7.fhir.r4.core -v 4.0.1
RUN rm -rf /packages

ENTRYPOINT java -Xmx2200m -Xshareclasses -Xquickstart -jar /app.jar