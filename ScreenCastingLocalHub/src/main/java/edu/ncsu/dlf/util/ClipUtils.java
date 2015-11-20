package edu.ncsu.dlf.util;

import static edu.ncsu.dlf.util.FileUtilities.nonNull;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;

import edu.ncsu.dlf.localHub.videoPostProduction.PostProductionHandler;

public class ClipUtils {


	private static ByteArrayOutputStream byteBufferForImage = new ByteArrayOutputStream();

	private ClipUtils()
	{	
	}

	public static JSONArray makeFrameListForClip(File clipDir) 
	{
		return makeFrameListForClip(clipDir, 0, 0);
	}
	
	public static JSONArray makeFrameListForClip(File clipDir, int startFrame, int endFrame) 
	{
		JSONArray fileNamesArr = new JSONArray();

		if (endFrame == 0)
			endFrame = Integer.MAX_VALUE;
		
		if (clipDir.exists() && clipDir.isDirectory())
		{
			String[] files = nonNull(clipDir.list());
			Arrays.sort(files);
			
			int frameCount = 0;
			for(String imageFile: files)
			{
				if (imageFile.startsWith("frame")) {
					if (frameCount >= startFrame && frameCount <= endFrame) {
						fileNamesArr.put(imageFile);
					}
					frameCount++;
				}
					
			}
		}
		else {
			return null;
		}
		return fileNamesArr;
	}
	
	public static JSONArray makeFrameListForClipNoExtensions(File clipDir, int startFrame, int endFrame) 
	{
		JSONArray fileNamesArr = new JSONArray();

		if (endFrame == 0)
			endFrame = Integer.MAX_VALUE;
		
		if (clipDir.exists() && clipDir.isDirectory())
		{
			String[] files = nonNull(clipDir.list());
			Arrays.sort(files);
			
			int frameCount = 0;
			for(String imageFile: files)
			{
				if (imageFile.startsWith("frame")) {
					if (frameCount >= startFrame && frameCount <= endFrame) {
						fileNamesArr.put(imageFile.substring(0, imageFile.indexOf('.')));
					}
					frameCount++;
				}
					
			}
		}
		else {
			return null;
		}
		return fileNamesArr;
	}
	
	public static String makeBase64EncodedThumbnail(File image) throws IOException {
		return Base64.encodeBase64String(makeThumbnail(image));
	}
	
	public static byte[] makeThumbnail(File image) throws IOException {
		final int fixedHeight = 100;
		BufferedImage img = ImageIO.read(image);

		int newWidth = (img.getWidth() * fixedHeight) / img.getHeight();
		BufferedImage scaledImage = new BufferedImage(newWidth, fixedHeight, BufferedImage.TYPE_INT_RGB);

		scaledImage.createGraphics().drawImage(img, 0, 0, newWidth, fixedHeight, 0, 0, img.getWidth(), img.getHeight(), null);

		byte[] scaledImageData = null;
		synchronized (byteBufferForImage)
		{
			byteBufferForImage.reset();
			ImageIO.write(scaledImage, PostProductionHandler.FULLSCREEN_IMAGE_FORMAT, byteBufferForImage);
			scaledImageData = byteBufferForImage.toByteArray();
		}

		return scaledImageData;
	}
}
