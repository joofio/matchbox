# matchbox - playground with the hapi fhir spring boot server

plain spring-boot-server based as provided by [hapi-fhir spring boot examles](https://github.com/jamesagnew/hapi-fhir/tree/master/hapi-fhir-spring-boot)
* prequisites to build are [pullrequest for org.hl7.fhir.core](https://github.com/hapifhir/org.hl7.fhir.core/pull/11) to be based on version 3.8.0-snaphsot and [branch of forked hpi-fhir](https://github.com/ahdis/hapi-fhir/tree/oliveregger_fhircore
* currently there is a kind of circular dependency on hapi-fhir-base which is included in hapi-fhir but needed by org.hl7.fhir.core [see also comment on pull request](https://github.com/hapifhir/org.hl7.fhir.core/pull/11)

**Feature experimental**

 Operation $convert on Resource @link https://www.hl7.org/fhir/resource-operation-convert.html
 * - Convertion between fhir versions is handled with VersionInterceptor 
 * - Convertion between fhir+xml and fhir+json is automatically handled in the hapi-fhir base request handling ...

FHIR Mapping Language support based on the FHIR Java reference implementation:
* prototype support for the [$transform operation for StructureMap](http://www.hl7.org/fhir/structuremap-operation-transform.html)
* convenience operation to $parse a StructureMap from a text respresentation to a StructureMap

FHIR RI Validation Support for the $validate operation
* using the org.fhir.core validation RI infrastructure
* capability to load an implementation guide (currently "http://build.fhir.org/ig/hl7ch/ch-core/ is fix configured) 

Health Checks provided by spring-boot
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

http://localhost:8080/r4/metadata

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
docker build -t eu.gcr.io/${PROJECT_ID}/matchbox:v2 .
docker tag matchbox eu.gcr.io/${PROJECT_ID}/matchbox:v2
docker push eu.gcr.io/${PROJECT_ID}/matchbox:v2

gcloud container clusters get-credentials cluster-europe-west3a-fhir-ch

kubectl create -f matchbox.yaml
kubectl get pods


[see tutorial](https://cloud.google.com/kubernetes-engine/docs/tutorials/hello-app?hl=de)
[container registry](https://console.cloud.google.com/gcr/images/fhir-ch?project=fhir-ch&authuser=1&folder&hl=de&organizationId=22040958741)



tbd:
[routing](https://medium.com/google-cloud/kubernetes-routing-internal-services-through-fqdn-d98db92b79d3)