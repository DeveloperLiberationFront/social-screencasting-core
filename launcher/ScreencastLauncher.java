import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class ScreencastLauncher
{
	
	public static void main(String[] args)
	{
		try
		{
			findAndExecuteLatestVersion();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;	//abort execution
		}
		catch (NoExecutablesFoundException e)
		{
			e.printStackTrace();
			//fall through to try to update
		}
		
		searchForUpdate();
	}

	private static void searchForUpdate()
	{
		// TODO Auto-generated method stub
		
	}

	private static String findAndExecuteLatestVersion() throws IOException, NoExecutablesFoundException
	{
		File f = new File(".");
		String[] fileNames = f.list();
		Arrays.sort(fileNames);
		for(int i = fileNames.length-1;i>=0;i--) {
			String fileName = fileNames[i];
			if (fileName.startsWith("Screencasting-") && fileName.endsWith(".jar")) {
				ProcessBuilder pBuilder = new ProcessBuilder("java","-jar",fileName);
				pBuilder.directory(f);
				pBuilder.start();
				return fileName;
			}
		}
		throw new NoExecutablesFoundException("Could not find any jars to execute in "+f+" : "+Arrays.toString(fileNames));
		
	}
	
	
	private static class NoExecutablesFoundException extends Exception {

		public NoExecutablesFoundException(String string)
		{
			super(string);
		}
		
	}
}


