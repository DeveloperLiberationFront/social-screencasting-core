package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.IOException;

public interface ImagesToMediaOutput
{
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws IOException;

	public String getMediaTypeInfo();

}
