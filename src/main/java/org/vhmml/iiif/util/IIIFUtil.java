package org.vhmml.iiif.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.PercentEscaper;

@Component
public class IIIFUtil {

	private static Logger LOG;
	
	private enum LOG_LEVEL {
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	};
	
	public static final String COMMAND_HELP = "help";
	public static final String COMMAND_SET_CONFIG = "updateConfig";
	public static final String COMMAND_SHOW_CONFIG = "showConfig";	
	public static final String COMMAND_GEN_IMAGE_DATA = "generateImageData";	
	public static final String COMMAND_VALIDATE_IMAGE_DATA = "validateImageData";	
		
	public static final String CONFIG_IMAGES_DIR = "images.dir";
	public static final String CONFIG_BASE_IMAGE_REQUEST_URL = "base.image.request.url";
	public static final String CONFIG_ENCODE_SLASHES = "encode.slashes";
		
	private static ObjectMapper objectMapper = new ObjectMapper();
	private static PercentEscaper percentEscaper = new PercentEscaper("", false);
	
	private static final String IMAGE_PATTERN = "(.+(\\.(?i)(jpg|jpeg|png|gif|bmp|tiff))$)";
	
	private static final Map<String, String> imageDirCache = new HashMap<>();
	
	private String imagesDir;
	
	private static boolean useLog4j = true;
	
	// we don't use log4j if we're running from the command line because log4j is configured to log to a file under the tomcat install
	static {
		if(StringUtils.isNotEmpty(System.getProperty("catalina.home"))) {
			LOG = Logger.getLogger(IIIFUtil.class);
		} else {
			useLog4j = false;
		}
	}
	
	public IIIFUtil() {
		String imagesDirName = getImagesDir();
		
		if(!new File(imagesDirName).exists()) {
			log("WARNING!! Images directory " + imagesDirName + " does not exist! This is the directory specified by the images.dir property in the iiif-config.properties file. It is the location where the IIIF Utility will look for images. Perhaps you updated this directory to point to a removable drive which has now been removed? You can update this directory using the updateConfig command or by editing the iiif-config.properties file directly. You can use the showConfig command to view all current configuration values. For more help use the help command.\n", LOG_LEVEL.ERROR);
		} else {
			List<File> imageDirs = getDirectoryList(imagesDirName);		
			
			for(File dir : imageDirs) {
				String imageDirPath = dir.getAbsolutePath();
				String relativeProjectDir = imageDirPath.substring(imageDirPath.indexOf(imagesDir) + imagesDir.length());
				String imageDir = relativeProjectDir.replace('\\', '/') + "/";
				imageDirCache.put(dir.getName(), imageDir);
			}	
		}			
	}	
	
	public static void main(String[] args) {
		IIIFUtil util = new IIIFUtil();
		
		if(args == null || args.length == 0) {
			util.printHelp();
		} else {
			String command = args[0];

			try {
				switch(command) {
					case COMMAND_SHOW_CONFIG:
						util.showConfig();
						break;
					case COMMAND_SET_CONFIG:
						util.updateConfig(Arrays.copyOfRange(args, 1, args.length));					
						break;
					case COMMAND_GEN_IMAGE_DATA:
						util.generateImageData(args[1]);
						break;
					case COMMAND_VALIDATE_IMAGE_DATA:
						util.validateImageData(args[1]);
						break;
					case COMMAND_HELP:
						util.printHelp();
						break;
					default:
						util.log("Unknown command specified: " + command, LOG_LEVEL.ERROR);
				}
			} catch(Exception e) {
				util.log("Unexpected exception running command " + command + "\n" + e.getMessage(), LOG_LEVEL.ERROR);
			}
		}		
	}
	
	public String findImageDirectory(String dirName) {
		String imageDir = imageDirCache.get(dirName);
		
		if(StringUtils.isEmpty(imageDir)) {
			File dir = null;
			List<File> imageDirs = getDirectoryList(getImagesDir());
			
			for(File nextDir : imageDirs) {
				if(nextDir.getName().equals(dirName)) {
					dir = nextDir;
					break;
				}
			}
			
			if(dir != null) {
				String imageDirPath = dir.getAbsolutePath();
				String relativeProjectDir = imageDirPath.substring(imageDirPath.indexOf(imagesDir) + imagesDir.length());
				imageDir = relativeProjectDir.replace('\\', '/') + "/";
				imageDirCache.put(dirName, imageDir);
			}
		}		

		return imageDir;
	}
	
	public List<File> getDirectoryList(String directory) {
		final List<File> directories = new ArrayList<>();
		
		// get all sub-directories
		FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
			@Override
		    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		        directories.add(dir.toFile());
		        return FileVisitResult.CONTINUE;
		    }
		};
		
		try {
			Files.walkFileTree(new File(directory).toPath(), fileVisitor);
		} catch(IOException e) {
			String message = "IOException attempting to get directory listing for directory " + directory;
			log(message, LOG_LEVEL.ERROR, e);			
			throw new RuntimeException(message);
		}		
		
		return directories;
	}
	
	/**
	 * This method gets the image list for a project using the <code>imagesLocation</code> parameter, which is a path 
	 * that is relative to the configured iiif config image.dir property. For example, if the iiif configuration 
	 * has the <code>images.dir</code> property is set to /var/lib/images/vhmml_images and we want to find the images
	 * for GARZ 00005, we would need to pass the path to the images relative to /var/lib/images/vhmml_images, which will
	 * be something like READING_ROOM/GARZ/GARZ 00005.  The reason we do it this way is because finding the path to the images is an 
	 * expensive task, so in vhmml we save the path to the images in the database with the object meta data, so we don't 
	 * have to look it up every time we need to get the images or image list.
	 * 
	 * @param imagesLocation
	 * @return
	 * @throws IOException
	 */
	public List<String> getImageList(String imagesLocation) throws IOException {
		List<String> imageList = new ArrayList<String>();
		File projectDir = new File(getImagesDir() + imagesLocation);		
				
		if(projectDir.exists()) {			
			Collection<File> images = FileUtils.listFiles(projectDir, new RegexFileFilter(IMAGE_PATTERN), null);
			
			for(File image : images) {
				imageList.add(image.getName());
			}
		}
				
		return imageList;
	}
	
	public String getImageData(String manifestImagesDir) throws IOException {
		String imagesJson = null;
		String imagesDir = getImagesDir();
		String projectPath = imagesDir + manifestImagesDir;
		File projectDir = new File(projectPath);
		
		// if the images directory isn't right underneath the imagesDir, it could be a sub directory,
		// the logic for finding the sub dir is expensive so only call findImageDirectory if know it's not a top level dir
		if(!projectDir.exists()) {
			projectDir = new File(imagesDir + findImageDirectory(manifestImagesDir));
		}
			
		File dataFile = FileUtils.getFile(projectDir, "data.json");
		
		if(dataFile.exists()) {
			imagesJson = FileUtils.readFileToString(dataFile, "utf-8");
		}
		
		return imagesJson;
	}
	
	public void showConfig() {
		Properties props = getConfig();
		Enumeration<Object> keys = props.keys();
		
		log("\n------ Current Configuration ------\n", LOG_LEVEL.INFO);
		
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			log(key + "=" + props.getProperty(key), LOG_LEVEL.INFO);
		}
	}
	
	// we just read the properties file every time for simplicity because config values could be changed by a user
	// updating the file directly, through this util via command line or via the web app and we want everyone
	// to see any changes.
	public Properties getConfig() {
		Properties config = new Properties();
		String homeDirName = System.getProperty("user.home");
		log("looking for iiif-config.properites in " + homeDirName, LOG_LEVEL.INFO);
		File homeDir = new File(homeDirName);
		File configFile = FileUtils.getFile(homeDir, "iiif-config.properties");
		InputStream input = null;
		
		try {					
			if(configFile.exists()) {
				input = new FileInputStream(configFile);
			} else {			
				log("Configuration has not been set, using default values.", LOG_LEVEL.INFO);
				input = IIIFUtil.class.getClassLoader().getResourceAsStream("iiif-config.properties");									
			}	
			
			config.load(input);						
		} catch (Exception e) {
			throw new RuntimeException("Exception attemtping to load configuration values from iiif-config.properties");
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch(IOException e) {
					log("IOException attempting to close input stream after reading configuration", LOG_LEVEL.ERROR, e);
				}				
			}			
		}
		
		return config;
	}
	
	public void setConfig(Properties properties) throws IOException {
		
		if(properties != null) {
			Enumeration<Object> keys = properties.keys();
			List<String> props = new ArrayList<String>();
			
			while(keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				props.add(key + "=" + properties.getProperty(key));
			}
			
			updateConfig(props.toArray(new String[props.size()]));
		}			
	}
	
	public void updateConfig(String[] args) throws IOException {
		File homeDir = new File(System.getProperty("user.home"));
		File configFile = FileUtils.getFile(homeDir, "iiif-config.properties");
		
		if(!configFile.exists()) {
			configFile.createNewFile();
		}
		
		Properties currentProps = new Properties();
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		
		try {
			inputStream = new FileInputStream(configFile);
			currentProps.load(inputStream);
			
			for(int i = 0; i < args.length; i++) {
				String[] property = args[i].split("=");
				String propName = property[0];
				String propValue = property[1];
				
				if(CONFIG_IMAGES_DIR.equals(propName)) {
					propValue = propValue.replace('\\', '/');				
				}
				
				currentProps.setProperty(propName, propValue);			
			}
			
			String comment = "Updated on " + new SimpleDateFormat("yyyy/MM/dd 'at' hh:mm aaa z").format(new Date());
			outputStream = new FileOutputStream(configFile);
			currentProps.store(outputStream, comment);
		} finally {
			if(inputStream != null) {
				inputStream.close();
			}
			
			if(outputStream != null) {
				outputStream.close();
			}
		}		
	}
	
	/**
	 * This method will validate the IIIF image meta data for a project by comparing the number of image files in the
	 * project's image directory to the number of images specified in the IIIF image meta data for the project. Please
	 * note that the argument to this method is simple the name of hte directory the images reside in, not an absolute path
	 * or a relative path. For example, if the project images are in /var/lib/images/vhmml_images/READING_ROOM/GARZ/GARZ 00005,
	 * then you should just pass "GARZ 00005" to this method. 
	 * 
	 * @param projectDir
	 * @return
	 * @throws IOException
	 */
	public boolean validateImageData(String projectDir) throws IOException {
		boolean isValid = false;
		int imageCount = 0;
		int metaDataImageCount = 0;
		log("validating manifest for " + projectDir + "...", LOG_LEVEL.INFO);		
		String imagesLocation = findImageDirectory(projectDir);
		
		log("images for " + projectDir + " are located in " + getImagesDir() + imagesLocation, LOG_LEVEL.INFO);
		List<String> imageList = getImageList(imagesLocation);
		imageCount = imageList.size();
		
		log("project images directory contains " + imageList.size() + " images", LOG_LEVEL.INFO);
		String imagesJson = getImageData(projectDir);		
		List<Map<String, Object>> imageData = new ArrayList<Map<String, Object>>();				
		
		if(StringUtils.isNotEmpty(imagesJson)) {
			imageData = objectMapper.readValue(imagesJson, new TypeReference<List<Map<String, Object>>>(){});			
			metaDataImageCount = imageData.size();
			log("image meta data contains " + imageData.size() + " image references", LOG_LEVEL.INFO);			
		} else if(imageCount > 0) { // if there are images but no image data, they probably just didn't generate the data yet
			log("Unable to read image meta data for project " + projectDir + ", has the meta data been generated?", LOG_LEVEL.INFO);
		}
		
		isValid = imageCount == metaDataImageCount;
		
		if(isValid) {
			log("manifest for " + projectDir + " passed validation", LOG_LEVEL.INFO);
		}
		
		return isValid;
	}
	
	public void generateImageData(String topDirectory) throws IOException, ExecutionException, InterruptedException {
		long start = new Date().getTime();			
		String baseImagesDir = getImagesDir();
		
		if(!topDirectory.startsWith("/") && 
			!topDirectory.startsWith("\\") && 
			!topDirectory.startsWith("C:") && 
			!topDirectory.startsWith("E:") && 
			StringUtils.isNotEmpty(baseImagesDir)) {				
			topDirectory = baseImagesDir + topDirectory;  
		}
		List<File> directories = getDirectoryList(topDirectory);
		
    	// only process one directory at a time or we can run out of resources quickly
        ExecutorService imageDataJobPool = Executors.newSingleThreadExecutor();
        Properties config = getConfig();        
        List<String> failedProjects = new ArrayList<>();
        
        for(File directory : directories) {
			Future<List<Map<String, Object>>> imageDataJobResponse = imageDataJobPool.submit(new ImageDataJob(directory.getAbsolutePath(), config));
			// get call blocks until the job thread is done
			imageDataJobResponse.get();
			String projectName = directory.getName();
			
			if(!validateImageData(projectName)) {
				log("Project " + projectName + " failed validation!", LOG_LEVEL.WARN);
				failedProjects.add(projectName);
			}						
		}
                
        try {
        	imageDataJobPool.shutdown();
            imageDataJobPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {
			log("InterruptedException waiting for image data generation to complete", LOG_LEVEL.ERROR, e);
        }        		
		
        if(failedProjects.size() > 0) {
        	log("Warning: The following projects failed validation, the image data for these projects is most likely invalid. Please try re-running the image data generation for these projects.", LOG_LEVEL.WARN);
        	for(String projectName : failedProjects) {
        		log(projectName, LOG_LEVEL.WARN);
        	}
        }
        
    	log("Total time: " + ((new Date().getTime() - start)/1000) + " seconds", LOG_LEVEL.INFO);    	
	}
	
	public static List<Map<String, Object>> sortImageData(List<Map<String, Object>> imageData, final boolean rightToLeft) {
		Collections.sort(imageData,
			new Comparator<Map<String, Object>>() {
				public int compare(Map<String, Object> image1, Map<String, Object> image2) {
					int equal = 0;
					if(image1 != null && image2 != null) {
						if(rightToLeft) {
							equal = ((String)image2.get("label")).compareTo((String)image1.get("label"));
						} else {
							equal = ((String)image1.get("label")).compareTo((String)image2.get("label"));
						}						
					}
					
			        return equal;
			    }
			}
		);
		
		return imageData;
	}
	
	public static List<Map<String, Object>> sortImageData(List<Map<String, Object>> imageData) {		
		return sortImageData(imageData, false);
	}
	
	private static String getImageUrl(String imageServerUrl, String imageDir, String imageName, boolean encodeSlashes) throws UnsupportedEncodingException {
		
		if(!imageServerUrl.endsWith("/")) {
			imageServerUrl += "/";
		}
		
		String imagePath = encodeSlashes ? percentEscaper.escape(imageDir + "/" + imageName) : imageDir + "/" + imageName;

		return imageServerUrl + imagePath; 
	}
	
	private String getImagesDir() {
		
		if(this.imagesDir == null) {
			Properties props = getConfig();
			String configImagesDir = props.getProperty(CONFIG_IMAGES_DIR);		
			this.imagesDir = configImagesDir.endsWith("/") ? configImagesDir : configImagesDir + "/";
		}
		
		return this.imagesDir;
	}
	
	private void printHelp() {
		System.out.println("Usage: java -jar iiif-util.jar command [options]\n");
		System.out.println("Available commands are:");
		
		System.out.print("1) generateImageData - This command is used to generate IIIF image meta data for consumption by a IIIF image viewer. ");
		System.out.print("This command requires an option specifying the name of the directory containing images for which to generate image meta data. ");
		System.out.print("Please note that currently absolute paths must point to a directory that is under the images.dir directory. ");
		System.out.print("Also, absolute directories must begin with / or \\ or C: or E:, drive letters other than C: or E: on Windows will be treated as relative to the images.dir.");
		System.out.println("\tExample: Generate IIIF meta data for all directories under /var/lib/images/reading_room_images:");
		System.out.println("\t\tjava -jar iiif-util.jar generateImageData /var/lib/images/reading_room_images");
		
		System.out.println("2) showConfig - Show the current configuration being used by the IIIF utility.");
		System.out.println("\tExample:");
		System.out.println("\t\tjava -jar iiif-util-jar showConfig");
		
		System.out.println("3) updateConfig - Update configuration values used by the IIIF utility.");
		System.out.println("\tExample: Change the base.image.request.url property:");
		System.out.println("\t\tjava -jar iiif-util.jar updateConfig base.image.request.url=http://vhmmltestweb:8080/vhmml/readingRoom/image");
		
		System.out.println("4) validateImageData - Validate the IIIF image meta data for a directory.");
		System.out.println("\tThis command requires the directory name containing the images for the project you would like to validate.");
		System.out.println("\tPlease note that this is not an aboslute path or relative directory, simply the name of the directory containing the images for the project.");
		System.out.println("\tFor example, if the absolute path to the image is \"/var/lib/images/vhmml_images/READING_ROOM/GARZ/GARZ 00005\", then you would simply pass \"GARZ 00005\" as the directory argument.");
		System.out.println("\tExample:");
		System.out.println("\t\tjava -jar iiif-util.jar validateImageData \"GARZ 00005\"");		
		
		System.out.println("5) help - Print this help message.");
		System.out.println("\tExample: java -jar iiif-util-jar help");
	}
	
	// represents an image data generation job for a single directory
	private class ImageDataJob implements Callable<List<Map<String, Object>>> {
		private String directory;		
		private String imageRequestUrl;		
		private boolean encodeSlashes;
		private ExecutorService imageReaderThreadPool = Executors.newFixedThreadPool(7);
		
		public ImageDataJob(String directory, Properties config) {
			
			if(directory == null) {
				throw new IllegalArgumentException("The location of the images is a required argument");
			}	
											
			this.imageRequestUrl = config.getProperty(CONFIG_BASE_IMAGE_REQUEST_URL);						
			this.encodeSlashes = Boolean.valueOf(config.getProperty(CONFIG_ENCODE_SLASHES)).booleanValue();
			
			if(imageRequestUrl == null) {
				throw new RuntimeException("The base image request URL property is not set in the iiif-config.properties file. This is the base URL used to retrieve images from your IIIF compliant image server. For example, if you're using digilib it will look something like this: " + CONFIG_BASE_IMAGE_REQUEST_URL + "=http://myiifimageserver:port/digilib/Scaler/IIIF/");
			}
			
			this.directory = directory;					
		}

		@Override
		public List<Map<String, Object>> call() {	
			List<Map<String, Object>> imageDataList = new ArrayList<Map<String, Object>>();
			File dir = new File(this.directory);			
			File dataFile = FileUtils.getFile(dir, "data.json");
			int imagesProcessed = 0;
			long start = new Date().getTime();			
			
			if(dataFile.exists()) {
				dataFile.delete();
			}
			
			if(dir.exists()) {
				String[] fileNames = dir.list();
				List<Future<Map<String, Object>>> imageReaderResponses = new ArrayList<Future<Map<String, Object>>>();
				log("\n---------------------------------------------------------------------------------------------------", LOG_LEVEL.INFO);
				log(dir.getName(), LOG_LEVEL.INFO);
				log("---------------------------------------------------------------------------------------------------", LOG_LEVEL.INFO);
				log("processing directory: " + this.directory + " with " + fileNames.length + " files.", LOG_LEVEL.INFO);
				
				for(String fileName : fileNames) {
					String filePath = this.directory + "/" + fileName;
					File file = new File(filePath);
					
					
					if (!file.isDirectory()) {						
						try {
							Future<Map<String, Object>> futureResponse = imageReaderThreadPool.submit(new ImageReader(file, imageRequestUrl, encodeSlashes));
							imageReaderResponses.add(futureResponse);														
						} catch(Exception e) {
							log("Unexpected exception throw trying to read response from image reader job for image file " + file.getAbsolutePath(), LOG_LEVEL.ERROR, e);
						}

						imagesProcessed++;
					}
				}
				
				try {
					for(Future<Map<String, Object>> imageResponse : imageReaderResponses) {
						Map<String, Object> imageData = imageResponse.get();
						
						if(imageData != null && imageData.size() > 0) {
							imageDataList.add(imageData);
						}
					}					
				} catch (Exception e) {
					log("Unexpected exception reading responses from image reading threads", LOG_LEVEL.ERROR, e);
				}				
								
				imageReaderThreadPool.shutdown();
				
				try {
					imageReaderThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);										
					log("completed processing directory: " + this.directory + " in " + ((new Date().getTime() - start)/1000) + " seconds, " + imagesProcessed + " images processed.\n", LOG_LEVEL.INFO);
				} catch (InterruptedException e) {				
					log("InterruptedException waiting for image data generation to complete", LOG_LEVEL.ERROR, e);
				}				
				
				try {
					if(imageDataList.size() > 0) {						
						imageDataList = sortImageData(imageDataList);
						FileUtils.writeStringToFile(dataFile, objectMapper.writeValueAsString(imageDataList));
					}					
				} catch (Exception e) {
					throw new RuntimeException("Exception attempting to write image meta data to file system", e);
				}		
			}
			
			return imageDataList;
		}
	}

	private class ImageReader implements Callable<Map<String, Object>> {
		
		File file;			
		String imageServerUrl = null;
		boolean encodeSlashes = false;
		
		ImageReader(File file, String imageServerUrl, boolean encodeSlashes) {
			this.file = file;
			this.imageServerUrl = imageServerUrl;
			this.encodeSlashes = encodeSlashes;
		}
		
		@Override
		public Map<String, Object> call() {	
			
			Map<String, Object> image = new HashMap<String, Object>();
			String fileAbsolutePath = file.getAbsolutePath();
			Path imagePathObj = Paths.get(fileAbsolutePath);
			BufferedInputStream inputStream = null;
			try {			
				inputStream = new BufferedInputStream(Files.newInputStream(imagePathObj));
				BufferedImage bufferedImage = ImageIO.read(inputStream);
				
				if(bufferedImage == null) {
					log("Unable to read file " + fileAbsolutePath + " as an image. This file is most likely not an image, meta data will not be generated for it.", LOG_LEVEL.WARN);				
				} else {
					String imageName = file.getName();				
					File nextParentDir = file.getParentFile();
					String imagesDir = new File(getImagesDir()).getAbsolutePath();
					String parentDirName = "";					
					
					while(!imagesDir.equals(nextParentDir.getAbsolutePath())) {
						parentDirName = nextParentDir.getName() + "/" + parentDirName;
						nextParentDir = nextParentDir.getParentFile();
					}
					
					image.put("id", getImageUrl(imageServerUrl, parentDirName, imageName, encodeSlashes));
					image.put("format", Files.probeContentType(imagePathObj));
					image.put("width", bufferedImage.getWidth());
					image.put("height", bufferedImage.getHeight());
					image.put("label", imageName);
				}				
			} catch (IOException e) {
				log("IOException attempting to create meta data for file " + fileAbsolutePath, LOG_LEVEL.ERROR, e);
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					log("Unable to close buffered input stream after processing image", LOG_LEVEL.ERROR, e);
				}
			}
			
			return image;
		}
	}
	
	private void log(String message, LOG_LEVEL level) {
		
		if(useLog4j) {
			switch(level) {
				case DEBUG:
					LOG.debug(message);
					break;
				case INFO:
					LOG.info(message);
					break;
				case WARN:
					LOG.warn(message);
					break;
				case ERROR:
					LOG.error(message);
					break;
				case FATAL:
					LOG.fatal(message);
					break;
			}
		} else {
			System.out.println(message);
			
			if(level == LOG_LEVEL.ERROR || level == LOG_LEVEL.FATAL) {
				writeToLogFile(message);
			}
		}		
	}
	
	private void log(String message, LOG_LEVEL level, Throwable t) {
		
		if(useLog4j) {			
			
			switch(level) {
				case DEBUG:
					LOG.debug(message, t);
					break;
				case INFO:
					LOG.info(message, t);
					break;
				case WARN:
					LOG.warn(message, t);
					break;
				case ERROR:
					LOG.error(message, t);
					break;
				case FATAL:
					LOG.fatal(message, t);
					break;
			}
		} else {
			System.out.println(message);
			
			if(level == LOG_LEVEL.ERROR || level == LOG_LEVEL.FATAL) {
				writeToLogFile(message);
			}			
		}		
	}
	
	private void writeToLogFile(String message) {
		File file = new File("iiif-util.log");
		
		try {									
			FileUtils.writeStringToFile(file, message + "\n", true);
		} catch(Exception e) {
			System.out.println("Exception attempting to write error to log file: " + e.getMessage());
		}	
	}
}
