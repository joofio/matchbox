# matchbox
playground with the hapi fhir spring boot server

* plain spring-boot-server based as provided by [hapi-fhir spring boot examles](https://github.com/jamesagnew/hapi-fhir/tree/master/hapi-fhir-spring-boot)

FHIR Mapping Language support based on the FHIR Java reference implementation:
* prototype support for the [$transform operation for StructureMap](http://www.hl7.org/fhir/structuremap-operation-transform.html)
* convenience operation to $parse a StructureMap from a text respresentation to a StructureMap
* using the org.fhir.core validation infrastructure

* http://localhost:8080/actuator/health
* http://localhost:8080/r4/metadata (readyness)

## build with maven
```
.
/mvnw package
```

## execute
```
java -jar target/matchbox-0.1.0-SNAPSHOT.jar
```

## docker build (for Dockerfile.simple)
```
docker build . --build-arg JAR_FILE=./target/matchbox-0.0.1-SNAPSHOT.jar -t matchbox
```

## docker run
```
docker run -d --name matchbox -p 8080:8080 matchbox --memory="5G" --cpus="1"
docker logs matchbox
```

## build through mvn
```
export DOCKER_HOST=tcp://localhost:2375
./mvnw install dockerfile:build
```

currently an error:
[ERROR] Failed to execute goal com.spotify:dockerfile-maven-plugin:1.4.9:build (default-cli) on project matchbox: Could not build image: java.util.concurrent.ExecutionException: com.spotify.docker.client.shaded.javax.ws.rs.ProcessingException: com.spotify.docker.client.shaded.org.apache.http.conn.HttpHostConnectException: Connect to localhost:2375 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused (Connection refused) -> [Help 1]

finish the dockerbuild after the error
docker build . -t matchbox
[background info on spring.io](https://spring.io/guides/gs/spring-boot-docker/)


## build docker for gcloud/kubernetes

export PROJECT_ID="$(gcloud config get-value project -q)"
docker build -t eu.gcr.io/${PROJECT_ID}/matchbox:v1 .
docker tag matchbox eu.gcr.io/${PROJECT_ID}/matchbox:v1
docker push eu.gcr.io/${PROJECT_ID}/matchbox:v1

gcloud container clusters get-credentials cluster-europe-west3a-fhir-ch

kubectl create -f matchbox.yaml
kubectl get pods


[see tutorial](https://cloud.google.com/kubernetes-engine/docs/tutorials/hello-app?hl=de)
[container registry](https://console.cloud.google.com/gcr/images/fhir-ch?project=fhir-ch&authuser=1&folder&hl=de&organizationId=22040958741)



tbd:
[routing](https://medium.com/google-cloud/kubernetes-routing-internal-services-through-fqdn-d98db92b79d3)