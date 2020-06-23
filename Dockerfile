FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 8080
VOLUME /tmp

ARG JAR_FILE=target/matchbox-0.8.9-SNAPSHOT.jar

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

COPY ${JAR_FILE} /app.jar
COPY packages /packages

RUN java -Xmx1G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id hl7.fhir.cda -v dev -tgz http://build.fhir.org/ig/ahdis/cda-core-2.0/branches/pullrequests/package.tgz -desc hl7.fhir.cda
RUN java -Xmx1G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id hl7.fhir.r4.core -v 4.0.1
RUN java -Xmx1G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id ch.fhir.ig.ch-emed -v 0.1.0
RUN java -Xmx1G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id ch.fhir.ig.ch-core -v 1.0.0
RUN java -Xmx1G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id ch.fhir.ig.ch-epr-term -v 2.0.3
RUN java -Xmx1G -Xms1G -cp /app.jar -Dloader.main=ch.ahdis.matchbox.util.PackageCacheInitializer org.springframework.boot.loader.PropertiesLauncher -id ch.fhir.ig.ch-atc -v 3.1.0 
RUN rm -rf /packages

ENTRYPOINT java -Xmx1G -Xshareclasses -Xquickstart -jar /app.jar