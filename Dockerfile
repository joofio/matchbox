FROM eu.gcr.io/fhir-ch/matchbox-nopreload:latest

#FROM adoptopenjdk/openjdk11-openj9:alpine-slim

#ARG JAR_FILE=target/matchbox-0.9.9-SNAPSHOT.jar

#ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

#COPY ${JAR_FILE} /app.jar
  
CMD ["java", "-Dserver.port=", "${PORT}" , "-Xmx1G", "-Xshareclasses", "-Xquickstart", "-jar", "/app.jar"]
#CMD ["java" , "-Xmx1G", "-Xshareclasses", "-Xquickstart", "-jar", "/app.jar"]
