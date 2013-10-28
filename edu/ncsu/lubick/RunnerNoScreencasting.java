package edu.ncsu.lubick;

import java.awt.Desktop;
import java.net.URI;

import edu.ncsu.lubick.localHub.LocalHub;

public class RunnerNoScreencasting 
{

	public static void main(String[] args) throws Exception
	{
		LocalHub.startServerAndReturnDebugAccess("HF/","kevinsDatabase.sqlite", true, false);
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:4443/"));
	}
	
}
