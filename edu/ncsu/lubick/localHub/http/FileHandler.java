package edu.ncsu.lubick.localHub.http;

import httpserver.HTTPException;
import httpserver.HTTPRequest;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import edu.ncsu.lubick.localHub.LocalHub;

public class FileHandler extends StorytellerHTTPHandler
{

	public static final String STATIC_FILES_DIRECTORY = "frontend";

	public FileHandler(HTTPRequest request, LocalHub lh) throws HTTPException
	{
		super(request, lh);
	}

	@Override
	public void handle() throws HTTPException
	{
		try
		{
			// Create the path
			StringBuilder pathBuilder = new StringBuilder();

			// Add a '/' and part of our path
			for (String segment : getRequest().getSplitPath())
			{
				pathBuilder.append("/");
				pathBuilder.append(segment);
			}

			// Set the path to the pathBuilder or a '/' if the path is empty.
			String path = pathBuilder.toString();
			if (path.isEmpty())
				path = "/";

			// If the path ends in a '/' append `playback.html`
			if (path.substring(path.length() - 1).equals("/"))
				path += "index.html";

			path = STATIC_FILES_DIRECTORY + path;

			// Set the response types
			// TODO: functionize the substring.equals part (as in, make it
			// 		 look like `if (isExtension("html")))` instead
			if (path.substring(path.length() - 4).equalsIgnoreCase("html"))
				setResponseType("text/html");
			else if (path.substring(path.length() - 3).equalsIgnoreCase("css"))
				setResponseType("text/css");
			else if (path.substring(path.length() - 2).equalsIgnoreCase("js"))
				setResponseType("text/javascript");
			else if (path.substring(path.length() - 3).equalsIgnoreCase("png"))
				setResponseType("image/png");
			else if (path.substring(path.length() - 3).equalsIgnoreCase("jpg"))
				setResponseType("image/jpg");
			else if (path.substring(path.length() - 3).equalsIgnoreCase("svg"))
				setResponseType("image/svg+xml");
			else if (path.substring(path.length() - 3).equalsIgnoreCase("zip"))
				setResponseType("application/zip");
			else
				setResponseType("text/plain");

			// If its an impage or zip we have to read the file differently.
			if (isImageResponse() || isZipResponse())
			{
				setResponseText("file://" + getResource(path));
				setResponseSize(new File(new URL(getResponseText()).toString()).length());
				return;
			}

			// Read the file
			InputStream inputStream = ClassLoader.getSystemResourceAsStream(path);

			// If the file doesn't exist, tell the client.
			if (inputStream == null)
			{
				message(404, "<h1>404 - File Not Found</h1>");
				return;
			}

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder builder = new StringBuilder();

			for(String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine())
			{
				builder.append(line);
				builder.append("\n");
			}

			bufferedReader.close();

			// Set the response to the file's contents.
			setResponseText(builder.toString());

			setHandled(true);
		}
		catch(IOException e)
		{
			throw new HTTPException("File Not Found", e);
		}
	}

	public boolean isImageResponse()
	{
		return getResponseType().contains("image") && !getResponseType().contains("svg+xml");
	}
	public boolean isZipResponse()
	{
		return getResponseType().equalsIgnoreCase("application/zip");
	}

	@Override
	public void writeData() throws IOException
	{
		if (isImageResponse())
		{
			String imgType = getResponseType().substring(getResponseType().length() - 3);
			BufferedImage img = ImageIO.read(new URL(getResponseText()).openStream());
			ImageIO.write(img, imgType, getWriter());
		}

		else if(isZipResponse())
		{
			String fileLocation = getResponseText().substring("file:".length());
			ZipFile zipFile = new ZipFile(fileLocation);
			Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();

			ZipOutputStream zipOut = new ZipOutputStream(getWriter());

			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				zipOut.putNextEntry(entry);
				InputStream reader = zipFile.getInputStream(entry);
				for(int i=0; i<entry.getSize(); i++){
					zipOut.write(reader.read());
				}
			}

			zipOut.finish();
			zipFile.close();

			// If we are sending a zip file, it is the exported files. We want to delete it
			// after it is requested and served.
			new File(fileLocation).delete();

			//new File(fileLocation).delete();
		}
		else
			writeLine(getResponseText());
	}

}
