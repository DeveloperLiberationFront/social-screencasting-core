package edu.ncsu.lubick.util;

import java.io.File;
import java.util.Date;

public class FileDateStructs {

	public File file;
	public Date date;

	public FileDateStructs(File file, Date date)
	{

		this.file = file;
		this.date = date;
	}

	@Override
	public String toString()
	{
		return "FileDateStructs [file=" + file + ", date=" + date + "]";
	}

}