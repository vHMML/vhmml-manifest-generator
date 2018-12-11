package org.vhmml.iiif.dto;

import org.vhmml.iiif.dto.json.deserialization.IIIFTypedMetaDataDeserializer;
import org.vhmml.iiif.dto.json.serialization.IIIFTypedMetaDataSerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(Include.NON_NULL)
@JsonSerialize(using = IIIFTypedMetaDataSerializer.class)
@JsonDeserialize(using = IIIFTypedMetaDataDeserializer.class)
public class IIIFTypedMetaData extends IIIFIdentifiable {

	private String type;

	public IIIFTypedMetaData() {
		super();
	}
	
	public IIIFTypedMetaData(String id, String type) {
		setId(id);
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
