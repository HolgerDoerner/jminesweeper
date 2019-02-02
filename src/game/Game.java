package game;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

/**
 * main class of the game
 * 
 * @author Holger Dörner
 */
public class Game {
	// game constants
	/////////////////
	static final char		BOMB			= '@';
	static final char		EMPTY			= '0';
	static final char		UNTOUCHED		= 'O';
	static final char		FLAGGED			= 'P';
	static final char		FLAGGED_BOMB	= '#';
	static final boolean	DEBUG			= true;

	// static fields
	////////////////
	static ExecutorService	threadPool;
	static CyclicBarrier	barrier;
	static char[][]			level;
	static boolean			isGameRunning	= true;
	static boolean			isVictory		= false;

	// static private fields
	////////////////////////
	private static int			sizeY;
	private static int			sizeX;
	private static int			numBombs;
	private static int			safeFields;
	private static GameBoard	gameBoard;

	/**
	 * calculates the fields of an level and updates the array before the game
	 * starts.
	 * 
	 * it traverses an two-dimensional array linear and checks the neighbor indexes
	 * for bombs. if it finds one it updates the current index and increases the
	 * value by 1.
	 */
	private static synchronized void calculateFields() {
		for (int y = 0; y < level.length; y++) {
			for (int x = 0; x < level[y].length; x++) {
				if (level[y][x] == 'O') {
					level[y][x] = '0';

					for (int i = -1, j = 1; i <= 1; i++, j--) {
						try {
							if (level[y + i][x + j] == '@') {
								level[y][x]++;
							}
						} catch (Exception e) {
						}
						try {
							if (level[y + i][x + i] == '@') {
								level[y][x]++;
							}
						} catch (Exception e) {
						}
						try {
							if (level[y][x + i] == '@') {
								level[y][x]++;
							}
						} catch (Exception e) {
						}
						try {
							if (level[y + i][x] == '@') {
								level[y][x]++;
							}
						} catch (Exception e) {
						}
					}
				}
			}
		}

		// wait here until the other tasks are finished
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			if (DEBUG)
				e.printStackTrace();
		}
	}

	/**
	 * marks field with a flag when the player right-clicks on it
	 * 
	 * @param field GameBoard.Field representing an element of the GameBoard in the
	 *              GUI
	 */
	public static void markField(final GameBoard.Field field) {
		if (!field.isActive())
			return;

		threadPool.execute(() -> {
			switch (level[field.getValueY()][field.getValueX()]) {
				case Game.BOMB:
					level[field.getValueY()][field.getValueX()] = Game.FLAGGED_BOMB;
					field.updateField(level[field.getValueY()][field.getValueX()]);
					break;

				default:
					level[field.getValueY()][field.getValueX()] = Game.FLAGGED;
					field.updateField(level[field.getValueY()][field.getValueX()]);
					safeFields--;
					break;
			}
		});
	}

	/**
	 * reveals a field and checks if neighbor fields can also be revealed. if this
	 * is the case it calls itself in a new process for the neighbor field and so on
	 * until no neighbor field is left to reveal.
	 * 
	 * @param field GameBoard.Field representing an element of the GameBoard in the
	 *              GUI
	 */
	public static synchronized void reveralField(final GameBoard.Field field) {
		if (!field.isActive())
			return;

		switch (level[field.getValueY()][field.getValueX()]) {
			// clicking on a bomb is usually a bad idea...
			case Game.BOMB:
			case Game.FLAGGED_BOMB:
				field.updateField(level[field.getValueY()][field.getValueX()]);
				gameOver();
				break;

			// if the field is empty (means no neighbor fields are bombs) it will be
			// it will be revealed and the neighbor fields are checked.
			case Game.EMPTY:
				// process the neighbor fields
				for (Component c : gameBoard.getGameFields()) {
					if (c instanceof GameBoard.Field) {
						GameBoard.Field other = (GameBoard.Field) c;

						for (int i = -1; i <= +1; i++) {
							if (other.getValueY() == field.getValueY() & other.getValueX() == field.getValueX()) {
								continue;
							}

							if ((other.getValueY() == field.getValueY() + i && other.getValueX() == field.getValueX())
									| (other.getValueX() == field.getValueX() + i
											&& other.getValueY() == field.getValueY())) {
								if (level[other.getValueY()][other.getValueX()] == Game.BOMB
										| level[other.getValueY()][other.getValueX()] == Game.FLAGGED_BOMB) {
									break;
								}

								threadPool.execute(() -> reveralField(other));
							}

						}
					}
				}

				field.updateField(level[field.getValueY()][field.getValueX()]);
				break;

			// if the value of the field is >0 than only this field gets revealed.
			default:
				field.updateField(level[field.getValueY()][field.getValueX()]);
				break;
		}

		safeFields--;

		if (DEBUG)
			gameBoard.updateDebugLabel(
					"Size: " + sizeY + "x" + sizeX + " (Safe : " + safeFields + " Bombs: " + numBombs + ")");

		if (safeFields == 0)
			gameVictory();
	}

	/**
	 * player clicked on a bomb-field, too bad...
	 */
	private static void gameOver() {
		isGameRunning = false;
		gameBoard.updateSmilie(3);
		gameBoard.updateAllFields(level);

		JOptionPane.showMessageDialog(gameBoard, "Dude, you had ONE job...", "GAME OVER", JOptionPane.ERROR_MESSAGE);

		System.exit(0);
	}

	/**
	 * player has revealed all save fields in the level.
	 */
	private static void gameVictory() {
		isGameRunning = false;
		isVictory = true;

		gameBoard.updateSmilie(2);
		gameBoard.updateAllFields(level);

		JOptionPane.showMessageDialog(gameBoard, "You have WON this level !!!", "VICTORY !!!",
				JOptionPane.INFORMATION_MESSAGE);

		System.exit(0);
	}

	/**
	 * entry point of the game
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws BrokenBarrierException
	 */
	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
		threadPool = Executors.newFixedThreadPool(4);
		barrier = new CyclicBarrier(3);

		// ask user what to do: new game, load game or cancel.
		int userChoice = JOptionPane.showOptionDialog(null, "Choose wisely...", "New Game",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] { "New", "Load", "Cancel" }, null);

		switch (userChoice) {
			// start a new game
			case 0:
				int[] newSettings = GameDialogs.showNewGameDialog();

				if (newSettings == null)
					System.exit(0);

				sizeY = newSettings[0];
				sizeX = newSettings[1];
				numBombs = newSettings[2];

				level = Level.newLevel(sizeY, sizeX, numBombs);
				gameBoard = new GameBoard(sizeY, sizeX);
				break;

			// load game from file
			case 1:
				Path filePath = GameDialogs.showLoadGameDialog();

				if (Game.DEBUG)
					System.out.println(filePath);

				if (filePath == null)
					System.exit(0);

				try {
					level = SaveGameUtility.readFromFile(filePath);
				} catch (IOException e) {
					if (Game.DEBUG)
						e.printStackTrace();
					System.exit(1);
				} finally {
					if (level == null)
						System.exit(1);
				}

				sizeY = level.length;
				sizeX = level[0].length;

				for (char[] c1 : level) {
					for (char c2 : c1) {
						if (c2 == '@')
							numBombs++;
					}
				}

				System.out.println(numBombs);

				gameBoard = new GameBoard(sizeY, sizeX);
				break;

			// user clicked 'cancel'
			default:
				System.exit(0);
		}

		safeFields = (sizeY * sizeX) - numBombs;

		// calculate the level and the gui in separate threads
		threadPool.execute(gameBoard);
		threadPool.execute(() -> calculateFields());

		// wait here for the previous tasks to finish to make sure
		// everything is nice and safe to proceed
		barrier.await();

		if (Game.DEBUG) {
			gameBoard.updateDebugLabel(
					"Size: " + sizeY + "x" + sizeX + " (Safe : " + safeFields + " Bombs: " + numBombs + ")");
			gameBoard.debugView(level);
		}
	}
}
