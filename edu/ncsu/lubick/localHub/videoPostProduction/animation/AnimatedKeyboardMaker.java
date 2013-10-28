package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class AnimatedKeyboardMaker implements KeypressAnimationMaker {

	
	private static BufferedImage unActivatedKeyboard;
	private static BufferedImage activatedKeyboard;
	
	
	public AnimatedKeyboardMaker() throws IOException 
	{
		conditionallyLoadImages();
	}

	private static void conditionallyLoadImages() throws IOException 
	{
		if (unActivatedKeyboard == null)
		{
			File unactivatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_small.png");
			unActivatedKeyboard = ImageIO.read(unactivatedKeyboardPath);
		}
		if (activatedKeyboard == null)
		{
			File activatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_pressed_small.png");
			activatedKeyboard = ImageIO.read(activatedKeyboardPath);
		}
	}
	
	@Override
	public BufferedImage makeAnimationForKeyCodes(int[] keycodes) 
	{
		int width = unActivatedKeyboard.getWidth();
		int height = unActivatedKeyboard.getHeight();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		copyFromImageToGraphics(img.getGraphics(), unActivatedKeyboard, 0, 0, width, height);
		
		for(int keyCode:keycodes)
		{
			AnimatedKeyPress animation = AnimatedKeyPressFactory.makeAnimatedKeyPress(keyCode);
			animation.drawAnimatedSegment(img.getGraphics(), activatedKeyboard);
		}
		
		return img;

	}

	public static void copyFromImageToGraphics(Graphics g, BufferedImage img, int firstX, int firstY, int secondX, int secondY)
	{
		g.drawImage(img, firstX, firstY, secondX, secondY, firstX, firstY, secondX, secondY, null);
	}

}
