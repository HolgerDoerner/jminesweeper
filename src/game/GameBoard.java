package game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;

class GameBoard extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;
	
	private final JPanel	pnlMain		= new JPanel();
	private final JPanel	pnlMenu		= new JPanel();
	private final JLabel	lblSmiley	= new JLabel();
	
	private final int	sizeY;
	private final int	sizeX;
	private List<Field>	gameFields;
	
	class Field extends JButton {
		private static final long serialVersionUID = 1L;
		
		private final Field	_this_;
		private final int	_y_;
		private final int	_x_;
		private boolean		active	= true;
		
		int getValueY() {
			return _y_;
		}
		
		int getValueX() {
			return _x_;
		}
		
		boolean isActive() {
			return active;
		}
		
		private void setActive(boolean active) {
			this.active = active;
		}
		
		private void setDebugText(char text) {
			this.setText("" + text);
		}
		
		private Field(int y, int x) {
			_this_ = this;
			this._y_ = y;
			this._x_ = x;
			
			this.setPreferredSize(new Dimension(50, 50));
			this.setFont(new Font(null, Font.PLAIN, 40));
			this.setMargin(new Insets(0, 0, 0, 0));
			this.setBackground(Color.LIGHT_GRAY);
			
			// handler for mouse-rightclick
			// flag / un-flag a field
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) // left mouse-button
						Game.reveralField(_this_);
					else if (e.getButton() == MouseEvent.BUTTON3) // right mouse-button
						Game.markField(_this_);
					
					updateSmilie(1);
					
					if (Game.DEBUG)
						System.out.println("Clicked Field: " + _this_.getValueY() + "x" + _this_.getValueX()
								+ " Mouse-Button: " + e.getButton());
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					updateSmilie(0);
				}
			});
		}
		
		void updateField(final char fieldValue) {
			switch (fieldValue) {
				case Game.FLAGGED:
					this.setBackground(Color.YELLOW);
					this.setText(Character.toString(0x2691));
					this.active = false;
					break;
				
				case Game.BOMB:
					this.setText(Character.toString(0x1F571));
					
					if (Game.isVictory)
						this.setBackground(Color.GREEN);
					else
						this.setBackground(Color.RED);
					
					this.active = false;
					break;
				
				case Game.FLAGGED_BOMB:
					this.setText(Character.toString(0x2691));
					
					if (!Game.isGameRunning)
						this.setBackground(Color.GREEN);
					else
						this.setBackground(Color.YELLOW);
					
					this.active = false;
					break;
				
				default:
					this.setBackground(Color.GRAY);
					if (fieldValue > '0') {
						this.setText("" + fieldValue);
						if (fieldValue == '1')
							this.setForeground(Color.GREEN);
						else if (fieldValue == '2')
							this.setForeground(Color.YELLOW);
						else if (fieldValue >= '3')
							this.setForeground(Color.RED);
					}
					this.active = false;
					break;
			}
		}
	}
	
	private class MainMenu extends JMenuBar {
		private static final long serialVersionUID = 1L;
		
		private final JMenu			fileMenu		= new JMenu("File");
		private final JSeparator	menuSeparator	= new JSeparator();
		private final JMenuItem		saveMenuItem	= new JMenuItem("Save");
		private final JMenuItem		exitMenuItem	= new JMenuItem("Exit");
		
		private MainMenu() {
			exitMenuItem.addActionListener(e -> System.exit(0));
			
			fileMenu.add(saveMenuItem);
			fileMenu.add(menuSeparator);
			fileMenu.add(exitMenuItem);
			
			this.add(fileMenu);
		}
	}
	
	GameBoard(int sizeY, int sizeX) {
		this.sizeY = sizeY;
		this.sizeX = sizeX;
	}
	
	List<Field> getGameFields() {
		return this.gameFields;
	}
	
	void updateAllFields(final char[][] data) {
		for (Field field : this.gameFields) {
			field.updateField(data[field.getValueY()][field.getValueX()]);
			field.setActive(false);
		}
	}
	
	void updateSmilie(int status) {
		switch (status) {
			// mouse down
			case 0:
				this.lblSmiley.setText(Character.toString(0x1F632));
				break;
			
			// mouse up
			case 1:
				this.lblSmiley.setText(Character.toString(0x1F60A));
				break;
			
			// victory
			case 2:
				this.lblSmiley.setText(Character.toString(0x1F60D));
				break;
			
			// defeat
			case 3:
				this.lblSmiley.setText(Character.toString(0x1F62D));
				break;
		}
	}
	
	void debugView(char[][] data) {
		for (Field field : gameFields) {
			field.setDebugText(data[field.getValueY()][field.getValueX()]);
		}
	}
	
	@Override
	public void run() {
		this.lblSmiley.setFont(new Font(null, Font.BOLD, 50));
		this.lblSmiley.setAlignmentX(CENTER_ALIGNMENT);
		this.lblSmiley.setText(Character.toString(0x1F60A));
		
		this.pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.PAGE_AXIS));
		
		this.pnlMenu.add(Box.createVerticalGlue());
		this.pnlMenu.add(lblSmiley);
		this.pnlMenu.add(Box.createVerticalGlue());
		
		this.pnlMain.setLayout(new GridLayout(sizeY, sizeX));
		
		this.gameFields = new LinkedList<>();
		
		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				Field f = new Field(i, j);
				this.gameFields.add(f);
				this.pnlMain.add(f);
				
				if (Game.DEBUG)
					System.out.println("Generated Field: y" + f.getValueY() + " x" + f.getValueX());
			}
		}
		
		this.setLayout(new BorderLayout());
		
		this.setJMenuBar(new MainMenu());
		
		this.add(pnlMenu, BorderLayout.NORTH);
		this.add(pnlMain, BorderLayout.CENTER);
		
		this.setTitle("Minesweeper");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.pack();
		
		// at this point we make sure that everything is ready
		// so we have to wait for the other tasks to finish their job
		try {
			Game.barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.setVisible(true);
	}
}
