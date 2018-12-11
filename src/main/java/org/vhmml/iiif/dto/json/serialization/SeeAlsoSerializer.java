package org.vhmml.iiif.dto.json.serialization;

import java.io.IOException;

import org.vhmml.iiif.dto.SeeAlso;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SeeAlsoSerializer extends JsonSerializer<SeeAlso> {
 
    @Override
    public void serialize(SeeAlso seeAlso, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
    	jsonGenerator.writeStartObject();
    		JacksonUtil.writeStringField(jsonGenerator,"@id", seeAlso.getId());
    		JacksonUtil.writeStringField(jsonGenerator,"dcterms:format", seeAlso.getDcTermsFormat());
    	jsonGenerator.writeEndObject();
    }
}
