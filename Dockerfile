FROM eu.gcr.io/fhir-ch/matchbox-nopreload:latest
EXPOSE 8080
VOLUME /tmp

COPY my-conf /config

#ENTRYPOINT java -Xmx1G -Xshareclasses -Xquickstart -jar /app.jar -Dspring.config.additional-location=optional:file:/config/application.yml