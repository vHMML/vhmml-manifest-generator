package org.vhmml.iiif.dto.json.serialization;

import java.io.IOException;

import org.vhmml.iiif.dto.Service;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ServiceSerializer extends JsonSerializer<Service> {

    @Override
    public void serialize(Service service, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
    	jsonGenerator.writeStartObject();
	 		JacksonUtil.writeStringField(jsonGenerator,"@id", service.getId());
			JacksonUtil.writeStringField(jsonGenerator,"@context", service.getContext());
			JacksonUtil.writeStringField(jsonGenerator,"profile", service.getProfile());
		jsonGenerator.writeEndObject();
    }
}
