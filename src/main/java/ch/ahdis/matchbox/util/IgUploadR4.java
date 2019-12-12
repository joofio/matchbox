//package ch.ahdis.matchbox.util;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.io.FileUtils;
//import org.hl7.fhir.instance.model.api.IIdType;
//import org.hl7.fhir.r4.context.SimpleWorkerContext;
//import org.hl7.fhir.r4.model.Bundle;
//import org.hl7.fhir.r4.model.ImplementationGuide;
//import org.hl7.fhir.r4.model.ImplementationGuide.ImplementationGuideDefinitionResourceComponent;
//import org.hl7.fhir.r4.model.MetadataResource;
//import org.hl7.fhir.r4.model.Resource;
//import org.hl7.fhir.r4.validation.ValidationEngine;
//import org.hl7.fhir.utilities.TextFile;
//import org.hl7.fhir.utilities.VersionUtil;
//import org.hl7.fhir.utilities.cache.NpmPackage;
//
//import ca.uhn.fhir.context.FhirContext;
//import ca.uhn.fhir.context.FhirVersionEnum;
//import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
//import ca.uhn.fhir.rest.client.api.IGenericClient;
//import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
//
//// FIXME: VersionConverter will not yet pick up development done for matchbox this is yet hidden in valdiation engine
//public class IgUploadR4 extends ValidationEngine {
//
//	public boolean initPassed = false;
//
//	public IgUploadR4() throws Exception {
//		super("hl7.fhir.core#4.0.1");
//	}
//
//	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IgUploadR4.class);
//
//	private Bundle getBundle(String implementationGuide) {
//		Bundle bundle = new Bundle();
//		bundle.setType(Bundle.BundleType.TRANSACTION);
//		SimpleWorkerContext ctx = getContext();
//		List<MetadataResource> conformanceInitialResource = ctx.allConformanceResources();
//		try {
//			initPassed = true;
//			loadIg(implementationGuide);
//		} catch (Exception e) {
//			log.error("error loading R4 or ImplementationGuide", e);
//			return null;
//		}
//		List<MetadataResource> conformanceResources = ctx.allConformanceResources();
//		boolean found = false;
//		for (MetadataResource conformanceResource : conformanceResources) {
//			if (!conformanceInitialResource.contains(conformanceResource)
//					&& conformanceResource instanceof ImplementationGuide) {
//				ImplementationGuide ig = (ImplementationGuide) conformanceResource;
//				log.info("");
//				log.info("Implementation guide" + ig + " " + (found ? "ignored" : "used"));
//				if (ig.getDefinition() != null && !found) {
//					found = true; // there are four versions in ch.core, why is that?
//					List<ImplementationGuideDefinitionResourceComponent> resources = ig.getDefinition().getResource();
//					if (resources != null) {
//						for (ImplementationGuideDefinitionResourceComponent resourceComponent : resources) {
//							if (resourceComponent.getReference() != null && resourceComponent.hasExampleCanonicalType()) {
//								IIdType resourceIdType = resourceComponent.getReference().getReferenceElement();
//								Resource resource = ctx.fetchResourceById(resourceIdType.getResourceType(),
//										resourceIdType.getValueAsString());
//								org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry = bundle.addEntry();
//								entry.getRequest().setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST);
//								entry.setResource(resource);
//							}
//						}
//					}
//				}
//			}
//		}
//		return bundle;
//	}
//
//	/**
//	 * overwritten because base class loads only conformance resources
//	 */
//	public Map<String, byte[]> loadPackage(NpmPackage pi) throws IOException {
//		if (!initPassed) {
//			return super.loadPackage(pi);
//		} else {
//			Map<String, byte[]> res = new HashMap<String, byte[]>();
//			for (String s : pi.list("package")) {
//				if (s.contains("-"))
//					res.put(s, TextFile.streamToBytes(pi.load("package", s)));
//			}
//			String ini = "[FHIR]\r\nversion=" + pi.fhirVersion() + "\r\n";
//			res.put("version.info", ini.getBytes());
//			return res;
//		}
//	}
//
//	private boolean upload(Bundle bundle, String targetServer) {
//		log.info("Uploading bundle to server: " + targetServer);
//
//		FhirContext contextR4 = FhirVersionEnum.R4.newContext();
//
//		if (targetServer.startsWith("file://")) {
//			String path = targetServer.substring("file://".length());
//			log.info("Writing bundle to: {}", path);
//			File file = new File(path);
//			if (file.exists()) {
//				file.delete();
//			}
//			FileWriter w;
//			try {
//				w = new FileWriter(file, false);
//				w.append(contextR4.newXmlParser().encodeResourceToString(bundle));
//				w.close();
//			} catch (IOException e) {
//				log.error("Failed to write bundle", e);
//				log.error(contextR4.newXmlParser().encodeResourceToString(bundle));
//			}
//		} else {
//			IGenericClient fhirClient = contextR4.newRestfulGenericClient(targetServer);
//
//			fhirClient.registerInterceptor(new GZipContentInterceptor());
//			Bundle responseBundle = null;
//			long start = System.currentTimeMillis();
//			try {
//				responseBundle = fhirClient.transaction().withBundle(bundle).execute();
//			} catch (BaseServerResponseException e) {
//				log.error("Failed to upload bundle:HTTP " + e.getStatusCode() + ": " + e.getMessage());
//				log.error(contextR4.newXmlParser().encodeResourceToString(bundle));
//				return false;
//			}
//			long delay = System.currentTimeMillis() - start;
//			log.info("Finished uploading bundle to server (took {} ms)", delay);
//			log.info(contextR4.newXmlParser().encodeResourceToString(responseBundle));
//		}
//
//		return true;
//	}
//
//	public boolean upload(String implementationGuide, String targetServer) {
//		Bundle bundle = getBundle(implementationGuide);
//		if (bundle != null && bundle.getEntry().size() > 0) {
//			return upload(bundle, targetServer);
//		}
//		log.info("Bundle empty");
//		return false;
//	}
//
//	public static void main(String[] args) throws Exception {
//
//		System.out.println("Matchbox IgUploadR4");
//		if (hasParam(args, "-ig") && hasParam(args, "-target")) {
//			String ig = getParam(args, "-ig");
//			String target = getParam(args, "-target");
//			IgUploadR4 igupload = new IgUploadR4();
//			igupload.upload(ig, target);
//
//		} else {
//			System.out.println("-ig or -target missing.");
//			System.out.println("-ig [package|file|url]: an IG or profile definition to load. Can be ");
//			System.out.println("     the URL of an implementation guide or a package ([id]-[ver]) for");
//			System.out.println("     a built implementation guide or a local folder that contains a");
//			System.out.println("     set of conformance resources.");
//			System.out.println("-target [url]: taget fhir server");
//		}
//
//	}
//
//	private static boolean hasParam(String[] args, String param) {
//		for (String a : args)
//			if (a.equals(param))
//				return true;
//		return false;
//	}
//
//	private static String getParam(String[] args, String param) {
//		for (int i = 0; i < args.length - 1; i++)
//			if (args[i].equals(param))
//				return args[i + 1];
//		return null;
//	}
//
//};
