# matchbox - playground with the hapi fhir spring boot server

plain spring-boot-server based as provided by [hapi-fhir spring boot examples](https://github.com/jamesagnew/hapi-fhir/tree/master/hapi-fhir-spring-boot)

**Feature experimental**

Operation $convert on Resource @link https://www.hl7.org/fhir/resource-operation-convert.html
 * Convertion between fhir+xml and fhir+json is automatically handled in the hapi-fhir base request handling

FHIR Mapping Language support based on the FHIR Java reference implementation:
* support for the [$transform operation for StructureMap](http://www.hl7.org/fhir/structuremap-operation-transform.html)
* support for conversion between CDA and FHIR (and back)

FHIR RI Validation Support for the $validate operation
* using the org.fhir.core validation RI infrastructure
* capability to load an implementation guide into the validation infrastructure
* provid a system wide $validate operation with profile as query parameter

Health Checks provided by spring-boot
* http://localhost:8080/actuator/health
* http://localhost:8080/r4/metadata 


## build with maven

```
mvn package
```

## execute
```
java -jar target/matchbox-0.8.7-SNAPSHOT.jar
```

http://localhost:8080/r4/metadata



## build docker for gcloud/kubernetes

IMORTANT: adjust jar in Dockerfile

export PROJECT_ID="$(gcloud config get-value project -q)"


docker build -t matchbox . 
docker tag matchbox eu.gcr.io/fhir-ch/matchbox:v0810
docker push eu.gcr.io/fhir-ch/matchbox:v0810

docker run -d --name matchbox -p 8080:8080 matchbox --memory="5G" --cpus="1"
docker logs matchbox


gcloud container clusters get-credentials cluster-europe-west3a-fhir-ch

kubectl create -f matchbox.yaml
kubectl get pods

kubectl apply -f matchbox-ahdis.yaml 

[see tutorial](https://cloud.google.com/kubernetes-engine/docs/tutorials/hello-app?hl=de)
[container registry](https://console.cloud.google.com/gcr/images/fhir-ch?project=fhir-ch&authuser=1&folder&hl=de&organizationId=22040958741)



tbd:
[routing](https://medium.com/google-cloud/kubernetes-routing-internal-services-through-fqdn-d98db92b79d3)



Matchbox memory behaviour:

Default configuration goes up to around 2.7 GiB after $convert operation
changed to FROM adoptopenjdk/openjdk11-openj9:alpine-slim -> reduced around to 2.16 GIB

Startup 810 MiB
Loading all IG's 2.021GiB 
--> need to investigate how we can make this less memory intensive

 : Started MatchboxApplication in 92.94 seconds (JVM running for 94.398)
