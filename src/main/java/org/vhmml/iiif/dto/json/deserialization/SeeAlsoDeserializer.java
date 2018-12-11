package org.vhmml.iiif.dto.json.deserialization;

import java.io.IOException;

import org.vhmml.iiif.dto.SeeAlso;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class SeeAlsoDeserializer extends JsonDeserializer<SeeAlso> {

	@Override
	public SeeAlso deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		SeeAlso seeAlso = new SeeAlso();
		JsonNode node = jp.getCodec().readTree(jp);
		seeAlso.setId(JacksonUtil.getStringProperty(node, "@id"));
		seeAlso.setDcTermsFormat(JacksonUtil.getStringProperty(node, "dcterms:format"));			
		
		return seeAlso;
	}

}
