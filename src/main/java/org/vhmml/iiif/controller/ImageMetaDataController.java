package org.vhmml.iiif.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vhmml.iiif.dto.Manifest;
import org.vhmml.iiif.service.IIIFService;
import org.vhmml.iiif.util.IIIFUtil;

@Controller
public class ImageMetaDataController {
	
	@Autowired
	private IIIFService iiifService;

	@Autowired
	private IIIFUtil iiifUtil;
	
	@ResponseBody
	@RequestMapping(value = "/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Properties getConfig() {
		return iiifUtil.getConfig();
	}
	
	@ResponseBody
	@RequestMapping(value = "/config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Properties updateConfig(HttpServletRequest request) throws IOException, InterruptedException {
		List<String> args = new ArrayList<>();
		
		for(String paramName : request.getParameterMap().keySet()) {
			args.add(paramName + "=" + request.getParameter(paramName));
		}		
		
		iiifUtil.updateConfig(args.toArray(new String[args.size()]));
		
		return iiifUtil.getConfig();
	}
	
	@ResponseBody
	@RequestMapping(value = "/manifest/{manifestId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Manifest getIIIFManifest(@PathVariable String manifestId) throws IOException, InterruptedException, ExecutionException {
		return iiifService.getIIIFManifest(manifestId, new HashMap<String, Object>());
	}
	
	@ResponseBody
	@RequestMapping(value = "/manifest/{manifestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Manifest getIIIFManifest(@PathVariable String manifestId, @RequestBody(required = false) Map<String, Object> manifestData) throws IOException, InterruptedException, ExecutionException {
		return iiifService.getIIIFManifest(manifestId, manifestData);
	}
	
	@ResponseBody
	@RequestMapping(value = "/imagesPath/{manifestId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getImagesPath(@PathVariable String manifestId) throws IOException {		
		return iiifService.getProjectImagesPath(manifestId);
	}
	
	@ResponseBody
	@RequestMapping(value = "/imageList/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<String> getImageList(HttpServletRequest request) throws IOException {
		String requestPath = request.getServletPath();
		// we parse the path to the images manually instead of using @PathVariable because the paths have percent 
		// encoded slashes in them for sub directories which would get interpreted as separate segments in the URL
		String imagesPath = requestPath.substring(requestPath.indexOf("/imageList") + "/imageList".length());
		return iiifService.getProjectImageList(imagesPath);
	}
}
