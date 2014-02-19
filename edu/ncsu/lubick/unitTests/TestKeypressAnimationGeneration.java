package edu.ncsu.lubick.unitTests;

import static java.awt.event.KeyEvent.*;
import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.Runner;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.ShortcutsToKeyCodesConverter;

public class TestKeypressAnimationGeneration {
	
	@BeforeClass
	public static void setUpBeforeAll()
	{
		RemoteSQLDatabaseFactory.setUpToUseMockDB(true);
		try
		{
			URL url = Runner.class.getResource(LocalHub.LOGGING_FILE_PATH);
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			//load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
	}

	@Test
	public void testCopy()
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Ctrl+C");

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_C, results[1]);

	}

	@Test
	public void testCopyLowerCase()
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("ctrl+c");

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_C, results[1]);

	}

	@Test
	public void testComplicatedJunitTests()
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Alt+Shift+X, T");

		assertEquals(4, results.length);
		assertEquals(VK_ALT, results[0]);
		assertEquals(VK_SHIFT, results[1]);
		assertEquals(VK_X, results[2]);
		assertEquals(VK_T, results[3]);

	}

	@Test
	public void testOpenDeclaration()
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("F3");

		assertEquals(1, results.length);
		assertEquals(VK_F3, results[0]);

	}

	@Test
	public void testNumberCommand() throws Exception
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Ctrl+7");

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_7, results[1]);
	}

	@Test
	public void testPunctuationCommand() throws Exception
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Ctrl+/");

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_SLASH, results[1]);
	}

	@Test
	public void testTrickyPatterns() throws Exception
	{
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Ctrl+,"); // navigate previous

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_COMMA, results[1]);

	}

	@Test
	public void testKeyboardCreation() throws Exception
	{
		AnimatedKeyboardMaker akm = new AnimatedKeyboardMaker();
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] keycodes = converter.convert("Alt+Shift+X, T");

		BufferedImage img = akm.makeAnimatedKeyboardForKeycodes(keycodes);
		assertNotNull(img);

		debugWriteImageToFile(img, "test.png");

		assertTrue(doTwoImagesMatch("./src/test_images/AltShiftXT.png", "test.png"));

	}
	
	public static void debugWriteImageToFile(BufferedImage bufferedImage, String outputFileName) throws IOException
	{
		File outputFile = new File(outputFileName);
		if (outputFile.exists() && !outputFile.delete())
		{
			fail("Problem overwriting debugging image");

		}
		ImageIO.write(bufferedImage, "png", outputFile);
	}
	
	public static boolean doTwoImagesMatch(String fileOne, String fileTwo) throws IOException
	{
		return doTwoImagesMatch(new File(fileOne), new File(fileTwo));
	}

	public static boolean doTwoImagesMatch(File fileOne, File fileTwo) throws IOException
	{
		int[] firstImageRawData = readInImagesRawData(fileOne);

		int[] secondImageRawData = readInImagesRawData(fileTwo);
		return verifyArrayMatchesExactly(firstImageRawData, secondImageRawData);

	}

	private static boolean verifyArrayMatchesExactly(int[] firstImageRawData, int[] secondImageRawData)
	{
		if (firstImageRawData.length != secondImageRawData.length)
		{
			return false;
		}
		for (int i = 0; i < firstImageRawData.length; i++)
		{
			if (firstImageRawData[i] != secondImageRawData[i])
			{
				return false;
			}
		}
		return true;
	}
	
	private static int[] readInImagesRawData(File imageFile) throws IOException
	{

		BufferedImage image = ImageIO.read(imageFile);

		Rectangle imageSize = new Rectangle(0, 0, image.getWidth(), image.getHeight());

		return convertBufferedImageToIntArray(image, imageSize);
	}

	private static int[] convertBufferedImageToIntArray(BufferedImage image, Rectangle imageSizeRectangle)
	{
		int[] rawData = new int[imageSizeRectangle.width * imageSizeRectangle.height];

		image.getRGB(0, 0, imageSizeRectangle.width, imageSizeRectangle.height, rawData, 0, imageSizeRectangle.width);

		return rawData;
	}

}
