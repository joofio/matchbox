# matchbox - CDA/FHIR Mapping with FHIR Mapping Language and the CDA Logical model

The Java Reference Implementation provides support for the [FHIR Mapping Language](https://www.hl7.org/fhir/mapping-language.html)
to transform [Logical Models](https://www.hl7.org/fhir/mapping-language.html) to FHIR and back.

## FHIR R4 transformation

The following operations available with matchbox:

start up matchbox, which will run a lcoal server on port 8080
```
java -cp matchbox-0.6.0-SNAPSHOT.jar  
```

1. parse and register a map with matchbox

```
POST http://localhost:8080/r4/StructureMap HTTP/1.1
Accept: application/fhir+xml;fhirVersion=4.0
Content-Type: text/fhir-mapping

map "http:/ahdis.ch/matchbox/fml/qr2patgender" = "qr2patgender"
uses "http://hl7.org/fhir/StructureDefinition/QuestionnaireResponse" alias QuestionnaireResponse as source
uses "http://hl7.org/fhir/StructureDefinition/Patient" alias Patient as target
group QuestionnaireResponse(source src : QuestionnaireResponse, target tgt : Patient) {
  src.item as item -> tgt as patient then item(item, patient);
}
group item(source src, target tgt: Patient) {
  src.item as item where linkId.value in ('patient.sex') -> tgt.gender = (item.answer.valueString);
}
```

If successful the map will be available on matchbox with the specified map url, if not an OperationOutcome error will indicate the problem parsing the textual description of the map.


2. transform content with a map

the $transform operation requires a source request parameter with the URI of the map, the content to transform can be provided in the body with specifying the Content-Type 

```
POST http://localhost:8080/r4/StructureMap/$transform?source=http:/ahdis.ch/matchbox/fml/qr2patgender
Accept: application/fhir+xml;fhirVersion=4.0
Content-Type: application/fhir+json;fhirVersion=4.0

{
  "resourceType": "QuestionnaireResponse",
  "status": "in-progress",
  "item": [
    {
      "linkId": "patient",
      "text": "Patient",
      "item": [
        {
          "linkId": "patient.sex",
          "text": "Geschlecht",
          "answer": [
            {
              "valueString": "female"
            }
          ]
        }
      ]
    }
  ]
}
```

The above two operations allows transforms between FHIR Release 4.

## CDA to FHIR transformations

A [FHIR Logical model for CDA](https://github.com/HL7/cda-core-2.0) is in development which can be used
to convert between FHIR and cda. There are some open pull requests, if you want to work with these

See the following steps to develop with the [cda/fhir maps](https://github.com/ahdis/cda-fhir-maps)






