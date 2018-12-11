package org.vhmml.iiif.dto.json.serialization;

import java.io.IOException;

import org.vhmml.iiif.dto.Resource;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ResourceSerializer extends JsonSerializer<Resource> {
 
    @Override
    public void serialize(Resource resource, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
    	jsonGenerator.writeStartObject();    	
    		JacksonUtil.writeStringField(jsonGenerator,"@id", resource.getId());
    		JacksonUtil.writeStringField(jsonGenerator,"@type", resource.getType());
    		JacksonUtil.writeStringField(jsonGenerator,"format", resource.getFormat());
    		JacksonUtil.writeNumberField(jsonGenerator,"height", resource.getHeight());
    		JacksonUtil.writeNumberField(jsonGenerator,"width", resource.getWidth());
    		JacksonUtil.writeObjectField(jsonGenerator,"service", resource.getService());
    	jsonGenerator.writeEndObject();
    }
}
