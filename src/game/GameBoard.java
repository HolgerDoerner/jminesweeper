package game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

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
	
	private final JPanel		pnlMain			= new JPanel();
	private final JPanel		pnlMenu			= new JPanel();
	private final JLabel		lblSmiley		= new JLabel();
	
	private final int		sizeX;
	private final int		sizeY;
	private List<Component>	gameFields;
	
	class Field extends JButton {
		private static final long serialVersionUID = 1L;
		
		private final Field	_this_;
		private final int	_x_;
		private final int	_y_;
		private boolean		active	= true;
		
		int getValueX() {
			return _x_;
		}
		
		int getValueY() {
			return _y_;
		}
		
		boolean isActive() {
			return active;
		}
		
		private void setActive(boolean active) {
			this.active = active;
		}
		
		private Field(int x, int y) {
			_this_ = this;
			this._x_ = x;
			this._y_ = y;
			
			this.setPreferredSize(new Dimension(50, 50));
			this.setFont(new Font(null, Font.PLAIN, 40));
			this.setMargin(new Insets(0, 0, 0, 0));
			this.setBackground(Color.LIGHT_GRAY);
			
			// handler for mouse-rightclick
			// flag / un-flag a field
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						Game.markField(_this_);
					}
					
					updateSmilie(1);
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					updateSmilie(0);
				}
			});
			
			// handler for mouse-leftclick
			// uncovers a field (and all neighbours if possible)
			this.addActionListener(e -> {
				Game.reveralField(this);
			});
		}
		
		void updateField() {
			switch (Game.level[this._x_][this._y_]) {
				case GameConstants.FLAGGED:
					this.setBackground(Color.YELLOW);
					this.setText(Character.toString(0x2691));
					this.active = false;
					break;
				
				case GameConstants.BOMB:
					this.setBackground(Color.RED);
					this.setText(Character.toString(0x1F571));
					this.active = false;
					break;
				
				case GameConstants.FLAGGED_BOMB:
					if (!Game.isGameRunning) {
						this.setBackground(Color.GREEN);
						this.setText(Character.toString(0x2691));
					} else {
						this.setBackground(Color.YELLOW);
						this.setText(Character.toString(0x2691));
					}
					
					this.active = false;
					break;
				
				default:
					this.setBackground(Color.GRAY);
					if (Game.level[this._x_][this._y_] > '0') {
						this.setText("" + Game.level[this._x_][this._y_]);
						if (Game.level[this._x_][this._y_] == '1')
							this.setForeground(Color.GREEN);
						else if (Game.level[this._x_][this._y_] == '2')
							this.setForeground(Color.YELLOW);
						else if (Game.level[this._x_][this._y_] >= '3')
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
	
	GameBoard(int sizeX, int sizeY) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
	}
	
	List<Component> getGameFields() {
		return this.gameFields;
	}
	
	void updateAllFields() {
		for (Component c : this.gameFields) {
			if (c instanceof Field) {
				Field field = (Field) c;
				field.updateField();
				field.setActive(false);
			}
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
	
	@Override
	public void run() {
		this.lblSmiley.setFont(new Font(null, Font.BOLD, 50));
		this.lblSmiley.setAlignmentX(CENTER_ALIGNMENT);
		this.lblSmiley.setText(Character.toString(0x1F60A));
		
		this.pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.PAGE_AXIS));
		
		this.pnlMenu.add(Box.createVerticalGlue());
		this.pnlMenu.add(lblSmiley);
		this.pnlMenu.add(Box.createVerticalGlue());
		
		this.pnlMain.setLayout(new GridLayout(sizeX, sizeY));
		
		this.gameFields = new LinkedList<>();
		
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeY; j++) {
				Field f = new Field(i, j);
				this.gameFields.add(f);
				this.pnlMain.add(f);
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
		this.setVisible(true);
	}
}
