package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AnimationHelper extends JPanel implements MouseMotionListener, KeyListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 701852946292219382L;
	private BufferedImage unActivatedKeyboard = null;
	private BufferedImage activatedKeyboard = null;
	
	private final double widthOfSquare = 27.6;
	private final Point Q_START = new Point(47,67);
	private final int Q_HEIGHT = 24;
	private final Point A_START = new Point(56,91);
	private final int A_HEIGHT = 23;
	private final Point Z_START = new Point(65,114);
	private final int Z_HEIGHT = 23;
	
	private final Rectangle offScreen = new Rectangle(-25, -25, 25, 25);
	private Rectangle currentRectangle = new Rectangle(-25, -25, 25, 25);
	
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
		//innerPanel.addMouseMotionListener(innerPanel);
		outerFrame.addKeyListener(innerPanel);
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

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("Pressed: "+e);
		Point rowCol = getRowColForKeyPress(e);
		System.out.println(rowCol);
		Rectangle adjustedRectToShow = createRecForRowCol(rowCol);
		if (adjustedRectToShow == null)
		{
			return;
		}
		this.currentRectangle = adjustedRectToShow;
		repaint();
		
	}

	private Rectangle createRecForRowCol(Point rowCol) 
	{
		if (rowCol.y == -1)
		{
			return null;
		}
		if (rowCol.x == 0)
		{
			int roundedWidthOfSquare = (int)Math.round(widthOfSquare);
			int offSetX = (int)Math.round(rowCol.y * widthOfSquare + Q_START.x);
			
			return new Rectangle(offSetX, Q_START.y, roundedWidthOfSquare, Q_HEIGHT);
		}
		if (rowCol.x == 1)
		{
			int roundedWidthOfSquare = (int)Math.round(widthOfSquare);
			int offSetX = (int)Math.round(rowCol.y * widthOfSquare + A_START.x);
			
			return new Rectangle(offSetX, A_START.y, roundedWidthOfSquare, A_HEIGHT);
		}
		if (rowCol.x == 2)
		{
			int roundedWidthOfSquare = (int)Math.round(widthOfSquare);
			int offSetX = (int)Math.round(rowCol.y * widthOfSquare + Z_START.x);
			
			return new Rectangle(offSetX, Z_START.y, roundedWidthOfSquare, Z_HEIGHT);
		}
		return null;
	}

	private Point getRowColForKeyPress(KeyEvent e) {
		int row = getRowForKeyPress(e);
		int col = getColForKeyPress(e);
		
		return new Point(row, col);
	}
	
	
	private char[] firstRow = new char[]{'q','w','e','r','t','y','u','i','o','p','[',']'};
	private char[] secondRow = new char[]{'a','s','d','f','g','h','j','k','l',';','\''};
	private char[] thirdRow = new char[]{'z','x','c','v','b','n','m',',','.','/'};
	

	private int getRowForKeyPress(KeyEvent e) {
		if (arrayContains(firstRow, e.getKeyChar()))
		{
			return 0;
		}
		if (arrayContains(secondRow, e.getKeyChar()))
		{
			return 1;
		}
		if (arrayContains(thirdRow, e.getKeyChar()))
		{
			return 2;
		}
		return -1;
		
	}
	
	private boolean arrayContains(char[] array, char searchTerm)
	{
		for(Character c: array)
		{
			if (c.equals(searchTerm))
			{
				return true;
			}
		}
		return false;
	}
	
	private int getColForKeyPress(KeyEvent e) {
		for (int i = 0;i<firstRow.length;i++)
		{
			if (i<firstRow.length && firstRow[i] == e.getKeyChar())
			{
				return i;
			}
			if (i<secondRow.length && secondRow[i] == e.getKeyChar())
			{
				return i;
			}
			if (i<thirdRow.length && thirdRow[i] == e.getKeyChar())
			{
				return i;
			}
		}
		return -1;
	}


	@Override
	public void keyReleased(KeyEvent e) {
		System.out.println("Released: "+e);
		currentRectangle = offScreen;
		repaint();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	
	
}
