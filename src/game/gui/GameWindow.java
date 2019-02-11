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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.BevelBorder;

import game.Game;

/**
 * main user interface of the game
 *
 * @author Holger Dörner
 *
 */
public class GameWindow extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;

	private JPanel pnlMain = new JPanel();
	private final JPanel pnlMenu = new JPanel();
	private final JLabel lblSmiley = new JLabel();
	private final JLabel lblTime = new JLabel("000");
	private final JLabel lblBombs = new JLabel("000");
	private final JLabel lblStatus = new JLabel(" ");
	private Map<String, Field> gameFields;

	/**
	 * inner class encapsulating the logic for the fields
	 *
	 * @author Holger Dörner
	 *
	 */
	class Field extends JButton {
		private static final long serialVersionUID = 1L;

		private final Field _this_;
		private final int positionY;
		private final int positionX;
		private boolean active = true;
		private boolean clicked = false;

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
		 * only used when global debugging is on.
		 *
		 * @param text the character in the level on the position of this field
		 */
		private void setDebugText(char text) {
			this.setText("" + text);
		}

		/**
		 * constructor of the field.
		 *
		 * @param y the vertical position of the field
		 * @param x the horizontal position of the field
		 */
		private Field(int y, int x) {
			_this_ = this;
			this.positionY = y;
			this.positionX = x;

			this.setPreferredSize(new Dimension(30, 30));
			this.setFont(new Font(null, Font.TRUETYPE_FONT, 20));
			this.setMargin(new Insets(0, 0, 0, 0));
			this.setBackground(Color.LIGHT_GRAY);
			this.setBorder(new BevelBorder(BevelBorder.RAISED));

			// handler for mouse-clicks
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 & _this_.active) { // left mouse-button
						Game.revealField(_this_.positionY, _this_.positionX);
						//_this_.active = false;
						//_this_.clicked = true;
					} else if (e.getButton() == MouseEvent.BUTTON3 & _this_.active) { // right mouse-button
						Game.markField(_this_.positionY, _this_.positionX);
					}

					if (Game.gameRunning)
						updateSmilie(1); // reset smiley when mouse-button is released

					if (Game.DEBUG)
						System.out.println("Clicked Field: " + _this_.getPositionY() + "x" + _this_.getPositionX()
								+ "\tMouse-Button: " + e.getButton());
				}

				@Override
				public void mousePressed(MouseEvent e) {
					// update smiley when mouse-button is pressed
					if (Game.gameRunning)
						updateSmilie(0);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}
			});
		}

		/**
		 * lets a field update itself and reveal what's underneath
		 *
		 * @param fieldValue represents the status hidden under the field
		 */
		void updateField(final char fieldValue) {
			if (fieldValue == Game.BOMB) {
				this.setText("\uD83D\uDCA3");
				if (this.clicked)
					this.setBackground(Color.RED);
			} else if (fieldValue == Game.FLAGGED_BOMB) {
				this.setText("\uD83C\uDFF4");
				this.setForeground(Color.RED);
			} else if (fieldValue >= 'a' & fieldValue <= 'i') {
				this.setText("\uD83C\uDFF4");
				this.setForeground(Color.RED);
			} else if (fieldValue == Game.UNTOUCHED) {
				this.setText("");
				this.setForeground(Color.BLACK);
			} else if (fieldValue == Game.EMPTY) {
				this.setBackground(Color.GRAY);
				this.setBorder(new BevelBorder(BevelBorder.LOWERED));
				this.setText("");
				this.active = false;
			} else {
				this.setBackground(Color.GRAY);
				this.setBorder(new BevelBorder(BevelBorder.LOWERED));
				if (fieldValue > Game.EMPTY & fieldValue < '9') {
					this.setText("" + fieldValue);
					if (fieldValue == '1')
						this.setForeground(Color.BLUE);
					else if (fieldValue == '2')
						this.setForeground(Color.GREEN);
					else if (fieldValue >= '3')
						this.setForeground(Color.RED);
				}
				this.active = false;
			}
		}
	}

	/**
	 * inner class encapsulating the logic for the menubar.
	 *
	 *  @author Holger Dörner
	 */
	private class MainMenu extends JMenuBar {
		private static final long serialVersionUID = 1L;

		private final JMenu gameMenu = new JMenu("Game");
		private final JMenu newGameMenu = new JMenu("New");
		private final JMenu debugMenu = new JMenu("Debug");
		private final JMenuItem newEasyGame = new JMenuItem("Easy");
		private final JMenuItem newMediumGame = new JMenuItem("Medium");
		private final JMenuItem newHardGame = new JMenuItem("Hard");
		private final JMenuItem newCustomGame = new JMenuItem("Custom");
		private final JMenuItem loadMenuItem = new JMenuItem("Load");
		private final JMenuItem saveMenuItem = new JMenuItem("Save");
		private final JMenuItem exitMenuItem = new JMenuItem("Exit");
		private final JMenuItem dbgPrintMenuItem = new JMenuItem("Print level to console");

		private MainMenu() {
			newEasyGame.addActionListener(e -> Game.newGame(8, 8, 10));
			newMediumGame.addActionListener(e -> Game.newGame(16, 16, 40));
			newHardGame.addActionListener(e -> Game.newGame(16, 30, 99));
			newCustomGame.addActionListener(e -> Game.newGame());
			loadMenuItem.addActionListener(e -> Game.loadFromFile());
			saveMenuItem.addActionListener(e -> Game.saveToFile());
			exitMenuItem.addActionListener(e -> Game.exitGame());

			// only in DEBUG-mode
			dbgPrintMenuItem.addActionListener(e -> Game.printLevel());

			newGameMenu.add(newEasyGame);
			newGameMenu.add(newMediumGame);
			newGameMenu.add(newHardGame);
			newGameMenu.add(new JSeparator());
			newGameMenu.add(newCustomGame);

			gameMenu.add(newGameMenu);
			gameMenu.add(new JSeparator());
			gameMenu.add(loadMenuItem);
			gameMenu.add(saveMenuItem);
			gameMenu.add(new JSeparator());
			gameMenu.add(exitMenuItem);

			debugMenu.add(dbgPrintMenuItem);

			this.add(gameMenu);

			if (Game.DEBUG)
				this.add(debugMenu);
		}
	}

	/**
	 * default no-args constructor for the gui
	 */
	public GameWindow() {
	}

	/**
	 *
	 * @return a list of all fields on the gameboard
	 */
	Map<String, Field> getGameFields() {
		return this.gameFields;
	}

	/**
	 * updates touched fields on the gameboard when a level is loaded from file.
	 *
	 * format for the key is 'y-x'.
	 *
	 * @param touchedFields a java.util.Map containing the field-data
	 */
	public void updateTouchedFields(final Map<String, Character> touchedFields) {
		touchedFields.entrySet().forEach(
				entry -> Game.threadPool.execute(() -> gameFields.get(entry.getKey()).updateField(entry.getValue())));
	}

	/**
	 * updates all fields on the board
	 *
	 * @param data the level data
	 */
	public void updateAllFields(final char[][] data) {
		gameFields.values().stream().filter(field -> field.isActive()).forEach(field -> {
			Game.threadPool.execute(() -> {
				field.updateField(data[field.getPositionY()][field.getPositionX()]);
			});
		});

	}

	/**
	 * updates a single field on the gameboard.
	 *
	 * @param y      the vertical position on the gameboard
	 * @param x      the horizontal position on the gameboard
	 * @param status the status for the field to set
	 */
	public void updateField(final int y, final int x, final char status) {
		gameFields.get(y + "-" + x).updateField(status);
	}

	/**
	 * most important method in the game, makes the smiley alive ;-)
	 *
	 * <ul>
	 * <li>0 - mouse down</li>
	 * <li>1 - mouse up</li>
	 * <li>2 - victory</li>
	 * <li>3 - defeat</li>
	 * </ul>
	 *
	 * @param status the status of the smiley (0-3)
	 */
	public void updateSmilie(int status) {
		switch (status) {
		// mouse down
		case 0:
			this.lblSmiley.setText("\uD83D\uDE2F");
			break;

		// mouse up / default
		case 1:
			this.lblSmiley.setText("\uD83D\uDE0A");
			break;

		// victory
		case 2:
			this.lblSmiley.setText("\uD83D\uDE0E");
			break;

		// defeat
		case 3:
			this.lblSmiley.setText("\uD83D\uDE2D");
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
	 * updates the status-label beneath the gamefield.
	 *
	 * @param statusText a java.lang.String containing the text to display
	 */
	public void updateStatusLabel(String statusText) {
		this.lblStatus.setText(statusText);
	}

	public void updateTimer(String time) {
		if (time.length() == 1)
            this.lblTime.setText("00" + time);
        else if (time.length() == 2)
            this.lblTime.setText("0" + time);
        else if (time.length() == 3)
            this.lblTime.setText(time);
	}

	public void updateBombCounter(String numBombs) {
		if (numBombs.length() == 1)
			numBombs = "00" + numBombs;
		if (numBombs.length() == 2)
			numBombs = "0" + numBombs;

		this.lblBombs.setText(numBombs);
	}

	/**
	 * generates a new board inside of the main-window. should a board already
	 * exist, it gets deleted.
	 *
	 * @param sizeY the vertical size
	 * @param sizeX the horizontal size
	 */
	public void newBoard(final int sizeY, final int sizeX) {
		this.remove(pnlMain);
		this.pnlMain = new JPanel();
		this.pnlMain.setLayout(new GridLayout(sizeY, sizeX));

		this.gameFields = new LinkedHashMap<>();

		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				Field f = new Field(i, j);
				this.gameFields.put(i + "-" + j, f);
				this.pnlMain.add(f);

				if (Game.DEBUG)
					System.out.println("Generated Field: y" + i + " x" + j);
			}
		}

		this.add(pnlMain, BorderLayout.CENTER);
		this.pack();

		updateSmilie(1);
	}

	/**
	 * let the gui run in its own thread.
	 */
	@Override
	public void run() {
		Thread.currentThread().setName("User-Interface");

		this.lblSmiley.setFont(new Font(null, Font.BOLD, 50));
		this.lblSmiley.setAlignmentX(CENTER_ALIGNMENT);
		this.lblSmiley.setText("\uD83D\uDE0A");

		this.lblTime.setFont(new Font(null, Font.BOLD, 30));

		this.lblBombs.setFont(new Font(null, Font.BOLD, 30));

		this.pnlMenu.add(lblBombs);
		this.pnlMenu.add(Box.createHorizontalStrut(20));
		this.pnlMenu.add(lblSmiley);
		this.pnlMenu.add(Box.createHorizontalStrut(20));
		this.pnlMenu.add(lblTime);

		this.setLayout(new BorderLayout());

		this.setJMenuBar(new MainMenu());

		this.add(pnlMenu, BorderLayout.NORTH);
		this.add(lblStatus, BorderLayout.SOUTH);

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

		this.setVisible(true);
	}
}
