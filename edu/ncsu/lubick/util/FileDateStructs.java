package edu.ncsu.lubick.util;

import java.io.File;
import java.util.Date;

public class FileDateStructs {

	public File file;
	public Date startTime;

	public FileDateStructs(File file, Date startTime)
	{

		this.file = file;
		this.startTime = startTime;
	}

	@Override
	public String toString()
	{
		return "FileDateStructs [file=" + file + ", startTime=" + startTime
				+ "]";
	}

}