package edu.ncsu.dlf.localHub.videoPostProduction.animation;

import java.awt.image.BufferedImage;

public interface KeypressAnimationMaker {

	BufferedImage makeUnactivatedAnimation();

	BufferedImage makeNewAnimationForKeyPresses(int[] keycodes, String toolKeyPresses);

	String getAnimationTypeName();

}
