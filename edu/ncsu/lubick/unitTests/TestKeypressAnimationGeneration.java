package edu.ncsu.lubick.unitTests;

import static java.awt.event.KeyEvent.*;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.Test;

import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.KeypressAnimationMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.ShortcutsToKeyCodesConverter;

public class TestKeypressAnimationGeneration {

	@Test
	public void testCopy() {
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Ctrl+C");

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_C, results[1]);

	}

	@Test
	public void testCopyLowerCase() {
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("ctrl+c");

		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_C, results[1]);

	}

	@Test
	public void testComplicatedJunitTests() {
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Alt+Shift+X, T");

		assertEquals(4, results.length);
		assertEquals(VK_ALT, results[0]);
		assertEquals(VK_SHIFT, results[1]);
		assertEquals(VK_X, results[2]);
		assertEquals(VK_T, results[3]);

	}

	@Test
	public void testOpenDeclaration() {
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("F3");

		assertEquals(1, results.length);
		assertEquals(VK_F3, results[0]);


	}

	@Test
	public void testKeyboardCreation() throws Exception 
	{
		KeypressAnimationMaker akm = new AnimatedKeyboardMaker();
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] keycodes = converter.convert("Alt+Shift+X, T");

		BufferedImage img = akm.makeAnimationForKeyCodes(keycodes);
		assertNotNull(img);

	}

	@Test
	public void testTrickyPatterns() throws Exception {
		ShortcutsToKeyCodesConverter converter = new ShortcutsToKeyCodesConverter();

		int[] results = converter.convert("Ctrl+,");	//navigate previous

		System.err.println(Arrays.toString(results));
		assertEquals(2, results.length);
		assertEquals(VK_CONTROL, results[0]);
		assertEquals(VK_COMMA, results[0]);

	}

}
