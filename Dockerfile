FROM eu.gcr.io/fhir-ch/matchbox-nopreload:latest

#FROM adoptopenjdk/openjdk11-openj9:alpine-slim

#ARG JAR_FILE=target/matchbox-0.9.9-SNAPSHOT.jar

#ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

#COPY ${JAR_FILE} /app.jar
COPY my-conf /config
COPY start-heroku.sh .
#CMD ["java", "-Dserver.port=", "${PORT}" , "-Xmx1G", "-Xshareclasses", "-Xquickstart", "-jar", "/app.jar"]
CMD ["/bin/sh","-c","java", "${JAVA_OPTS}", "-Xshareclasses", "-Xquickstart -jar",  "/app.jar", "-Dserver.port=${PORT}"]
#https://stackoverflow.com/questions/43975939/heroku-run-docker-image-with-port-parameter