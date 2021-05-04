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
* provide a system wide $validate operation with profile as query parameter

Health Checks provided by spring-boot
* http://localhost:8080/actuator/health
* http://localhost:8080/r4/metadata 

## using matchbox

### test version, online instance
there is a test instance available at [http://test.ahdis.ch/r4](http://test.ahdis.ch/r4/metadata)

### docker version

a preconfigured docker container with the swiss ig's is here

```
docker pull eu.gcr.io/fhir-ch/matchbox:v091
docker run -d --name matchbox -p 8080:8080 eu.gcr.io/fhir-ch/matchbox:v091
docker logs matchbox
```

## build with maven

Note: The build is depending on hapi snapshot version, it might break.

```
mvn package
java -jar target/matchbox-0.9.1-SNAPSHOT.jar
```

http://localhost:8080/r4/metadata



## internal: build docker for gcloud/kubernetes

IMORTANT: adjust jar in Dockerfile

docker build -t matchbox . 
docker tag matchbox eu.gcr.io/fhir-ch/matchbox:v093
docker push eu.gcr.io/fhir-ch/matchbox:v093


docker run -d --name matchbox -p 8080:8080 --memory="5G" --cpus="1" matchbox


## developing matchbox

1. to develop with matchbox you need to check out the **dev** branches of the forked [org.hl7.fhir.core](https://github.com/ahdis/org.hl7.fhir.core/tree/dev) and [hapi-fhir](https://github.com/ahdis/hapi-fhir/tree/dev) project
2. run mvn clean install -DskipTests in org.hl7.fhir.core and hapi-fhir (this will install local maven snapshots in your system)
3. checkout matchbox master and commpile it with mvn or your favorite development setup
