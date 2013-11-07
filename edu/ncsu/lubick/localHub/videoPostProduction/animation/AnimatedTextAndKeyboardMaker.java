package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedTextAndKeyboardMaker extends AnimatedKeyboardMaker {

	protected static final int EXTRA_HEIGHT = 50;

	// private ShortcutsToKeyCodesConverter stringMaker = new ShortcutsToKeyCodesConverter();

	protected String keyPresses = "";

	public AnimatedTextAndKeyboardMaker() throws IOException
	{
		super();
	}

	@Override
	public BufferedImage makeAnimationForKeyCodes(int[] keycodes)
	{
		BufferedImage image = super.makeAnimationForKeyCodes(keycodes);

		if (keycodes.length == 0)
		{
			return image;
		}
		// String text = stringMaker.convert(keycodes);

		return addTextToImage(image, unActivatedKeyboard.getHeight());
	}

	protected BufferedImage addTextToImage(BufferedImage image, int yOffset)
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

	@Override
	public BufferedImage makeUnactivatedAnimation()
	{
		int width = unActivatedKeyboard.getWidth();
		int height = unActivatedKeyboard.getHeight();
		BufferedImage img = new BufferedImage(width, height + EXTRA_HEIGHT, BufferedImage.TYPE_INT_ARGB);

		Graphics graphics = img.getGraphics();

		graphics.setColor(Color.white);
		graphics.fillRect(0, 0, width, height + EXTRA_HEIGHT);
		copyFromImageToGraphics(graphics, unActivatedKeyboard, 0, 0, width, height);
		return img;
	}

	@Override
	public void setCurrentKeyPresses(String toolKeyPresses)
	{
		this.keyPresses = toolKeyPresses;
	}
	
	@Override
	public String getAnimationName()
	{
		return "image_text";
	}
}
