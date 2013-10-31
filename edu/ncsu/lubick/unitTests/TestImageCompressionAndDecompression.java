package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wet.wired.jsr.recorder.CapFileManager;
import com.wet.wired.jsr.recorder.compression.CompressionFramePacket;
import com.wet.wired.jsr.recorder.compression.FrameCompressor;
import com.wet.wired.jsr.recorder.compression.FrameCompressorCodecStrategy;
import com.wet.wired.jsr.recorder.compression.FrameCompressorSavingStrategy;
import com.wet.wired.jsr.recorder.compression.FrameDataPack;

import edu.ncsu.lubick.BasicCapFileManager;
import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.localHub.forTesting.UtilitiesForTesting;
import edu.ncsu.lubick.localHub.videoPostProduction.DecompressionFramePacket;
import edu.ncsu.lubick.localHub.videoPostProduction.FrameDecompressor;
import edu.ncsu.lubick.localHub.videoPostProduction.FrameDecompressorCodecStrategy;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class TestImageCompressionAndDecompression
{

	private static final byte HOMOGENEOUS_FIRST_BYTE = (byte) -127; // this
																	// means the
																	// first
																	// byte is
	private static final Rectangle TestImage800x600 = new Rectangle(800, 600);
	private static final Rectangle TestImage1600x900 = new Rectangle(1600, 900);
	private static final int BYTES_FOR_HOMOGENEOUS_IMAGE = 15244; // observed
																	// via
																	// validated
																	// tests.

	private static final int ABSOLUTE_BLACK_PIXEL_VALUE = -16777216;
	private static final int FUDGED_BLACK_PIXEL_VALUE = -16777215;
	private static Logger logger = Logger.getLogger(TestImageCompressionAndDecompression.class.getName());

	private FrameCompressorCodecStrategy compressorToTest;
	private FrameDecompressorCodecStrategy decompressorToTest;
	private CompressionFramePacket compressionPacket;
	private DecompressionFramePacket decompressionPacket;

	private byte[] packedBytes;
	private Rectangle imageSizeRectangle;

	// Patterns for checking
	private byte[] blueMainPattern = new byte[] { 0, 0, -1 }; // this is
																// equivalent to
																// RGB(0,0,255)
	private byte[] blueTailPattern = new byte[] { 65, 0, 0, -1 }; // Each
																	// homogeneous
																	// 800x600
																	// ends with
	private byte[] darkRedMainPattern = new byte[] { 122, 0, 0 };
	private byte[] darkRedTailPattern = new byte[] { 65, 122, 0, 0 };
	private byte[] greenMainPattern = new byte[] { 0, -1, 0 };
	private byte[] greenTailPattern = new byte[] { 65, 0, -1, 0 };
	private byte[] redMainPattern = new byte[] { -1, 0, 0 };
	private byte[] redTailPattern = new byte[] { 65, -1, 0, 0 };
	private byte[] blackMainPattern = new byte[] { 0, 0, 1 }; // all black is
																// compressed to
																// have an RGB
																// of 0,0,1 so
																// as not to
																// confuse the
																// "same as last time"
																// stuff
	private byte[] blackTailPattern = new byte[] { 65, 0, 0, 1 };

	static
	{
		PropertyConfigurator.configure(ScreenRecordingModule.LOGGING_FILE_PATH);
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{

	}

	@Before
	public void setUp() throws Exception
	{

	}

	@Test
	public void testSimpleRed() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_red.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		verifyCompressedHomogenousImage(slimmedPackedBytes, redMainPattern, redTailPattern);
	}

	@Test
	public void testSimpleGreen() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_green.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		verifyCompressedHomogenousImage(slimmedPackedBytes, greenMainPattern, greenTailPattern);
	}

	@Test
	public void testSimpleBlue() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_blue.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		verifyCompressedHomogenousImage(slimmedPackedBytes, blueMainPattern, blueTailPattern);
	}

	@Test
	public void testSimpleDarkRed() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_darkRed.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		verifyCompressedHomogenousImage(slimmedPackedBytes, darkRedMainPattern, darkRedTailPattern);
	}

	@Test
	public void testSimpleBlack() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_black.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		verifyCompressedHomogenousImage(slimmedPackedBytes, blackMainPattern, blackTailPattern);
	}

	@Test
	public void testBlueCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_blue.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testBlackCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_black.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		if (logger.isTraceEnabled())
			debugWriteToImage(uncompressedData, "./src/test_images/testBlackCompressionAndDecompression.png");

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testDarkRedCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_darkRed.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testPurpleCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_purple.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testPurpleRedCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_purple_red.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testPurpleBlueCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_purple_blue.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testYellowWithBlackPatternCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_yellow_with_black_pattern.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testBlueWithBoxCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_blue_with_box.png");

		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile); // do
																			// this
																			// in
																			// two
																			// steps
																			// to
																			// have
																			// this
																			// to
																			// compare
																			// to
																			// later

		int numBytes = compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		logger.trace("Blue with Box compressed bytes are " + Arrays.toString(slimmedPackedBytes));

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testBlueTwoFrames() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File firstImage = new File("./src/test_images/800x600_blue.png");
		int[] firstImageRawData = readInImagesRawDataUsingImplicitSize(firstImage); // do
																					// this
																					// in
																					// two
																					// steps
																					// to
																					// have
																					// this
																					// to
																					// compare
																					// to
																					// later

		File secondImage = new File("./src/test_images/800x600_blue_with_box.png");
		int[] secondImageRawData = readInImagesRawDataUsingImplicitSize(secondImage);

		int firstNumBytes = compressToPackedBytesArray(firstImageRawData);
		logger.trace("First frame compressed bytes " + firstNumBytes);
		byte[] firstImageSlimmedPackedBytes = slimDataInPackedBytesArray(firstNumBytes);

		int secondNumBytes = compressToPackedBytesArray(secondImageRawData);
		logger.trace("Second frame compressed bytes " + secondNumBytes);

		assertTrue(secondNumBytes < firstNumBytes);

		byte[] secondImageSlimmedPackedBytes = slimDataInPackedBytesArray(secondNumBytes);

		logger.trace("The last 500 bytes are " + last500BytesToString(secondImageSlimmedPackedBytes));

		int[] uncompressedFirstImageData = decompressDataInSlimmedPackedBytes(firstImageSlimmedPackedBytes);

		int[] uncompressedSecondImageData = decompressDataInSlimmedPackedBytes(secondImageSlimmedPackedBytes);

		if (logger.isTraceEnabled())
			debugWriteToImage(uncompressedSecondImageData, "./src/test_images/testBlueTwoFrames.png");
		verifyArrayMatchesStraightPattern(firstImageRawData, uncompressedFirstImageData);

		debugWriteToImage(uncompressedSecondImageData, "./test.png");
		verifyArrayMatchesStraightPattern(secondImageRawData, uncompressedSecondImageData);
	}

	@Test
	public void testTwoLifelikeFrames() throws Exception
	{
		setUpForImageSize(TestImage1600x900); // Set up for the larger frame
												// size here:

		File firstImage = new File("./src/test_images/full_screen_0008.png");
		int[] firstImageRawData = readInImagesRawDataUsingImplicitSize(firstImage);
		if (logger.isTraceEnabled())
			logger.trace("The last 500 bytes of the uncompressed first frame are " + last500IntsToString(firstImageRawData));

		File secondImage = new File("./src/test_images/full_screen_0009.png");
		int[] secondImageRawData = readInImagesRawDataUsingImplicitSize(secondImage);

		int firstNumBytes = compressToPackedBytesArray(firstImageRawData);
		logger.trace("First frame compressed bytes " + firstNumBytes);
		byte[] firstImageSlimmedPackedBytes = slimDataInPackedBytesArray(firstNumBytes);
		if (logger.isTraceEnabled())
			logger.trace("The last 500 bytes of the compressed first frame are " + last500BytesToString(firstImageSlimmedPackedBytes));

		int secondNumBytes = compressToPackedBytesArray(secondImageRawData);
		logger.trace("Second frame compressed bytes " + secondNumBytes);

		assertTrue(secondNumBytes < firstNumBytes);

		byte[] secondImageSlimmedPackedBytes = slimDataInPackedBytesArray(secondNumBytes);

		int[] uncompressedFirstImageData = decompressDataInSlimmedPackedBytes(firstImageSlimmedPackedBytes);

		int[] uncompressedSecondImageData = decompressDataInSlimmedPackedBytes(secondImageSlimmedPackedBytes);

		if (logger.isTraceEnabled())
			debugWriteToImage(uncompressedFirstImageData, "./src/test_images/testTwoLifelikeFrames.png");
		verifyArrayMatchesStraightPattern(firstImageRawData, uncompressedFirstImageData);
		verifyArrayMatchesStraightPattern(secondImageRawData, uncompressedSecondImageData);
	}

	@Test
	public void test10FrameSequence() throws Exception
	{
		File testCapFile = new File("testCapfile.cap");
		BasicCapFileManager testCapFileManager = new BasicCapFileManager(testCapFile);
		testCapFileManager.setAndWriteFrameWidth(1600);
		testCapFileManager.setAndWriteFrameHeight(900);

		setUpForImageSizeUsingCapManager(TestImage1600x900, testCapFileManager); // Set
																					// up
																					// for
																					// the
																					// larger
																					// frame
																					// size
																					// here
																					// and
																					// use
																					// a
																					// cap
																					// manager
																					// so
																					// we
																					// can
																					// check
																					// the
																					// output
		List<File> testImages = load10TestImages();

		for (File f : testImages)
		{
			BufferedImage image = ImageIO.read(f);
			int[] rawData = convertBufferedImageToIntArray(image);
			FrameDataPack pack = makeFrameDataPack(rawData);
			((FrameCompressor) compressorToTest).packFrame(pack);
		}

		File testSequenceFolder = new File("./Scratch/");
		UtilitiesForTesting.clearOutDirectory("./Scratch/");
		PostProductionHandler.debugWriteOutAllImagesInCapFile(testCapFile, testSequenceFolder);

		File[] createdFiles = testSequenceFolder.listFiles();
		// because listFiles() returns things out of order, we must sort
		Arrays.sort(createdFiles);
		assertEquals(testImages.size(), createdFiles.length);
		for (int i = 0; i < createdFiles.length; i++)
		{
			File fileOne = testImages.get(i);
			File fileTwo = createdFiles[i];
			assertTrue(fileOne + " != " + fileTwo, doTwoImagesMatch(fileOne, fileTwo));
		}

	}

	private List<File> load10TestImages() throws IOException
	{
		List<File> retVal = new ArrayList<>();
		for (int i = 1; i <= 10; i++)
		{
			String imageName = String.format("./src/test_images/sequence (%d).png", i);
			File imageFile = new File(imageName);
			assertTrue(imageFile.exists());
			retVal.add(imageFile);
		}
		return retVal;
	}

	// =======================================================================================
	// ============================HELPERS===================================================

	private String last500BytesToString(byte[] bytes)
	{
		if (bytes.length <= 500)
			return Arrays.toString(bytes);
		StringBuilder builder = new StringBuilder(1500);
		builder.append("[...");
		for (int i = bytes.length - 501; i < bytes.length; i++)
		{
			builder.append(", ");
			builder.append(bytes[i]);
		}
		builder.append(']');
		return builder.toString();
	}

	private String last500IntsToString(int[] ints)
	{
		if (ints.length <= 500)
			return Arrays.toString(ints);
		StringBuilder builder = new StringBuilder(1500);
		builder.append("[...");
		for (int i = ints.length - 501; i < ints.length; i++)
		{
			builder.append(", ");
			builder.append(ints[i]);
		}
		builder.append(']');
		return builder.toString();
	}

	private void setUpForImageSize(Rectangle imageSize) throws IOException
	{
		this.imageSizeRectangle = imageSize;
		CapFileManager manager = new HiddenCapFileManager();
		compressorToTest = new FrameCompressor(manager, imageSize.width * imageSize.height);

		decompressorToTest = prepareDecompressorForImageSize();

		compressionPacket = new CompressionFramePacket(imageSize.width * imageSize.height);
		decompressionPacket = new DecompressionFramePacket(imageSizeRectangle);

		packedBytes = new byte[imageSize.width * imageSize.height * 3];
	}

	private void setUpForImageSizeUsingCapManager(Rectangle imageSize, CapFileManager manager) throws IOException
	{
		this.imageSizeRectangle = imageSize;
		compressorToTest = new FrameCompressor(manager, imageSize.width * imageSize.height);

		decompressorToTest = prepareDecompressorForImageSize();

		compressionPacket = new CompressionFramePacket(imageSize.width * imageSize.height);
		decompressionPacket = new DecompressionFramePacket(imageSizeRectangle);

		packedBytes = new byte[imageSize.width * imageSize.height * 3];
	}

	private FrameDecompressorCodecStrategy prepareDecompressorForImageSize() throws IOException
	{
		FrameDecompressorCodecStrategy decompressor = new FrameDecompressor();

		return decompressor;
	}

	private int[] decompressDataInSlimmedPackedBytes(byte[] slimmedPackedBytes) throws IOException
	{

		BufferedImage image = decompressImageFromSlimmedPackedBytes(slimmedPackedBytes);

		return convertBufferedImageToIntArray(image);
	}

	private BufferedImage decompressImageFromSlimmedPackedBytes(byte[] slimmedPackedBytes) throws IOException
	{

		decompressionPacket.setEncodedData(slimmedPackedBytes);

		decompressionPacket = decompressorToTest.decodeFramePacket(decompressionPacket);
		BufferedImage image = decompressorToTest.createBufferedImageFromDecompressedFramePacket(decompressionPacket);

		return image;
	}

	private void debugWriteToImage(int[] uncompressedData, String outputFile) throws IOException
	{
		BufferedImage bufferedImage = new BufferedImage(imageSizeRectangle.width, imageSizeRectangle.height, BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, imageSizeRectangle.width, imageSizeRectangle.height, uncompressedData, 0, imageSizeRectangle.width);
		debugWriteImageToFile(bufferedImage, outputFile);
	}

	public static void debugWriteImageToFile(BufferedImage bufferedImage, String outputFile) throws IOException
	{
		ImageIO.write(bufferedImage, "png", new File(outputFile));
	}

	private void verifyCompressedHomogenousImage(byte[] arrayOfDataBytes, byte[] mainPattern, byte[] tailPattern)
	{
		assertEquals(HOMOGENEOUS_FIRST_BYTE, arrayOfDataBytes[0]);
		verifyArrayMatchesStraightPattern(arrayOfDataBytes, 1, 4, mainPattern);
		verifyArrayMatchesCompressedPattern(arrayOfDataBytes, 4, BYTES_FOR_HOMOGENEOUS_IMAGE - 4, mainPattern);
		verifyArrayMatchesStraightPattern(arrayOfDataBytes, BYTES_FOR_HOMOGENEOUS_IMAGE - 4, BYTES_FOR_HOMOGENEOUS_IMAGE, tailPattern);
	}

	private void verifyArrayMatchesStraightPattern(byte[] arrayOfDataBytes, int startIndex, int endIndex, byte[] pattern)
	{
		if (pattern.length < endIndex - startIndex)
		{
			fail("There aren't enough numbers in the pattern to do the comparison");
		}
		for (int i = 0; startIndex + i < endIndex; i++)
		{
			assertEquals("Problem at index " + (startIndex + i), pattern[i], arrayOfDataBytes[startIndex + i]);
		}
	}

	@SuppressWarnings("unused")
	private void verifyArrayMatchesStraightPattern(byte[] expectedBytes, byte[] actualBytes)
	{
		if (actualBytes.length != expectedBytes.length)
		{
			fail("There aren't enough numbers in the pattern to do the comparison");
		}
		for (int i = 0; i < expectedBytes.length; i++)
		{
			assertEquals("Problem at index " + i, expectedBytes[i], actualBytes[i]);
		}
	}

	private void verifyArrayMatchesStraightPattern(int[] expectedBytes, int[] actualBytes)
	{
		if (actualBytes.length != expectedBytes.length)
		{
			fail("There aren't enough numbers in the pattern to do the comparison");
		}
		for (int i = 0; i < expectedBytes.length; i++)
		{
			if (expectedBytes[i] == ABSOLUTE_BLACK_PIXEL_VALUE)
			{
				expectedBytes[i] = FUDGED_BLACK_PIXEL_VALUE;
			}
			assertEquals("Problem at index " + i, expectedBytes[i], actualBytes[i]);
		}
	}

	private void verifyArrayMatchesCompressedPattern(byte[] slimmedPackedBytes, int startIndex, int endIndex, byte[] pattern)
	{
		for (int i = 0; startIndex + i < endIndex; i++)
		{
			if (i % (pattern.length + 1) == 0)
			{
				assertEquals(FrameCompressor.MAX_BLOCK_LENGTH, slimmedPackedBytes[startIndex + i]);
			}
			else
			{
				assertEquals(pattern[i % (pattern.length + 1) - 1], slimmedPackedBytes[startIndex + i]);
			}
		}
	}

	private byte[] slimDataInPackedBytesArray(int numBytes)
	{
		byte[] slimmedPackedBytes = new byte[numBytes]; // Just to make sure we
														// have 0s there
		for (int i = 0; i < slimmedPackedBytes.length; i++)
		{
			slimmedPackedBytes[i] = packedBytes[i];
		}
		return slimmedPackedBytes;
	}

	private int readInImageAndCompressToPackedBytesArray(File imageFile) throws IOException
	{
		int[] rawData = readInImagesRawDataUsingImplicitSize(imageFile);

		return compressToPackedBytesArray(rawData);
	}

	// returns number of bytes in this compressed bit
	private int compressToPackedBytesArray(int[] rawData)
	{

		FrameDataPack dataPack = makeFrameDataPack(rawData);

		compressionPacket.updateFieldsForNextFrame(dataPack);
		compressionPacket.dataToWriteBuffer = packedBytes; // using this for
															// "spying" reasons
															// instead of
															// compressionPacket.resizeInternalBytesIfNeeded();
		compressionPacket.isFullFrame = false;

		int numBytes = compressorToTest.compressData(compressionPacket);

		return numBytes;
	}

	private FrameDataPack makeFrameDataPack(int[] rawData)
	{
		FrameDataPack dataPack = new FrameDataPack(rawData, new Date().getTime());
		return dataPack;
	}

	private int[] readInImagesRawDataUsingImplicitSize(File imageFile) throws IOException
	{

		BufferedImage image = ImageIO.read(imageFile);

		return convertBufferedImageToIntArray(image);
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

	private int[] convertBufferedImageToIntArray(BufferedImage image)
	{
		int[] rawData = new int[imageSizeRectangle.width * imageSizeRectangle.height];

		image.getRGB(0, 0, imageSizeRectangle.width, imageSizeRectangle.height, rawData, 0, imageSizeRectangle.width);

		return rawData;
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

	private class HiddenCapFileManager implements CapFileManager {

		@Override
		public void shutDown()
		{
		}

		@Override
		public void flush() throws IOException
		{
		}

		@Override
		public void setAndWriteFrameWidth(int width) throws IOException
		{
		}

		@Override
		public void setAndWriteFrameHeight(int height) throws IOException
		{
		}

		@Override
		public void notifyStartWritingFrame(boolean isFullFrame) throws IOException
		{
		}

		@Override
		public void notifyEndWritingFrame()
		{
		}

		@Override
		public void write(int i) throws IOException
		{
		}

		@Override
		public void write(byte[] bA) throws IOException
		{
		}

		@Override
		public void write(byte[] dataToWrite, int offset, int numBytesToWrite) throws IOException
		{
		}

		@Override
		public FrameCompressorCodecStrategy getCodecStrategy()
		{
			return null;
		}

		@Override
		public FrameCompressorSavingStrategy getSavingStrategy()
		{
			return null;
		}

	}

}
