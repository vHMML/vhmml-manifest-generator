package org.vhmml.iiif.dto.json.serialization;

import java.io.IOException;

import org.vhmml.iiif.dto.Canvas;
import org.vhmml.iiif.dto.Sequence;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SequenceSerializer extends JsonSerializer<Sequence> {
 
    @Override
    public void serialize(Sequence sequence, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
    	jsonGenerator.writeStartObject();    	
    		JacksonUtil.writeStringField(jsonGenerator,"@id", sequence.getId());
    		JacksonUtil.writeStringField(jsonGenerator,"@type", sequence.getType());
    		JacksonUtil.writeStringField(jsonGenerator,"label", sequence.getLabel());
    		jsonGenerator.writeArrayFieldStart("canvases");
    		
    		for(Canvas canvas : sequence.getCanvases()) {
    			jsonGenerator.writeObject(canvas);
    		}
    		
    		jsonGenerator.writeEndArray();

    	jsonGenerator.writeEndObject();
    }
}
