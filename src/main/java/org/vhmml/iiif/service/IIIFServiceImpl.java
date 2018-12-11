package org.vhmml.iiif.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriUtils;
import org.vhmml.iiif.dto.Canvas;
import org.vhmml.iiif.dto.IIIFTypedMetaData;
import org.vhmml.iiif.dto.Image;
import org.vhmml.iiif.dto.Manifest;
import org.vhmml.iiif.dto.Resource;
import org.vhmml.iiif.dto.Sequence;
import org.vhmml.iiif.dto.Service;
import org.vhmml.iiif.util.IIIFUtil;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@org.springframework.stereotype.Service
public class IIIFServiceImpl implements IIIFService {	
	
	public static final String PROP_LABEL = "label";
	public static final String PROP_META_DATA = "metaData";
	public static final String PROP_DESCRIPTION = "description";
	public static final String PROP_ATTRIBUTION = "attribution";
	public static final String PROP_SEE_ALSO_URL = "seeAlsoUrl";
	public static final String PROP_SEE_FORMAT = "seeAlsoFormat";
	public static final String PROP_DC_TERMS_WITHIN = "dcTermsWithin";
	public static final String PROP_META_DATA_ID = "metaDataId";
	public static final String PROP_META_DATA_TYPE = "metaDataType";
	public static final String PROP_VIEWING_DIRECTION = "viewingDirection";
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	private IIIFUtil iiifUtil;
	
	@Value("${iiif.version}")
	private String iiifVersion;
	
	static {
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}
	
	public Manifest getIIIFManifest(String manifestId, Map<String, Object> manifestData) throws IOException, InterruptedException, ExecutionException {
		
		if(StringUtils.isEmpty(manifestId)) {
			throw new IllegalArgumentException("Missing required manifestId parameter.");
		}
		
		Manifest manifest = new Manifest();		
		manifest.setContext("http://www.shared-canvas.org/ns/context.json");
		String host = InetAddress.getLocalHost().getHostName();
		UriComponents uriComponents = ServletUriComponentsBuilder.fromCurrentContextPath().build();	
		// this needs to be the url to get this manifest
		String manifestUrl = UriUtils.encodePath(uriComponents.getScheme() + "://" + host + ":" + uriComponents.getPort() + "/iiif-service/manifest/" + manifestId, "utf-8");//		
		
		String viewingDirection = (String)manifestData.get(PROP_VIEWING_DIRECTION);
		boolean rightToLeft = "right-to-left".equals(viewingDirection);
		
		manifest.setId(manifestUrl);
		manifest.setType("sc:Manifest");
		manifest.setViewingDirection(viewingDirection);
    	manifest.setLabel((String)manifestData.get(PROP_LABEL));
    	Object metaData = manifestData.get(PROP_META_DATA);
    	
    	if(metaData != null) {
    		manifest.setMetaData((List<Map<String, String>>)metaData);
    	}
    	    	
    	manifest.setDescription((String)manifestData.get(PROP_DESCRIPTION));	    	
    	manifest.setAttribution((String)manifestData.get(PROP_ATTRIBUTION));
    	// not currently using see also so removing it for now so it doesnt' show up in Mirador's info panel
    	// manifest.setSeeAlso(new SeeAlso((String)manifestData.get(PROP_SEE_ALSO_URL), (String)manifestData.get(PROP_SEE_FORMAT)));
    	
    	manifest.setSequences(createSequences(manifestUrl, manifestId, rightToLeft));    	
    	
    	// TODO: it seems like the standard here is to use a url that points to the directory for all manifests
    	// by this author???  for an example see http://dms-data.stanford.edu/data/manifests/Walters/ 
    	manifest.setDcTermsWithin((String)manifestData.get(PROP_DC_TERMS_WITHIN));

    	// TODO: figure out what to put here
    	manifest.setScMetaData(new IIIFTypedMetaData((String)manifestData.get(PROP_META_DATA_ID), (String)manifestData.get(PROP_META_DATA_TYPE)));
		
		return manifest;
	}
	
	/**
	 * This method finds the location of the image directory by manifest id. The convention is that
	 * the images are stored in a directory named the same as the manifest id, somewhere under the configured
	 * images directory used by the IIIF image server. For example, if the IIIF image server is configured
	 * to use /var/lib/images as the images directory and there is a manifest with an id of ABC123, then the 
	 * images for that manifest are stored in a directory named ABC123 that is located somewhere under 
	 * /var/lib/images.  This allows for storing the images under an arbitrary number of sub
	 * directories under /var/lib/images, e.g. the images for the example may be somewhere like 
	 * /var/lib/images/subdir/another/ABC123. 
	 * 
	 * @param manifestId
	 * @return
	 */
	public String getProjectImagesPath(String manifestId) {
		return iiifUtil.findImageDirectory(manifestId);
	}
	
	public List<String> getProjectImageList(String imagesLocation) throws IOException {
		List<String> imageList = iiifUtil.getImageList(imagesLocation);
		Collections.sort(imageList);
		
		return imageList;
	}
	
	private List<Sequence> createSequences(String manifestUrl, String manifestImagesDir, boolean rightToLeft) throws IOException, InterruptedException, ExecutionException {
		List<Sequence> sequences = new ArrayList<Sequence>();
		Sequence sequence = new Sequence();
		
		// TODO: what is the standard for this, use the url to the sequence json?? examples don't necessarily point to valid urls...
		// for example http://dms-data.stanford.edu/data/manifests/Walters/qm670kv1873/normal
		// copying Stanford's example for now
		sequence.setId(manifestUrl + "/normal");
		sequence.setType("sc:Sequence");
		sequence.setLabel("Current page order");
		sequence.setCanvases(createCanvases(manifestUrl, manifestImagesDir, rightToLeft));				
		sequences.add(sequence);
		
		return sequences;
	}
	
	private List<Canvas> createCanvases(String manifestUrl, String manifestImagesDir, boolean rightToLeft) throws IOException, InterruptedException, ExecutionException {
		List<Canvas> canvases = new ArrayList<Canvas>();							
		List<Map<String, Object>> imageList = getReadingRoomImages(manifestImagesDir, rightToLeft);
		int count = 1;
		
		for(Map<String, Object> imageMap : imageList) {
			
			String label = (String)imageMap.get("label");
			Integer width = (Integer)imageMap.get("width");
			Integer height = (Integer)imageMap.get("height");

			Canvas canvas = new Canvas();
			List<Image> images = new ArrayList<Image>();
			// copying Stanford's example pattern for these urls right now
			String canvasId = manifestUrl + "/canvas/canvas-" + count;
			canvas.setId(canvasId);
			canvas.setType("sc:Canvas");
			canvas.setLabel(label);
			canvas.setWidth(width);
			canvas.setHeight(height);
			
			Image image = new Image();
			image.setId(manifestUrl + "/imageanno/anno-" + count);
			image.setType("oa:Annotation");				
			image.setOn(canvasId);
 
			Resource resource = new Resource();
			String imageUrl = (String)imageMap.get("id");
			resource.setId(imageUrl);
			resource.setType("dctypes:Image");
			resource.setFormat((String)imageMap.get("format"));
			resource.setWidth(width);
			resource.setHeight(height);

			Service service = new Service();
			// following Standord's example again here by just setting this to the image URL
			service.setId(imageUrl);
			service.setContext("http://iiif.io/api/image/" + iiifVersion + "/context.json");
			service.setProfile("http://iiif.io/api/image/" + iiifVersion + "/level" + iiifVersion + ".json");
			resource.setService(service);
			image.setResource(resource);
			images.add(image);
			canvas.setImages(images);
			canvases.add(canvas);
			count++;
		}
	
		return canvases;
	}
	
	private List<Map<String, Object>> getReadingRoomImages(String manifestImagesDir, boolean rightToLeft) throws IOException, InterruptedException, ExecutionException {		
		List<Map<String, Object>> imageData = new ArrayList<Map<String, Object>>();				
		String imagesJson = iiifUtil.getImageData(manifestImagesDir);
		
		if(StringUtils.isNotEmpty(imagesJson)) {
			imageData = objectMapper.readValue(imagesJson, new TypeReference<List<Map<String, Object>>>(){});
		} else {
			// TODO: probably want a way to turn this off, also should make sure only 1 can run at a time
			iiifUtil.generateImageData(manifestImagesDir);
			imagesJson = iiifUtil.getImageData(manifestImagesDir);
		}
		
		if(rightToLeft && imageData != null) {
			imageData = IIIFUtil.sortImageData(imageData, rightToLeft);
		}
		
		return imageData;
	}
}
