package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wet.wired.jsr.recorder.CapFileManager;
import com.wet.wired.jsr.recorder.compression.FrameCompressor;
import com.wet.wired.jsr.recorder.compression.FrameCompressorCodecStrategy;
import com.wet.wired.jsr.recorder.compression.FrameCompressorSavingStrategy;
import com.wet.wired.jsr.recorder.compression.FramePacket;

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.localHub.videoPostProduction.DefaultCodec;


public class TestImageCompressionAndDecompression
{

	private static final byte HOMOGENEOUS_FIRST_BYTE = (byte)-127;	//this means the first byte is 
	private static final Rectangle testImageFrame = new Rectangle(800, 600);
	private static final int BYTES_FOR_HOMOGENEOUS_IMAGE = 15244;	//observed via validated tests.
	private FrameCompressor compressorToTest;
	
	private static Logger logger = Logger.getLogger(FrameCompressor.class.getName());
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
	
	static {
		PropertyConfigurator.configure(ScreenRecordingModule.LOGGING_FILE_PATH);
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
		logger.info("Starting CompressionTests");
	}

	@Before
	public void setUp() throws Exception 
	{
		CapFileManager manager = new HiddenCapFileManager();
		compressorToTest = new FrameCompressor(manager, testImageFrame.width * testImageFrame.height);
		packedBytes = new byte[testImageFrame.width * testImageFrame.height*3];
	}

	@Test
	public void testSimpleRed() throws Exception
	{
		File imageFile = new File("./src/test_images/800x600_red.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
		
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		verifyCompressedHomogenousImage(slimmedPackedBytes, redMainPattern, redTailPattern);
	}
	
	@Test
	public void testSimpleGreen() throws Exception
	{
		File imageFile = new File("./src/test_images/800x600_green.png");
		
		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
		
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		verifyCompressedHomogenousImage(slimmedPackedBytes, greenMainPattern, greenTailPattern);
	}
	
	@Test
	public void testSimpleBlue() throws Exception
	{
		File imageFile = new File("./src/test_images/800x600_blue.png");
		
		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
			
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		verifyCompressedHomogenousImage(slimmedPackedBytes, blueMainPattern, blueTailPattern);
	}
	

	@Test
	public void testSimpleDarkRed() throws Exception
	{
		File imageFile = new File("./src/test_images/800x600_darkRed.png");

		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
		
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		verifyCompressedHomogenousImage(slimmedPackedBytes, darkRedMainPattern, darkRedTailPattern);
	}
	
	@Test
	public void testBlueCompressionAndDecompression() throws Exception
	{
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
		File imageFile = new File("./src/test_images/800x600_darkRed.png");
		
		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later
		
		int numBytes =  compressToPackedBytesArray(rawData);
			
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		int[] uncompressedData = decompressDataInSlimmedPackedBytes(rawData.length,slimmedPackedBytes);
		
		verifyArrayMatchesStraightPattern(rawData, uncompressedData);
	}

	
	
	//=======================================================================================
	//============================HELPERS===================================================
	
	private int[] decompressDataInSlimmedPackedBytes(int numBytesPerFrame, byte[] slimmedPackedBytes) 
	{
		edu.ncsu.lubick.localHub.videoPostProduction.FramePacket aFrame = DefaultCodec.makeTestFramePacket(numBytesPerFrame);
		
		aFrame.setEncodedData(slimmedPackedBytes);
		
		aFrame.runLengthDecode();
		
		int[] uncompressedData = aFrame.getData();
		return uncompressedData;
	}
	
	@SuppressWarnings("unused")
	private void debugWriteToImage(int[] uncompressedData, String outputFile) throws IOException 
	{
		BufferedImage bufferedImage = new BufferedImage(testImageFrame.width, testImageFrame.height, BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, testImageFrame.width, testImageFrame.height, uncompressedData, 0, testImageFrame.width);
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
		FramePacket aFrame = FrameCompressor.makeTestFramePacket(rawData.length);
		
		int numBytes = compressorToTest.compressDataUsingRunLengthEncoding(rawData, aFrame, packedBytes, true);
		return numBytes;
	}

	private int[] readInImagesRawData(File imageFile) throws IOException {
		int[] rawData = new int[testImageFrame.width * testImageFrame.height];
		
		BufferedImage image = ImageIO.read(imageFile);

		image.getRGB(0, 0, testImageFrame.width, testImageFrame.height, rawData, 0, testImageFrame.width);
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
