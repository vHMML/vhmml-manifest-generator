package org.vhmml.iiif.dto;

import org.vhmml.iiif.dto.json.deserialization.SeeAlsoDeserializer;
import org.vhmml.iiif.dto.json.serialization.SeeAlsoSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = SeeAlsoSerializer.class)
@JsonDeserialize(using = SeeAlsoDeserializer.class)
public class SeeAlso extends IIIFIdentifiable {

	private String dcTermsFormat;
	
	public SeeAlso() {
		super();
	}
	
	public SeeAlso(String id, String dcTermsFormat) {
		setId(id);
		this.dcTermsFormat = dcTermsFormat;
	}

	public String getDcTermsFormat() {
		return dcTermsFormat;
	}

	public void setDcTermsFormat(String dcTermsFormat) {
		this.dcTermsFormat = dcTermsFormat;
	}
}
