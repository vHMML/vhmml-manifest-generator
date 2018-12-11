package org.vhmml.iiif.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IIIFUtilTest {
	
	private static File testImagesDir;
	private static File bundledImagedDir;
	
	@BeforeClass
	public static void copyTestImagesToImagesDir() throws Exception {
		URL resource = ClassLoader.getSystemClassLoader().getResource("test_images");
	    bundledImagedDir = new File(resource.toURI());
	    IIIFUtil iiifUtil = new IIIFUtil();
	    Properties config = iiifUtil.getConfig();
	    String imagesDir = (String)config.get(IIIFUtil.CONFIG_IMAGES_DIR);	    
	    testImagesDir = new File(imagesDir + File.separator + "test_images");
	    if(!testImagesDir.canWrite()) {
	    	testImagesDir.setWritable(true);
	    }
	    FileUtils.copyDirectory(bundledImagedDir, testImagesDir);
	}
	
	@AfterClass
	public static void removeTestImages() throws IOException {
		FileUtils.forceDeleteOnExit(testImagesDir);
	}
	
	@Test
	public void testGenerateImageDataRelativePath() throws Exception {		
		new IIIFUtil().generateImageData("test_images");	
		assertTrue("Couldn't find meta data file after calling IIIFUtil.generateImageData", metaDataExists(testImagesDir));		
	}
	
	@Test
	public void testGenerateImageDataAbsolutePath() throws Exception {		
		new IIIFUtil().generateImageData(testImagesDir.getAbsolutePath());	
		assertTrue("Couldn't find meta data file after calling IIIFUtil.generateImageData", metaDataExists(testImagesDir));		
	}
	
	@Test
	public void testGenerateImageDataWithSubDirectory() throws Exception {
		File subDir = new File(testImagesDir.getAbsolutePath() + "/subdir");
		subDir.mkdir();
		
		if(!subDir.canWrite()) {
	    	subDir.setWritable(true);
	    }
		
		FileUtils.copyDirectory(bundledImagedDir, subDir);
		new IIIFUtil().generateImageData(testImagesDir.getAbsolutePath());		
		assertTrue("Couldn't find meta data file in sub directory after calling IIIFUtil.generateImageData", metaDataExists(subDir));		
	}
	
	@Test
	public void testGenerateImageDataWithNonImage() throws Exception {		
		File tempFile = new File(testImagesDir.getAbsolutePath() + "/test.txt");
		tempFile.createNewFile();
		new IIIFUtil().generateImageData(testImagesDir.getAbsolutePath());
		assertTrue("Couldn't find meta data file after calling IIIFUtil.generateImageData", metaDataExists(testImagesDir));		
	}
	
	@Test
	public void testUpdateConfig() throws Exception {
		IIIFUtil iiifUtil = new IIIFUtil();
		Properties originalConfig = iiifUtil.getConfig(); 
		String[] config = {IIIFUtil.CONFIG_IMAGES_DIR + "=/var/lib/images/reading_room_images", IIIFUtil.CONFIG_BASE_IMAGE_REQUEST_URL + "=http://hmmldev:8080/vhmml/readingRoom/image/"};
		iiifUtil.updateConfig(config);
		
		File homeDir = new File(System.getProperty("user.home"));
		File configFile = FileUtils.getFile(homeDir, "iiif-config.properties");		
		assertTrue("config file not found after calling setConfig", configFile.exists());
		
		FileInputStream input = new FileInputStream(configFile);
		Properties props = new Properties();
		props.load(input);			
		input.close();
		
		assertEquals("wrong value for " + IIIFUtil.CONFIG_IMAGES_DIR + " after calling setConfig", "/var/lib/images/reading_room_images", props.get(IIIFUtil.CONFIG_IMAGES_DIR));
		assertEquals("wrong value for " + IIIFUtil.CONFIG_BASE_IMAGE_REQUEST_URL + " after calling setConfig", "http://hmmldev:8080/vhmml/readingRoom/image/", props.get(IIIFUtil.CONFIG_BASE_IMAGE_REQUEST_URL));
		
		iiifUtil.setConfig(originalConfig);
	}
	
	@Test
	public void testFindImageDirectory() throws Exception {
		IIIFUtil iiifUtil = new IIIFUtil();
		Properties config = iiifUtil.getConfig();
		FileUtils.forceMkdir(new File(config.getProperty(IIIFUtil.CONFIG_IMAGES_DIR) + "/test_images/subdir"));
		String imageDirectory = iiifUtil.findImageDirectory("test_images");				
		assertNotNull("Top level image directory not found after creating", imageDirectory);
		
		// test sub dir
		imageDirectory = iiifUtil.findImageDirectory("subdir");				
		assertNotNull("Top level image directory not found after creating", imageDirectory);
	}
	
	@Test
	public void testGetImageList() throws Exception {
		IIIFUtil iiifUtil = new IIIFUtil();
		Properties config = iiifUtil.getConfig();
		FileUtils.forceMkdir(new File(config.getProperty(IIIFUtil.CONFIG_IMAGES_DIR) + "/test_images/subdir"));
		List<String> list = iiifUtil.getImageList("subdir");
		assertNotNull("Null image list returned calling with subdir", list);
	}
	
	private boolean metaDataExists(File directory) {
		boolean metaDataFileFound = false;
		
		if(directory != null && directory.exists()) {
			String[] fileList = directory.list();
			
			for(String fileName : fileList) {
				if("data.json".equals(fileName)) {
					metaDataFileFound = true;
					break;
				}
			}
		}
		
		return metaDataFileFound;
	}
}
