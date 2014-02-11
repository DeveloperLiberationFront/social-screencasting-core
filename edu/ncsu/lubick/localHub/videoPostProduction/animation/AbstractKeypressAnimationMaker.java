package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

public abstract class AbstractKeypressAnimationMaker implements KeypressAnimationMaker {

	protected static final int EXTRA_HEIGHT = 50;
	protected static BufferedImage unActivatedKeyboard;
	protected static BufferedImage activatedKeyboard;

	public AbstractKeypressAnimationMaker() throws IOException
	{
		conditionallyLoadImages();
	}

	protected static void conditionallyLoadImages() throws IOException
	{
		if (unActivatedKeyboard == null)
		{
			URL unactivatedKeyboardPath = AbstractKeypressAnimationMaker.class.
							getResource("/imageAssets/QWERTY_keyboard_small.png");
			unActivatedKeyboard = ImageIO.read(unactivatedKeyboardPath);
		}
		if (activatedKeyboard == null)
		{
			URL activatedKeyboardPath = AbstractKeypressAnimationMaker.class.
					getResource("/imageAssets/QWERTY_keyboard_pressed_small.png");
			activatedKeyboard = ImageIO.read(activatedKeyboardPath);
		}
	}

	public static void copyFromImageToGraphics(Graphics g, BufferedImage img, int firstX, int firstY, int secondX, int secondY)
	{
		g.drawImage(img, firstX, firstY, secondX, secondY, firstX, firstY, secondX, secondY, null);
	}

	protected BufferedImage addTextToImage(BufferedImage image, int yOffset, String keyPresses)
	{
		Graphics2D g = image.createGraphics();
		g.setColor(Color.black);
		Font font = new Font("Serif", Font.BOLD, 32);
		g.setFont(font);
		FontRenderContext context = g.getFontRenderContext();

		Rectangle2D rect = font.getStringBounds(keyPresses, context);

		int x = image.getWidth() / 2 - (int) Math.round(rect.getWidth()) / 2;

		g.drawString(keyPresses, x, yOffset + 32);
		return image;
	}

	public BufferedImage makeAnimatedKeyboardForKeycodes(int[] keycodes) // public for use in unit tests
	{
		BufferedImage img = makeUnactivatedAnimation();

		for (int keyCode : keycodes)
		{
			KeyPressAnimation animation = KeyPressAnimationFactory.makeKeyPressAnimation(keyCode);
			animation.drawAnimatedSegment(img.getGraphics(), activatedKeyboard);
		}

		return img;
	}

}
