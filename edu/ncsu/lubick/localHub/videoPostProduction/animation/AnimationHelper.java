package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AnimationHelper extends JPanel implements MouseMotionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 701852946292219382L;
	private BufferedImage unActivatedKeyboard = null;
	private BufferedImage activatedKeyboard = null;
	

	
	Rectangle currentRectangle = new Rectangle(0, 0, 25, 25);
	
	public AnimationHelper() throws IOException {
		File unactivatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_small.png");
		System.out.println(unactivatedKeyboardPath.getAbsolutePath());
		unActivatedKeyboard = ImageIO.read(unactivatedKeyboardPath);
		
		File activatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_pressed_small.png");
		activatedKeyboard = ImageIO.read(activatedKeyboardPath);
	}
	
	public static void main(String[] args) throws IOException {
		JFrame outerFrame = new JFrame("test");
		outerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		outerFrame.setBounds(0, 0, 540, 190);
		
		AnimationHelper innerPanel = new AnimationHelper();
		innerPanel.setSize(800,600);
		outerFrame.add(innerPanel);
		innerPanel.addMouseMotionListener(innerPanel);
		outerFrame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println(e);
			}
		});
		outerFrame.setVisible(true);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(unActivatedKeyboard, 0, 0, null);
		g.drawImage(activatedKeyboard, currentRectangle.x, currentRectangle.y,
				currentRectangle.x + currentRectangle.width, currentRectangle.y + currentRectangle.height,
				currentRectangle.x, currentRectangle.y,
				currentRectangle.x + currentRectangle.width, currentRectangle.y + currentRectangle.height, null);
	}

	@Override
	public void mouseDragged(MouseEvent e) {mouseMoved(e);}

	@Override
	public void mouseMoved(MouseEvent e) {
		currentRectangle.x = e.getX();
		currentRectangle.y = e.getY();
		repaint();
	}
	
	
}
