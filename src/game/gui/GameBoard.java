package game.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
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

import game.Game;

/**
 * main user interface of the game
 * 
 * @author Holger Dörner
 * 
 */
public class GameBoard extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;

	private final JPanel	pnlMain		= new JPanel();
	private final JPanel	pnlMenu		= new JPanel();
	private final JLabel	lblSmiley	= new JLabel();
	private final JLabel	lblDebug	= new JLabel();

	private final int			sizeY;
	private final int			sizeX;
	private Map<String, Field>	gameFields;

	/**
	 * inner class encapsulating the logic for the fields
	 * 
	 * @author Holger Dörner
	 * 
	 */
	class Field extends JButton {
		private static final long serialVersionUID = 1L;

		private final Field	_this_;
		private final int	positionY;
		private final int	positionX;
		private boolean		active	= true;

		/**
		 * getter for the y-position
		 * 
		 * @return the y-position of the field on the gameboard
		 * 
		 */
		int getPositionY() {
			return positionY;
		}

		/**
		 * getter for the x-position
		 * 
		 * @return the x-position of the field on the gameboard
		 * 
		 */
		int getPositionX() {
			return positionX;
		}

		/**
		 * getter to retrieve the status of a field.
		 * 
		 * true = the field wasn't revealed. false = the field is revealed.
		 * 
		 * @return boolean indicating the status of the field
		 * 
		 */
		boolean isActive() {
			return active;
		}

		/**
		 * setter to set the status of the field.
		 * 
		 * <ul>
		 * <li>true = the field wasn't revealed</li>
		 * <li>false = the field is revealed</li>
		 * </ul>
		 * 
		 * @param active indicating the new status of the field
		 * 
		 */
		private void setActive(boolean active) {
			this.active = active;
		}

		/**
		 * only used when global debugging is on.
		 * 
		 * @param text the character in the level on the position of this field
		 * 
		 */
		private void setDebugText(char text) {
			this.setText("" + text);
		}

		/**
		 * constructor of the field.
		 * 
		 * @param y the vertical position of the field
		 * @param x the horizontal position of the field
		 * 
		 */
		private Field(int y, int x) {
			_this_ = this;
			this.positionY = y;
			this.positionX = x;

			this.setPreferredSize(new Dimension(40, 40));
			this.setFont(new Font(null, Font.PLAIN, 30));
			this.setMargin(new Insets(0, 0, 0, 0));
			this.setBackground(Color.LIGHT_GRAY);

			// handler for mouse-clicks
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) // left mouse-button
						Game.revealField(_this_.positionY, _this_.positionX);
					else if (e.getButton() == MouseEvent.BUTTON3) // right mouse-button
						Game.markField(_this_.positionY, _this_.positionX);

					updateSmilie(1); // update smiley when mouse-button is pressed

					if (Game.DEBUG)
						System.out.println("Clicked Field: " + _this_.getPositionY() + "x" + _this_.getPositionX()
								+ "\tMouse-Button: " + e.getButton());
				}

				// reset smiley when mouse-button is released
				@Override
				public void mousePressed(MouseEvent e) {
					updateSmilie(0);
				}
			});
		}

		/**
		 * lets a field update itself and reveal what's underneath
		 * 
		 * @param fieldValue represents the status hidden under the field
		 * 
		 */
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

				case Game.TOUCHED:
					this.setBackground(Color.GRAY);
					this.active = false;
					break;

				// safe field. just give values >0 some color.
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

	/**
	 * inner class encapsulating the logic for the menubar.
	 * 
	 * Holger Dörner
	 *
	 */
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

	/**
	 * constructor for the gameboard
	 * 
	 * @param sizeY the vertical size of the board
	 * @param sizeX the horizontal size of the board
	 */
	public GameBoard(int sizeY, int sizeX) {
		this.sizeY = sizeY;
		this.sizeX = sizeX;
	}

	/**
	 * 
	 * @return a list of all fields on the gameboard
	 */
	Map<String, Field> getGameFields() {
		return this.gameFields;
	}

	/**
	 * updates all fields on the board
	 * 
	 * @param data the level data
	 */
	public void updateAllFields(final char[][] data) {
		gameFields.values().stream().filter(field -> field.isActive()).forEach(field -> {
			field.updateField(data[field.getPositionY()][field.getPositionX()]);
			field.setActive(false);
		});
	}

	public void updateField(final int y, final int x, final char status) {
		gameFields.get(y + "-" + x).updateField(status);
	}

	/**
	 * most important method in the game, makes the smiley alive ;-)
	 * 
	 * @param status the status of the smiley (0-3)
	 */
	public void updateSmilie(int status) {
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

	/**
	 * shows the raw level-data on the fields. only used when Game.DEBUG is set.
	 * 
	 * @param data an array containing the level data
	 */
	public void debugView(char[][] data) {
		for (Field field : gameFields.values()) {
			field.setDebugText(data[field.getPositionY()][field.getPositionX()]);
		}
	}

	/**
	 * updates the label beneath the gamefield. only used in debug-mode
	 * 
	 * @param debugText a java.lang.String containing the text to display
	 */
	public void updateDebugLabel(String debugText) {
		this.lblDebug.setText(debugText);
	}

	/**
	 * lets the gui run in its own thread.
	 */
	@Override
	public void run() {
		Thread.currentThread().setName("User-Interface");

		this.lblSmiley.setFont(new Font(null, Font.BOLD, 50));
		this.lblSmiley.setAlignmentX(CENTER_ALIGNMENT);
		this.lblSmiley.setText(Character.toString(0x1F60A));

		this.pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.PAGE_AXIS));

		this.pnlMenu.add(Box.createVerticalGlue());
		this.pnlMenu.add(lblSmiley);
		this.pnlMenu.add(Box.createVerticalGlue());

		this.pnlMain.setLayout(new GridLayout(sizeY, sizeX));

		this.gameFields = new LinkedHashMap<>();

		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				Field f = new Field(i, j);
				this.gameFields.put(i + "-" + j, f);
				this.pnlMain.add(f);

				if (Game.DEBUG)
					System.out.println("Generated Field: y" + f.getPositionY() + " x" + f.getPositionX());
			}
		}

		this.setLayout(new BorderLayout());

		this.setJMenuBar(new MainMenu());

		this.add(pnlMenu, BorderLayout.NORTH);
		this.add(pnlMain, BorderLayout.CENTER);

		if (Game.DEBUG)
			this.add(lblDebug, BorderLayout.SOUTH);

		this.setTitle("jMinesweeper");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.pack();

		// at this point we make sure that everything is ready
		// so we have to wait for the other tasks to finish their job
		try {
			Game.barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			if (Game.DEBUG)
				e.printStackTrace();
		}

		this.setResizable(false);
		this.setMaximumSize(new Dimension(500, 500));
		this.setVisible(true);
	}
}
