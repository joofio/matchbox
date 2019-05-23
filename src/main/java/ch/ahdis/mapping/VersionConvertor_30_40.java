package ch.ahdis.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Resource;

import ch.ahdis.mapping.chmed16af.Chmed16afVersionConverterR3R4;

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

/**
 * VersionConvertor_30_40 extends the base VersionConvertor_30_40 to include conversions
 * necessary to ImplementatinGuide changes 
 * 
 * UseCases:
 * 
 */
public class VersionConvertor_30_40 extends org.hl7.fhir.convertors.VersionConvertor_30_40 {
	
  public VersionConvertor_30_40() {
		super();
		this.addImplemenationGuideVersionConverter(new Chmed16afVersionConverterR3R4());
	}

	private List<IgVersionConverterR4> igVersionConverters = new ArrayList<IgVersionConverterR4>();

 
  /**
	 * Impleentation Guide vary between version, after conversion between R3R4, we give the posssibilty to add resource specifig conversions afterwards
	 * @param r4
	 */
	void addImplemenationGuideVersionConverter(IgVersionConverterR4 r4) {
		igVersionConverters.add(r4);
	}
	
	@Override
	public Resource convertResource(org.hl7.fhir.dstu3.model.Resource src, boolean nullOk) throws FHIRException {
		Resource resource = super.convertResource(src, nullOk);
		for(IgVersionConverterR4 igVersionConverter: igVersionConverters) {
			igVersionConverter.upgrade(resource);
		}
		return resource;
	}
	
	@Override
	public org.hl7.fhir.dstu3.model.Resource convertResource(Resource src, boolean nullOk) throws FHIRException {
		for(IgVersionConverterR4 igVersionConverter: igVersionConverters) {
			src = igVersionConverter.downgrade(src);
		}
		return super.convertResource(src, nullOk);
	}


}
