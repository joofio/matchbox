package ch.ahdis.matchbox.interceptor;

/*
 * #%L
 * Matchbox Server
 * %%
 * Copyright (C) 2018 - 2019 ahdis
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.convertors.NullVersionConverterAdvisor30;
import org.hl7.fhir.convertors.VersionConvertor_10_30;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

/**
 * VersionInterceptor converts the FHIR resources in request/response to the current hapi-fhir server version 
 * For requests the FHIR version of the resource has to be specified in the Content-Type http Header
 * For responses the FHIR version of the resource has to be specified in the Accept http Header
 * 
 * POST {{host}}/$convert HTTP/1.1
 * Accept: application/fhir+json
 * Content-Type: application/fhir+xml;fhirVersion=3.0
 *
 * TODO: No testcases written yet
 *
 * TODO: different versions for Accept-Header and Content-Type are not allowed according to the FHIR specification, 
 * need to raise a gforge for that: see https://www.hl7.org/fhir/http.html#version-parameter
 * 
 * TODO: conversion is done using the VersionConvertor_xx_xx, difference to using directly the RXTOX Maps based on
 * StructureMap should be evaluated and considered for further conversions (looks like VersionConvertor_xx_xx were originally 
 * created out of StructureMaps?)
 * 
 * inspired and credits to hapi-fhir @see ca.uhn.hapi.converters.server.VersionedApiConverterInterceptor
 *
 */
public class VersionInterceptor extends InterceptorAdapter {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VersionInterceptor.class);
	private VersionConvertor_10_30 versionConvertor_10_30;
	private FhirVersionEnum serverVersion;
	private static Map<FhirVersionEnum, FhirContext> myFhirContextMap = Collections.synchronizedMap(new HashMap<FhirVersionEnum, FhirContext>());
	
	public VersionInterceptor() {
		super();
	}
	
	private FhirVersionEnum extractFhirVersion(String header) {
		if (header==null) {
			return null;
		}
		StringTokenizer tok = new StringTokenizer(header, ";");
		String wantVersionString = null;
		while (tok.hasMoreTokens()) {
			String next = tok.nextToken().trim();
			if (next.startsWith("fhirVersion=")) {
				wantVersionString = next.substring("fhirVersion=".length()).trim();
				break;
			}
		}
		if (isNotBlank(wantVersionString)) {
			return FhirVersionEnum.forVersionString(wantVersionString);
		}
		return null;
	}
	
	public static FhirContext getContextForVersion(FhirContext theContext, FhirVersionEnum theForVersion) {
		FhirContext context = theContext;
		if (context.getVersion().getVersion() != theForVersion) {
			context = myFhirContextMap.get(theForVersion);
			if (context == null) {
				context = theForVersion.newContext();
				myFhirContextMap.put(theForVersion, context);
			}
		}
		return context;
	}
	
	public FhirContext getContextForVersion(RequestDetails theRequestDetails, FhirVersionEnum theForVersion) {
		return getContextForVersion(theRequestDetails.getServer().getFhirContext(), theForVersion);
	}


	@Override
	public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest,
			HttpServletResponse theResponse) throws AuthenticationException {
		
		String contentType = defaultString(theRequest.getHeader(Constants.HEADER_CONTENT_TYPE));
		FhirVersionEnum version = extractFhirVersion(contentType);

		if (version!=null) {
			if (serverVersion == null) {
				serverVersion = theRequestDetails.getServer().getFhirContext().getVersion().getVersion();
			}
			if (version!=serverVersion) {

				EncodingEnum encoding = RestfulServerUtils.determineRequestEncoding(theRequestDetails);
				IParser parser = encoding.newParser(getContextForVersion(theRequestDetails, version));
				
				IBaseResource requestResource = parser.parseResource( new ByteArrayInputStream(theRequestDetails.loadRequestContents()));			
				IBaseResource reqeustResourceConverted = null;
				
				if (version==FhirVersionEnum.DSTU2) {
					try {
						if (versionConvertor_10_30 == null) {
							versionConvertor_10_30 = new VersionConvertor_10_30(new NullVersionConverterAdvisor30());
						}
						if (requestResource instanceof IResource) {
							// code copied need to a testcase maybe
							FhirContext contextDstu2 = getContextForVersion(theRequestDetails, FhirVersionEnum.DSTU2);
							FhirContext contextDstu2Hl7Org = getContextForVersion(theRequestDetails, FhirVersionEnum.DSTU2_HL7ORG);
							requestResource = contextDstu2Hl7Org.newJsonParser().parseResource(contextDstu2.newJsonParser().encodeResourceToString(requestResource));
						}
						reqeustResourceConverted = versionConvertor_10_30.convertResource(toDstu2(requestResource));
					} catch (FHIRException e) {
						log.error("error converting request resource from 2To3, ignoring convertion: "+theRequestDetails, e);
						return true;
					}
					version=FhirVersionEnum.DSTU3;
				}
				if (version==FhirVersionEnum.DSTU3 && serverVersion==FhirVersionEnum.R4) {
					try {
						reqeustResourceConverted = VersionConvertor_30_40.convertResource(toDstu3(requestResource), true);
					} catch (FHIRException e) {
						log.error("error converting request resource from 3to4, ignoring convertion: "+theRequestDetails, e);
						return true;
					}
					version=FhirVersionEnum.R4;
				}
				if (version != serverVersion) {
					log.error("error converting version from "+version+" to "+serverVersion+" for "+theRequestDetails);
					return true;
				}
				
				try {
					IParser parserConverted = encoding.newParser(getContextForVersion(theRequestDetails, serverVersion));
					theRequestDetails.setRequestContents(parserConverted.encodeResourceToString(reqeustResourceConverted).getBytes());
				} catch (DataFormatException e) {
					log.error("error encoding Resource to R4, ignoring convertion: "+theRequestDetails, e);
					return true;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean outgoingResponse(RequestDetails theRequestDetails, ResponseDetails theResponseDetails, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws AuthenticationException {
		String[] formatParams = theRequestDetails.getParameters().get(Constants.PARAM_FORMAT);
		String accept = null;
		if (formatParams != null && formatParams.length > 0) {
			accept = formatParams[0];
		}
		if (isBlank(accept)) {
			accept = defaultString(theServletRequest.getHeader(Constants.HEADER_ACCEPT));
		}
		
		FhirVersionEnum version = extractFhirVersion(accept);
		
		if (version!=null) {
			if (serverVersion == null) {
				serverVersion = theRequestDetails.getServer().getFhirContext().getVersion().getVersion();
			}
			if (version!=serverVersion) {
				
				IBaseResource responseResource = theResponseDetails.getResponseResource();
				
				IBaseResource converted = null;
				try {
					if ((version==FhirVersionEnum.DSTU2 || version==FhirVersionEnum.DSTU3) && serverVersion==FhirVersionEnum.R4) {
						converted = VersionConvertor_30_40.convertResource(toR4(responseResource), true);
						responseResource = converted;						
					}
					if (version==FhirVersionEnum.DSTU2) {
						if (versionConvertor_10_30 == null) {
							versionConvertor_10_30 = new VersionConvertor_10_30(new NullVersionConverterAdvisor30());
						}
						converted = versionConvertor_10_30.convertResource(toDstu3(responseResource));						
					}
					if (converted==null) {
						log.error("error converting version from "+serverVersion+" to "+version+" for "+theRequestDetails);
						return true;
					}
					
				} catch (FHIRException e) {
					log.error("error converting resource from R4, ignoring convertion: "+theRequestDetails, e);
					return true;
				}
				if (converted != null) {
					// this works because serialization is done on version of resource
					theResponseDetails.setResponseResource(converted);
				} 
			}
		}
		return true;
	}

	private org.hl7.fhir.dstu3.model.Resource toDstu3(IBaseResource theResponseResource) {
		return (org.hl7.fhir.dstu3.model.Resource) theResponseResource;
	}
	
	private org.hl7.fhir.r4.model.Resource toR4(IBaseResource theResponseResource) {
		return (org.hl7.fhir.r4.model.Resource) theResponseResource;
	}
	
	private org.hl7.fhir.instance.model.Resource toDstu2(IBaseResource theResponseResource) {
		return (org.hl7.fhir.instance.model.Resource) theResponseResource;
	}

}
