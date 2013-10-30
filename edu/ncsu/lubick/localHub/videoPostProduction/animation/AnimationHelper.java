package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AnimationHelper extends JPanel implements KeyListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 701852946292219382L;
	private BufferedImage unActivatedKeyboard = null;
	private BufferedImage activatedKeyboard = null;

	private Map<Integer, AnimatedKeyPress> activatedAnimations = new HashMap<>();

	public AnimationHelper() throws IOException
	{
		File unactivatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_small.png");
		System.out.println(unactivatedKeyboardPath.getAbsolutePath());
		unActivatedKeyboard = ImageIO.read(unactivatedKeyboardPath);

		File activatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_pressed_small.png");
		activatedKeyboard = ImageIO.read(activatedKeyboardPath);
	}

	public static void main(String[] args) throws IOException
	{
		JFrame outerFrame = new JFrame("test");
		outerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		outerFrame.setBounds(0, 0, 560, 200);

		AnimationHelper innerPanel = new AnimationHelper();
		innerPanel.setSize(800, 600);
		outerFrame.add(innerPanel);
		outerFrame.addKeyListener(innerPanel);
		outerFrame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.out.println(e);
			}
		});
		outerFrame.setVisible(true);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(unActivatedKeyboard, 0, 0, null);
		for (AnimatedKeyPress animations : activatedAnimations.values())
		{
			animations.drawAnimatedSegment(g, activatedKeyboard);
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		System.out.println("Pressed: " + e);

		AnimatedKeyPress animation = AnimatedKeyPressFactory.makeAnimatedKeyPress(e);
		activatedAnimations.put(e.getKeyCode(), animation);
		repaint();

	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		System.out.println("Released: " + e);
		activatedAnimations.remove(e.getKeyCode());
		repaint();
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

}
