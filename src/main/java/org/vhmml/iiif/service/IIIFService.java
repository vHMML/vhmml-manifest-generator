package org.vhmml.iiif.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.vhmml.iiif.dto.Manifest;

public interface IIIFService {
	public Manifest getIIIFManifest(String manifestId, Map<String, Object> manifestData) throws IOException, InterruptedException, ExecutionException;
	public String getProjectImagesPath(String manifestId);
	public List<String> getProjectImageList(String manifestId) throws IOException;	
}
