package edu.ncsu.lubick;

import java.awt.Desktop;
import java.net.URI;

import edu.ncsu.lubick.localHub.LocalHub;

public class Runner 
{

	public static void main(String[] args) throws Exception {
		LocalHub.startServerForUse("HF/","kevinsDatabase.sqlite");
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:4443/"));
	}
}
