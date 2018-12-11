# vhmml-manifest-generator

The vHMML Manifest Generator creates the data.json files for images. Application requires Java Runtime Environment.

## Procedures

On Windows, open command prompt window and navigate to c:\iiif using the DOS console in Windows

Copy images to C:\Manifest

Check the IIIF generator configuration:
iiif-util showConfig OR java -jar iiif-util.jar showConfig

Run command to set IIIF generator to PROD:
iiif-util updateConfig base.image.request.url=https://www.vhmml.org/image/READING_ROOM images.dir=C:\Manifest
OR
java -jar iiif-util.jar updateConfig images.dir=E:/images/vhmml_images

Run command to create manifests for things that are in the C:\Manifest folder:
IIIf-util generateImageData C:\Manifest
OR
java -jar iiif-util.jar generateImageData "E:\images\vhmml_images\READING_ROOM\GARZ"
