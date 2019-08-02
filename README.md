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
cp /Users/oliveregger/Documents/github/chmed16af/output/package.tgz ./packages/ch.mediplan.chmed16af.tgz
cp /Users/oliveregger/Documents/github/fhir.versions.r3r4/output/package.tgz ./packages/fhir.versions.r3r4.tgz
cp /Users/oliveregger/Documents/github/ch-core/output/package.tgz ./packages/ch.fhir.ig.core.tgz

mvn clean install
docker build . --build-arg JAR_FILE=target/matchbox-0.3.0-SNAPSHOT.jar -t matchbox
docker run -d --name matchbox -p 8080:8080 matchbox --memory="5G" --cpus="1"
docker logs matchbox
```


## build docker for gcloud/kubernetes

export PROJECT_ID="$(gcloud config get-value project -q)"
docker build -t eu.gcr.io/${PROJECT_ID}/matchbox:v7 .
docker tag matchbox eu.gcr.io/${PROJECT_ID}/matchbox:v7
docker push eu.gcr.io/${PROJECT_ID}/matchbox:v7

gcloud container clusters get-credentials cluster-europe-west3a-fhir-ch

kubectl create -f matchbox.yaml
kubectl get pods

kubectl apply -f matchbox.yaml 

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
