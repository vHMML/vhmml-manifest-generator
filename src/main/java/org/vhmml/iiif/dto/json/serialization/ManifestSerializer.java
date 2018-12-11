package org.vhmml.iiif.dto.json.serialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.vhmml.iiif.dto.Manifest;
import org.vhmml.iiif.dto.Sequence;
import org.vhmml.iiif.util.JacksonUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ManifestSerializer extends JsonSerializer<Manifest> {
 
    @Override
    public void serialize(Manifest manifest, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
    	jsonGenerator.writeStartObject();
    		JacksonUtil.writeStringField(jsonGenerator, "@context", manifest.getContext());
    		JacksonUtil.writeStringField(jsonGenerator, "@id", manifest.getId());
    		JacksonUtil.writeStringField(jsonGenerator, "@type", manifest.getType());
    		JacksonUtil.writeStringField(jsonGenerator, "label", manifest.getLabel());
    		
    		List<Map<String, String>> metaData = manifest.getMetaData();
    		
    		if(metaData != null) {
    			jsonGenerator.writeArrayFieldStart("metadata");
    			
    			for(Map<String, String> metaDataParam : metaData) {
    				jsonGenerator.writeObject(metaDataParam);
    			}
    			
    			jsonGenerator.writeEndArray();
    		}
    		
    		JacksonUtil.writeStringField(jsonGenerator, "viewingDirection", manifest.getViewingDirection());
    		jsonGenerator.writeStringField("location", "vhmml");
    		jsonGenerator.writeStringField("logo", "https://www.vhmml.org/static/img/hmml-logo.png");	    	
    		JacksonUtil.writeStringField(jsonGenerator, "description", manifest.getDescription());
    		JacksonUtil.writeStringField(jsonGenerator, "attribution", manifest.getAttribution());
    		JacksonUtil.writeObjectField(jsonGenerator, "seeAlso", manifest.getSeeAlso());
	    	
	    	jsonGenerator.writeArrayFieldStart("sequences");
	    	
	    	for(Sequence sequence : manifest.getSequences()) {
	    		jsonGenerator.writeObject(sequence);
	    	}

	    	jsonGenerator.writeEndArray();
	    	JacksonUtil.writeStringField(jsonGenerator,"dcterms:within", manifest.getDcTermsWithin());
	    	JacksonUtil.writeObjectField(jsonGenerator,"sc:metadata", manifest.getScMetaData());
    	jsonGenerator.writeEndObject();
    }
}
