package org.vhmml.iiif.dto.json.deserialization;

import java.io.IOException;

import org.vhmml.iiif.dto.IIIFTypedMetaData;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class IIIFTypedMetaDataDeserializer extends JsonDeserializer<IIIFTypedMetaData> {

	@Override
	public IIIFTypedMetaData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		IIIFTypedMetaData metaData = new IIIFTypedMetaData();
		JsonNode node = jp.getCodec().readTree(jp);
		
		metaData.setId(JacksonUtil.getStringProperty(node, "@id"));
		metaData.setType(JacksonUtil.getStringProperty(node, "@type"));
		
		return metaData;
	}
}
