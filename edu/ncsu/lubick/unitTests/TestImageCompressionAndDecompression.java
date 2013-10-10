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
import com.wet.wired.jsr.recorder.compression.FrameCompressor;
import com.wet.wired.jsr.recorder.compression.FrameCompressorCodecStrategy;
import com.wet.wired.jsr.recorder.compression.FrameCompressorSavingStrategy;
import com.wet.wired.jsr.recorder.compression.FramePacket;

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.localHub.videoPostProduction.DefaultCodec;


public class TestImageCompressionAndDecompression
{

	private static final byte HOMOGENEOUS_FIRST_BYTE = (byte)-127;
	private static final Rectangle testImageFrame = new Rectangle(800, 600);
	private static final int BYTES_FOR_HOMOGENEOUS_IMAGE = 15244;	//observed via validated tests.
	private FrameCompressor compressorToTest;
	
	private static Logger logger = Logger.getLogger(FrameCompressor.class.getName());
	private byte[] packedBytes;
	
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
		CapFileManager manager = new HiddenCapFileManager();
		compressorToTest = new FrameCompressor(manager, testImageFrame.width * testImageFrame.height);
		packedBytes = new byte[testImageFrame.width * testImageFrame.height*3];
	}

	@Test
	public void testSimpleRed() throws Exception{
		File imageFile = new File("./src/test_images/800x600_red.png");
		String prefaceString = "simple red:";	
		
		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);

		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
		
		logger.info(prefaceString +Arrays.toString(slimmedPackedBytes));
	}
	
	@Test
	public void testSimpleGreen() throws Exception{
		File imageFile = new File("./src/test_images/800x600_green.png");
		String prefaceString = "simple green:";	
		
		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
			
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
		
		logger.info(prefaceString +Arrays.toString(slimmedPackedBytes));
	}
	
	@Test
	public void testSimpleBlue() throws Exception{
		File imageFile = new File("./src/test_images/800x600_blue.png");
		
		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
			
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		byte[] mainPattern = new byte[]{0,0,-1,126};
		byte[] tailPattern = new byte[]{64,0,0, -1};
		verifyCompressedHomogenousImage(slimmedPackedBytes, mainPattern, tailPattern);
	}
	

	@Test
	public void testSimpleDarkRed() throws Exception{
		File imageFile = new File("./src/test_images/800x600_darkRed.png");
		String prefaceString = "simple darkRed:";	
		
		int numBytes = readInImageAndCompressToPackedBytesArray(imageFile);
			
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
		
		logger.info(prefaceString +Arrays.toString(slimmedPackedBytes));
	}
	
	@Test
	public void testBlueCompressionAndDecompression() throws Exception{
		File imageFile = new File("./src/test_images/800x600_blue.png");
		
		int[] rawData = readInImagesRawData(imageFile);		//do this in two steps to have this to compare to later
		
		int numBytes =  compressToPackedBytesArray(rawData);
			
		assertEquals(BYTES_FOR_HOMOGENEOUS_IMAGE, numBytes);
		
		byte[] slimmedPackedBytes = slimDataInPackedBytesArray(numBytes);
			
		edu.ncsu.lubick.localHub.videoPostProduction.FramePacket aFrame = DefaultCodec.makeTestFramePacket(rawData.length);
		
		aFrame.setEncodedData(slimmedPackedBytes);
		
		aFrame.runLengthDecode();
		
		int[] uncompressedData = aFrame.getData();
		
		assertEquals(rawData.length, uncompressedData.length);
		for(int i = 0;i<rawData.length; i++)
		{
			assertEquals(rawData[i], uncompressedData[i]);
		}
	}
	
	
	
	//=======================================================================================
	//============================HELPERS===================================================

	private void verifyCompressedHomogenousImage(byte[] slimmedPackedBytes, byte[] mainPattern, byte[] tailPattern) {
		assertEquals(HOMOGENEOUS_FIRST_BYTE, slimmedPackedBytes[0]);
		verifyArrayMatchesPattern(slimmedPackedBytes, 1, BYTES_FOR_HOMOGENEOUS_IMAGE - 4, mainPattern);
		verifyArrayMatchesPattern(slimmedPackedBytes, BYTES_FOR_HOMOGENEOUS_IMAGE - 4, BYTES_FOR_HOMOGENEOUS_IMAGE, tailPattern);
	}
	
	private void verifyArrayMatchesPattern(byte[] slimmedPackedBytes, int startIndex, int endIndex, byte[] pattern) {
		for(int i = 0;startIndex + i < endIndex;i++)
		{
			assertEquals(pattern[i % pattern.length], slimmedPackedBytes[startIndex + i]);
		}
	}

	private byte[] slimDataInPackedBytesArray(int numBytes) {
		byte[] slimmedPackedBytes = new byte[numBytes + 10];	//Just to make sure we have 0s there
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
