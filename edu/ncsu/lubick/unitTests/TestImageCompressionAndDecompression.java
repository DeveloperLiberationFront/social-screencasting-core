package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.localHub.videoPostProduction.DecompressionFramePacket;
import edu.ncsu.lubick.localHub.videoPostProduction.DefaultCodec;


public class TestImageCompressionAndDecompression
{

	private static final byte HOMOGENEOUS_FIRST_BYTE = (byte)-127;	//this means the first byte is 
	private static final Rectangle TestImage800x600 = new Rectangle(800, 600);
	private static final Rectangle TestImage1600x900 = new Rectangle(1600, 900);
	private static final int BYTES_FOR_HOMOGENEOUS_IMAGE = 15244;	//observed via validated tests.
	private FrameCompressor compressorToTest;

	private static Logger logger = Logger.getLogger(TestImageCompressionAndDecompression.class.getName());
	private byte[] packedBytes;

	//Patterns for checking
	private byte[] blueMainPattern = new byte[]{0, 0,-1};		//this is equivalent to RGB(0,0,255)
	private byte[] blueTailPattern = new byte[]{65, 0, 0, -1};	//Each homogeneous 800x600 ends with 
	private byte[] darkRedMainPattern = new byte[]{122, 0, 0};
	private byte[] darkRedTailPattern = new byte[]{65, 122, 0, 0};
	private byte[] greenMainPattern = new byte[]{0, -1, 0};
	private byte[] greenTailPattern = new byte[]{65, 0, -1, 0};
	private byte[] redMainPattern = new byte[]{-1, 0, 0};
	private byte[] redTailPattern = new byte[]{65, -1, 0, 0};
	private Rectangle imageSizeRectangle;

	static {
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
	public void testBlueCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_blue.png");

		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later

		int numBytes =  compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testDarkRedCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_darkRed.png");

		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later

		int numBytes =  compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testPurpleCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_purple.png");

		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later

		int numBytes =  compressToPackedBytesArray(rawData);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testPurpleRedCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_purple_red.png");

		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later

		int numBytes =  compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testPurpleBlueCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_purple_blue.png");

		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later

		int numBytes =  compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testBlueWithBoxCompressionAndDecompression() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File imageFile = new File("./src/test_images/800x600_blue_with_box.png");

		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later

		int numBytes =  compressToPackedBytesArray(rawData);

		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		logger.trace("Blue with Box compressed bytes are "+Arrays.toString(slimmedPackedBytes));

		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);

		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	@Test
	public void testBlueTwoFrames() throws Exception
	{
		setUpForImageSize(TestImage800x600);

		File firstImage = new File("./src/test_images/800x600_blue.png");
		int[] firstImageRawData = readInImagesRawData(firstImage);		//do this in two steps to have this to compare to later

		File secondImage = new File("./src/test_images/800x600_blue_with_box.png");
		int[] secondImageRawData = readInImagesRawData(secondImage);

		CompressionFramePacket thisFrame = FrameCompressor.makeTestFramePacket(firstImageRawData.length);

		int numBytes =  compressToPackedBytesArray(firstImageRawData, thisFrame);
		logger.trace("First frame compressed bytes "+numBytes);
		byte[] firstImageSlimmedPackedBytes = slimDataInPackedBytesArray(numBytes);

		numBytes = compressToPackedBytesArray(secondImageRawData, thisFrame);
		logger.trace("Second frame compressed bytes "+numBytes);

		byte [] secondImageSlimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
		logger.trace("The last 500 bytes are "+last500BytesToString(secondImageSlimmedPackedBytes));

		DecompressionFramePacket decompressionFrame = DefaultCodec.makeTestFramePacket(firstImageRawData.length);

		decompressionFrame = decompressDataInSlimmedPackedBytes(decompressionFrame, firstImageSlimmedPackedBytes);
		int[] uncompressedFirstImageData = decompressionFrame.getData();

		decompressionFrame = decompressDataInSlimmedPackedBytes(decompressionFrame, secondImageSlimmedPackedBytes);
		int[] uncompressedSecondImageData = decompressionFrame.getData();

		if (logger.isTraceEnabled()) debugWriteToImage(uncompressedSecondImageData, "./src/test_images/test.png");
		verifyArrayMatchesStraightPattern(firstImageRawData, uncompressedFirstImageData);
		verifyArrayMatchesStraightPattern(secondImageRawData, uncompressedSecondImageData);
	}

	@Test
	public void testTwoLifelikeFrames() throws Exception
	{
		setUpForImageSize(TestImage1600x900); //Set up for the larger frame size here:

		File firstImage = new File("./src/test_images/full_screen_0008.png");
		int[] firstImageRawData = readInImagesRawData(firstImage);	
		if (logger.isTraceEnabled()) logger.trace("The last 500 bytes of the uncompressed first frame are "+last500IntsToString(firstImageRawData));

		File secondImage = new File("./src/test_images/full_screen_0009.png");
		int[] secondImageRawData = readInImagesRawData(secondImage);

		CompressionFramePacket thisFrame = FrameCompressor.makeTestFramePacket(firstImageRawData.length);

		int numBytes =  compressToPackedBytesArray(firstImageRawData, thisFrame);
		logger.trace("First frame compressed bytes "+numBytes);
		byte[] firstImageSlimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
		if (logger.isTraceEnabled()) logger.trace("The last 500 bytes of the compressed first frame are "+last500BytesToString(firstImageSlimmedPackedBytes));

		numBytes = compressToPackedBytesArray(secondImageRawData, thisFrame);
		logger.trace("Second frame compressed bytes "+numBytes);

		byte [] secondImageSlimmedPackedBytes = slimDataInPackedBytesArray(numBytes);


		DecompressionFramePacket decompressionFrame = DefaultCodec.makeTestFramePacket(firstImageRawData.length);

		decompressionFrame = decompressDataInSlimmedPackedBytes(decompressionFrame, firstImageSlimmedPackedBytes);
		int[] uncompressedFirstImageData = decompressionFrame.getData();

		decompressionFrame = decompressDataInSlimmedPackedBytes(decompressionFrame, secondImageSlimmedPackedBytes);
		int[] uncompressedSecondImageData = decompressionFrame.getData();

		if (logger.isTraceEnabled()) debugWriteToImage(uncompressedFirstImageData, "./src/test_images/test.png");
		verifyArrayMatchesStraightPattern(firstImageRawData, uncompressedFirstImageData);
		verifyArrayMatchesStraightPattern(secondImageRawData, uncompressedSecondImageData);
	}



	//=======================================================================================
	//============================HELPERS===================================================

	private String last500BytesToString(byte[] bytes) 
	{
		if (bytes.length <= 500)
			return Arrays.toString(bytes);
		StringBuilder builder = new StringBuilder(1500);
		builder.append("[...");
		for(int i = bytes.length-501; i<bytes.length; i++)
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
		for(int i = ints.length-501; i<ints.length; i++)
		{
			builder.append(", ");
			builder.append(ints[i]);
		}
		builder.append(']');
		return builder.toString();
	}
	private void setUpForImageSize(Rectangle imageSize) 
	{
		this.imageSizeRectangle = imageSize;
		CapFileManager manager = new HiddenCapFileManager();
		compressorToTest = new FrameCompressor(manager, imageSize.width * imageSize.height);
		packedBytes = new byte[imageSize.width * imageSize.height*3];
	}

	private int[] decompressDataInSlimmedPackedBytes(int numBytesPerFrame, byte[] slimmedPackedBytes) 
	{
		DecompressionFramePacket temporaryFrame = DefaultCodec.makeTestFramePacket(numBytesPerFrame);

		return decompressDataInSlimmedPackedBytes(temporaryFrame, slimmedPackedBytes).getData();
	}

	private DecompressionFramePacket decompressDataInSlimmedPackedBytes(DecompressionFramePacket aFrame, byte[] slimmedPackedBytes) 
	{
		DecompressionFramePacket updatedFrame = new DecompressionFramePacket(aFrame.getFrameSize(), aFrame);

		updatedFrame.setEncodedData(slimmedPackedBytes);

		updatedFrame.runLengthDecode();

		return updatedFrame;
	}

	private void debugWriteToImage(int[] uncompressedData, String outputFile) throws IOException 
	{
		BufferedImage bufferedImage = new BufferedImage(imageSizeRectangle.width, imageSizeRectangle.height, BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, imageSizeRectangle.width, imageSizeRectangle.height, uncompressedData, 0, imageSizeRectangle.width);
		ImageIO.write(bufferedImage, "png", new File(outputFile));
	}

	private void verifyCompressedHomogenousImage(byte[] arrayOfDataBytes, byte[] mainPattern, byte[] tailPattern) {
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
		for(int i = 0;startIndex + i < endIndex;i++)
		{
			assertEquals("Problem at index " + (startIndex + i),pattern[i] , arrayOfDataBytes[startIndex + i]);
		}
	}

	@SuppressWarnings("unused")
	private void verifyArrayMatchesStraightPattern(byte[] expectedBytes, byte[] actualBytes) 
	{
		if (actualBytes.length != expectedBytes.length)
		{
			fail("There aren't enough numbers in the pattern to do the comparison");
		}
		for(int i = 0;i <expectedBytes.length;i++)
		{
			assertEquals("Problem at index " + i,expectedBytes[i] , actualBytes[i]);
		}
	}

	private void verifyArrayMatchesStraightPattern(int[] expectedBytes, int[] actualBytes) 
	{
		if (actualBytes.length != expectedBytes.length)
		{
			fail("There aren't enough numbers in the pattern to do the comparison");
		}
		for(int i = 0;i <expectedBytes.length;i++)
		{
			assertEquals("Problem at index " + i,expectedBytes[i] , actualBytes[i]);
		}
	}

	private void verifyArrayMatchesCompressedPattern(byte[] slimmedPackedBytes, int startIndex, int endIndex, byte[] pattern) {
		for(int i = 0;startIndex + i < endIndex;i++)
		{
			if (i % (pattern.length + 1) == 0)
			{
				assertEquals(FrameCompressor.MAX_BLOCK_LENGTH, slimmedPackedBytes[startIndex + i]);
			}
			else
			{
				assertEquals(pattern[i % (pattern.length + 1) - 1] , slimmedPackedBytes[startIndex + i]);
			}
		}
	}

	private byte[] slimDataInPackedBytesArray(int numBytes) {
		byte[] slimmedPackedBytes = new byte[numBytes];	//Just to make sure we have 0s there
		for (int i = 0;i<slimmedPackedBytes.length; i++)
		{
			slimmedPackedBytes[i]=packedBytes[i];
		}
		return slimmedPackedBytes;
	}

	private int readInImageAndCompressToPackedBytesArray(File imageFile) throws IOException {
		int[] rawData = readInImagesRawData(imageFile);

		return compressToPackedBytesArray(rawData);
	}

	private int compressToPackedBytesArray(int[] rawData) {
		CompressionFramePacket tempFrame = FrameCompressor.makeTestFramePacket(rawData.length);

		return compressToPackedBytesArray(rawData, tempFrame);
	}

	private int compressToPackedBytesArray(int[] rawData, CompressionFramePacket frameToUse) 
	{
		frameToUse.updateFieldsForNextFrame(rawData, -1, false);
		int numBytes = compressorToTest.compressDataUsingRunLengthEncoding(rawData, frameToUse, packedBytes, false);

		return numBytes;
	}


	private int[] readInImagesRawData(File imageFile) throws IOException {
		int[] rawData = new int[imageSizeRectangle.width * imageSizeRectangle.height];

		BufferedImage image = ImageIO.read(imageFile);

		image.getRGB(0, 0, imageSizeRectangle.width, imageSizeRectangle.height, rawData, 0, imageSizeRectangle.width);
		return rawData;
	}


	private class HiddenCapFileManager implements CapFileManager {

		@Override
		public void shutDown() {}

		@Override
		public void flush() throws IOException {}

		@Override
		public void setAndWriteFrameWidth(int width) throws IOException {}

		@Override
		public void setAndWriteFrameHeight(int height) throws IOException {}

		@Override
		public void startWritingFrame(boolean isFullFrame) throws IOException {}

		@Override
		public void endWritingFrame() {}

		@Override
		public void write(int i) throws IOException {}

		@Override
		public void write(byte[] bA) throws IOException {}

		@Override
		public void write(byte[] dataToWrite, int offset, int numBytesToWrite) throws IOException {}

		@Override
		public FrameCompressorCodecStrategy getCodecStrategy() {return null;}

		@Override
		public FrameCompressorSavingStrategy getSavingStrategy() {return null;}

	}

}
