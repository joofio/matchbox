FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 8080
VOLUME /tmp

ARG JAR_FILE=target/matchbox-0.3.0-SNAPSHOT.jar

COPY ${JAR_FILE} /app.jar
COPY packages /packages

RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id hl7.fhir.core -v 3.0.1
RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id hl7.fhir.core -v 4.0.0
RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id ch.mediplan.chmed16af -v dev -tgz ./packages/ch.mediplan.chmed16af.tgz
RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id fhir.versions.r3r4 -v dev -tgz /packages/fhir.versions.r3r4.tgz
RUN java -Xmx2G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id ch.fhir.ig.core -v dev -tgz /packages/ch.fhir.ig.core.tgz
RUN rm -rf /packages

ENTRYPOINT java -Xmx2200m -Xshareclasses -Xquickstart -jar /app.jar