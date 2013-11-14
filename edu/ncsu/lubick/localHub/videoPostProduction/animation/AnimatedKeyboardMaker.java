package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedKeyboardMaker extends AbstractKeypressAnimationMaker {

	public AnimatedKeyboardMaker() throws IOException
	{
		conditionallyLoadImages();
	}

	@Override
	public BufferedImage makeNewAnimationForKeyPresses(int[] keycodes, String toolKeyPresses)
	{
		BufferedImage img = makeUnactivatedAnimation();

		for (int keyCode : keycodes)
		{
			KeyPressAnimation animation = KeyPressAnimationFactory.makeKeyPressAnimation(keyCode);
			animation.drawAnimatedSegment(img.getGraphics(), activatedKeyboard);
		}

		return img;

	}

	@Override
	public BufferedImage makeUnactivatedAnimation()
	{
		int width = unActivatedKeyboard.getWidth();
		int height = unActivatedKeyboard.getHeight();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		copyFromImageToGraphics(img.getGraphics(), unActivatedKeyboard, 0, 0, width, height);
		return img;
	}


	@Override
	public String getAnimationTypeName()
	{
		return "image";
	}



}
