package org.vhmml.iiif.dto.json.serialization;

import java.io.IOException;

import org.vhmml.iiif.dto.Canvas;
import org.vhmml.iiif.dto.Image;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CanvasSerializer extends JsonSerializer<Canvas> {
 
    @Override
    public void serialize(Canvas canvas, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
    	jsonGenerator.writeStartObject();    	
    		JacksonUtil.writeStringField(jsonGenerator, "@id", canvas.getId());
    		JacksonUtil.writeStringField(jsonGenerator, "@type", canvas.getType());
    		JacksonUtil.writeStringField(jsonGenerator, "label", canvas.getLabel());
    		JacksonUtil.writeNumberField(jsonGenerator, "height", canvas.getHeight());
    		JacksonUtil.writeNumberField(jsonGenerator, "width", canvas.getWidth());
    		jsonGenerator.writeArrayFieldStart("images");
    		
    		for(Image image : canvas.getImages()) {
    			jsonGenerator.writeObject(image);
    		}
    		
    		jsonGenerator.writeEndArray();

    	jsonGenerator.writeEndObject();
    }
}
