package game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

import game.data.Level;
import game.gui.GameBoard;
import game.gui.GameDialogs;
import game.util.DebugView;
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
	public static final char	FLAGGED			= 'P';
	public static final char	FLAGGED_BOMB	= '#';
	
	// public static fields
	///////////////////////
	public static boolean			newGame		= true;
	public static boolean			gameRunning	= true;
	public static boolean			victory		= false;
	public static CyclicBarrier		barrier;
	public static ExecutorService	threadPool;
	
	// private static fields
	////////////////////////
	private static char[][]					level;
	private static int						sizeY;
	private static int						sizeX;
	private static int						numBombs;
	private static int						safeFields;
	private static GameBoard				gameBoard;
	private static Map<String, Character>	touchedFields;
	
	/**
	 * calculates the fields of an level and updates the array before the game
	 * starts.
	 * 
	 * it traverses an two-dimensional array linear and checks the neighbor indexes
	 * for bombs. if it finds one it updates the current index and increases the
	 * value by 1.
	 */
	private static synchronized void calculateFields() {
		Thread.currentThread().setName("Calculate-Fields");
		
		for (int y = 0; y < level.length; y++) {
			for (int x = 0; x < level[y].length; x++) {
				if (level[y][x] == UNTOUCHED) {
					level[y][x] = EMPTY;
					
					for (int i = -1, j = 1; i <= 1; i++, j--) {
						// above-right <-> beneath-left
						try {
							if (level[y + i][x + j] == BOMB)
								level[y][x]++;
						} catch (Exception e) {
						}
						
						// above-left <-> beneath-right
						try {
							if (level[y + i][x + i] == BOMB)
								level[y][x]++;
						} catch (Exception e) {
						}
						
						// above <-> beneath
						try {
							if (level[y][x + i] == BOMB)
								level[y][x]++;
						} catch (Exception e) {
						}
						
						// left <-> right
						try {
							if (level[y + i][x] == BOMB)
								level[y][x]++;
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
		Thread.currentThread().setName("Revealing-Field-" + positionY + "x" + positionX);
		
		if (level[positionY][positionX] >= 'A') return;
		else if (level[positionY][positionX] == BOMB) {
			gameOver();
			return;
		}
		else if (level[positionY][positionX] == FLAGGED_BOMB);
		else if (level[positionY][positionX] == EMPTY) {
			for (int i = -1, j = 1; i <= 1; i++, j--) {
				if (i == 0)
					continue;
				
				// check left <-> right
				try {
					if (level[positionY][positionX + i] == BOMB);
					else if (level[positionY][positionX + i] == FLAGGED_BOMB);
					else if (level[positionY][positionX + i] == FLAGGED);
					else if (level[positionY][positionX + i] >= 'A');
					else {
						final int nextX = positionX + i;
						threadPool.execute(() -> revealField(positionY, nextX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
				
				// check above <-> beneath
				try {
					if (level[positionY + i][positionX] == BOMB);
					else if (level[positionY + i][positionX] == FLAGGED_BOMB);
					else if (level[positionY + i][positionX] == FLAGGED);
					else if (level[positionY + i][positionX] >= 'A');
					else {
						final int nextY = positionY + i;
						threadPool.execute(() -> revealField(nextY, positionX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
				
				// check above-left <-> beneath-right
				try {
					if (level[positionY + i][positionX + i] == BOMB);
					else if (level[positionY + i][positionX + i] == FLAGGED_BOMB);
					else if (level[positionY + i][positionX + i] == FLAGGED);
					else if (level[positionY + i][positionX + i] >= 'A');
					else {
						final int nextY = positionY + i;
						final int nextX = positionX + i;
						threadPool.execute(() -> revealField(nextY, nextX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
				
				// check above-right <-> beneath-left
				try {
					if (level[positionY + i][positionX + j] == BOMB);
					else if (level[positionY + i][positionX + j] == FLAGGED_BOMB);
					else if (level[positionY + i][positionX + j] == FLAGGED);
					else if (level[positionY + i][positionX + j] >= 'A');
					else {
						final int nextY = positionY + i;
						final int nextX = positionX + j;
						threadPool.execute(() -> revealField(nextY, nextX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
			}
			
			gameBoard.updateField(positionY, positionX, level[positionY][positionX]);
			level[positionY][positionX] += 17;
		} else {
			gameBoard.updateField(positionY, positionX, level[positionY][positionX]);
			level[positionY][positionX] += 17;
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
		gameRunning = false;
		gameBoard.updateSmilie(3);
		gameBoard.updateAllFields(level);
		
		JOptionPane.showMessageDialog(gameBoard, "Dude, you had ONE job...", "GAME OVER", JOptionPane.ERROR_MESSAGE);
		
		System.exit(0);
	}
	
	/**
	 * player has revealed all save fields in the level.
	 */
	private static void gameVictory() {
		gameRunning = false;
		victory = true;
		
		gameBoard.updateSmilie(2);
		gameBoard.updateAllFields(level);
		
		JOptionPane.showMessageDialog(gameBoard, "You have WON this level !!!", "VICTORY !!!",
				JOptionPane.INFORMATION_MESSAGE);
		
		System.exit(0);
	}
	
	/**
	 * saves the current level to a file on disc and produces some kind of
	 * 'savegame'. if the file already exists it will be ovewritten, if not a new
	 * file will be created.
	 * 
	 * @param path a path to a file on disc
	 */
	public static void saveToFile(Path path) {
		try {
			SaveGameUtility.saveToFile(path, level);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * loads a level from a file
	 */
	public static void loadFromFile() {
		Path filePath = GameDialogs.showLoadGameDialog();
		
		if (filePath == null)
			System.exit(0);
		
		if (Game.DEBUG)
			System.out.println("Loading level from file: " + filePath);
		
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
		
		touchedFields = new LinkedHashMap<>();
		
		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				if (level[i][j] == BOMB)
					numBombs++;
				else if (level[i][j] == FLAGGED_BOMB) {
					touchedFields.put(i + "-" + j, level[i][j]);
					numBombs++;
				}
				else if (level[i][j] == FLAGGED) {
					touchedFields.put(i + "-" + j, level[i][j]);
					safeFields++;
				}
				else if (level[i][j] >= 'A') {
					touchedFields.put(i + "-" + j, (char)(level[i][j] - 17));
				}
				else
					safeFields++;
			}
		}
		
		gameBoard = new GameBoard(sizeY, sizeX);
		newGame = false;
	}
	
	/**
	 * start a new game and present a dialog letting the player make the desired
	 * settings. generates a random level based on user choices.
	 */
	public static void newGame() {
		int[] newSettings = GameDialogs.showNewGameDialog();
		
		if (newSettings == null)
			System.exit(0);
		
		sizeY = newSettings[0];
		sizeX = newSettings[1];
		numBombs = newSettings[2];
		
		// determine the number of safe fields in the level
		safeFields = (sizeY * sizeX) - numBombs;
		
		level = Level.newLevel(sizeY, sizeX, numBombs);
		gameBoard = new GameBoard(sizeY, sizeX);
	}
	
	public static void printLevel() {
		DebugView.printLevel(level);
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
		
		Thread.currentThread().setName("Game-Mainthread");
		
		// ask user what to do: new game, load game or cancel.
		int userChoice = JOptionPane.showOptionDialog(null, "Choose wisely...", "jMinesweeper",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] { "New", "Load", "Cancel" }, null);
		
		switch (userChoice) {
			// start a new game
			case 0:
				barrier = new CyclicBarrier(3, () -> {
					if (DEBUG)
						System.out.println("Barrier - - - GAME START - - - breached.");
				});
				newGame();
				break;
			
			// load game from file
			case 1:
				barrier = new CyclicBarrier(2, () -> {
					if (DEBUG)
						System.out.println("Barrier - - - GAME START - - - breached.");
				});
				loadFromFile();
				break;
			
			// user clicked 'cancel' or an error occurred
			default:
				System.exit(0);
		}
		
		// calculate the level and render the gui in separate threads
		threadPool.execute(gameBoard);
		
		if (newGame)
			threadPool.execute(() -> calculateFields());
		
		// wait here for the previous tasks to finish to make sure
		// everything is nice and safe to proceed
		barrier.await();
		
		if (!newGame)
			gameBoard.updateTouchedFields(touchedFields);
		
		if (Game.DEBUG) {
			gameBoard.updateDebugLabel(
					"Size: " + sizeY + "x" + sizeX + " (Safe : " + safeFields + " Bombs: " + numBombs + ")");
			gameBoard.debugView(level);
		}
	}
}
