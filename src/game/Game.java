package game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

import game.data.Level;
import game.gui.GameBoard;
import game.gui.GameDialogs;
import game.util.SaveGameUtility;

/**
 * main class of the game
 * 
 * @author Holger Dörner
 */
public class Game {
	// global game constants
	////////////////////////
	public static final boolean	DEBUG			= true;
	public static final char	BOMB			= '@';
	public static final char	EMPTY			= '0';
	public static final char	UNTOUCHED		= 'O';
	public static final char	TOUCHED			= 'X';
	public static final char	FLAGGED			= 'P';
	public static final char	FLAGGED_BOMB	= '#';

	// public static fields
	///////////////////////
	public static boolean			isGameRunning	= true;
	public static boolean			isVictory		= false;
	public static CyclicBarrier		barrier;
	public static ExecutorService	threadPool;

	// private static fields
	////////////////////////
	private static char[][]		level;
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
	 * @param positionY the vertical position
	 * @param positionX the horizontal position
	 */
	public static synchronized void markField(final int positionY, final int positionX) {
		switch (level[positionY][positionX]) {
			case TOUCHED:
			case FLAGGED:
			case FLAGGED_BOMB:
				break;

			case BOMB:
				level[positionY][positionX] = FLAGGED_BOMB;
				threadPool.execute(() -> gameBoard.updateField(positionY, positionX, FLAGGED_BOMB));
				break;

			default:
				level[positionY][positionX] = FLAGGED;
				threadPool.execute(() -> gameBoard.updateField(positionY, positionX, FLAGGED));
				safeFields--;
				break;
		}

		if (DEBUG)
			gameBoard.updateDebugLabel(
					"Size: " + sizeY + "x" + sizeX + " (Safe : " + safeFields + " Bombs: " + numBombs + ")");
	}

	/**
	 * reveals a field and checks if neighbor fields can also be revealed. if this
	 * is the case it calls itself in a new process for the neighbor field and so on
	 * until no neighbor field is left to reveal.
	 * 
	 * @param positionY the vertical position of the field
	 * @param positionX the horizontal position of the field
	 */
	public static synchronized void revealField(final int positionY, final int positionX) {
		switch (level[positionY][positionX]) {
			case TOUCHED:
				return;

			case BOMB:
			case FLAGGED_BOMB:
				gameOver();
				return;

			case EMPTY:
				for (int i = -1, j = 1; i <= 1; i++, j--) {
					if (i == 0)
						continue;

					// check left <-> right
					try {
						if (level[positionY][positionX + i] != BOMB || level[positionY][positionX + i] != FLAGGED_BOMB
								|| level[positionY][positionX + i] != TOUCHED) {
							final int nextX = positionX + i;
							threadPool.execute(() -> revealField(positionY, nextX));
						}
					} catch (ArrayIndexOutOfBoundsException e) {
					}

					// check above <-> beneath
					try {
						if (level[positionY + i][positionX] != BOMB || level[positionY + i][positionX] != FLAGGED_BOMB
								|| level[positionY + i][positionX] != TOUCHED) {
							final int nextY = positionY + i;
							threadPool.execute(() -> revealField(nextY, positionX));
						}
					} catch (ArrayIndexOutOfBoundsException e) {
					}

					// check above-left <-> beneath-right
					try {
						if (level[positionY + i][positionX + i] != BOMB
								|| level[positionY + i][positionX + i] != FLAGGED_BOMB
								|| level[positionY + i][positionX + i] != TOUCHED) {
							final int nextY = positionY + i;
							final int nextX = positionX + i;
							threadPool.execute(() -> revealField(nextY, nextX));
						}
					} catch (ArrayIndexOutOfBoundsException e) {
					}

					// check above-right <-> beneath-left
					try {
						if (level[positionY + i][positionX + j] != BOMB
								|| level[positionY + i][positionX + j] != FLAGGED_BOMB
								|| level[positionY + i][positionX + j] != TOUCHED) {
							final int nextY = positionY + i;
							final int nextX = positionX + j;
							threadPool.execute(() -> revealField(nextY, nextX));
						}
					} catch (ArrayIndexOutOfBoundsException e) {
					}
				}

				gameBoard.updateField(positionY, positionX, level[positionY][positionX]);
				level[positionY][positionX] = TOUCHED;
				break;

			default:
				gameBoard.updateField(positionY, positionX, level[positionY][positionX]);
				level[positionY][positionX] = TOUCHED;
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
