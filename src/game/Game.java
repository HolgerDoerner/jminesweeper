package game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import game.data.Level;
import game.gui.GameDialogs;
import game.gui.GameWindow;
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
	public static volatile boolean	gameRunning	= true;
	public static volatile boolean	victory		= false;
	public static CyclicBarrier		barrier;
	public static ExecutorService	threadPool;
	
	// private static fields
	////////////////////////
	private static Level					level;
	private static int						sizeY;
	private static int						sizeX;
	private static int						numBombs;
	private static int						numFlags;
	private static int						safeFields;
	private static GameWindow				gameWindow;
	private static Map<String, Character>	touchedFields;
	private static Future<?> 				timer;
	
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
		
		for (int y = 0; y < level.getSizeY(); y++) {
			for (int x = 0; x < level.getSizeX(); x++) {
				if (level.get(y, x) == UNTOUCHED) {
					level.set(y, x, EMPTY);
					
					for (int i = -1, j = 1; i <= 1; i++, j--) {
						// above-right <-> beneath-left
						try {
							if (level.get(y + i, x + j) == BOMB)
								level.increment(y, x);
						} catch (Exception e) {
						}
						
						// above-left <-> beneath-right
						try {
							if (level.get(y + i, x + i) == BOMB)
								level.increment(y, x);
						} catch (Exception e) {
						}
						
						// above <-> beneath
						try {
							if (level.get(y, x + i) == BOMB)
								level.increment(y, x);
						} catch (Exception e) {
						}
						
						// left <-> right
						try {
							if (level.get(y + i, x) == BOMB)
								level.increment(y, x);
						} catch (Exception e) {
						}
					}
				}
			}
		}
	}
	
	/**
	 * marks field with a flag when the player right-clicks on it
	 * 
	 * @param positionY the vertical position
	 * @param positionX the horizontal position
	 */
	public static synchronized void markField(final int positionY, final int positionX) {
		if (numFlags == 0)
			return;
		
		switch (level.get(positionY, positionX)) {
			case FLAGGED:
			case FLAGGED_BOMB:
				break;
			
			case BOMB:
				level.set(positionY, positionX, FLAGGED_BOMB);
				threadPool.execute(() -> gameWindow.updateField(positionY, positionX, FLAGGED_BOMB));
				break;
			
			default:
				level.set(positionY, positionX, FLAGGED);
				threadPool.execute(() -> gameWindow.updateField(positionY, positionX, FLAGGED));
				safeFields--;
				break;
		}
		
		numFlags--;
		
		gameWindow.updateFlagCounter("" + numFlags);
		gameWindow.updateStatusLabel("Size: " + sizeY + "x" + sizeX + " Bombs: " + numBombs);
		
		if (safeFields == 0)
			gameVictory();
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
		
		if (level.get(positionY, positionX) >= 'A')
			return;
		else if (level.get(positionY, positionX) == BOMB) {
			gameOver();
			return;
		} else if (level.get(positionY, positionX) == FLAGGED_BOMB)
			;
		else if (level.get(positionY, positionX) == EMPTY) {
			for (int i = -1, j = 1; i <= 1; i++, j--) {
				if (i == 0)
					continue;
				
				// check left <-> right
				try {
					if (level.get(positionY, positionX + i) == BOMB)
						;
					else if (level.get(positionY, positionX + i) == FLAGGED_BOMB)
						;
					else if (level.get(positionY, positionX + i) == FLAGGED)
						;
					else if (level.get(positionY, positionX + i) >= 'A')
						;
					else {
						final int nextX = positionX + i;
						threadPool.execute(() -> revealField(positionY, nextX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
				
				// check above <-> beneath
				try {
					if (level.get(positionY + i, positionX) == BOMB)
						;
					else if (level.get(positionY + i, positionX) == FLAGGED_BOMB)
						;
					else if (level.get(positionY + i, positionX) == FLAGGED)
						;
					else if (level.get(positionY + i, positionX) >= 'A')
						;
					else {
						final int nextY = positionY + i;
						threadPool.execute(() -> revealField(nextY, positionX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
				
				// check above-left <-> beneath-right
				try {
					if (level.get(positionY + i, positionX + i) == BOMB)
						;
					else if (level.get(positionY + i, positionX + i) == FLAGGED_BOMB)
						;
					else if (level.get(positionY + i, positionX + i) == FLAGGED)
						;
					else if (level.get(positionY + i, positionX + i) >= 'A')
						;
					else {
						final int nextY = positionY + i;
						final int nextX = positionX + i;
						threadPool.execute(() -> revealField(nextY, nextX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
				
				// check above-right <-> beneath-left
				try {
					if (level.get(positionY + i, positionX + j) == BOMB)
						;
					else if (level.get(positionY + i, positionX + j) == FLAGGED_BOMB)
						;
					else if (level.get(positionY + i, positionX + j) == FLAGGED)
						;
					else if (level.get(positionY + i, positionX + j) >= 'A')
						;
					else {
						final int nextY = positionY + i;
						final int nextX = positionX + j;
						threadPool.execute(() -> revealField(nextY, nextX));
					}
				} catch (ArrayIndexOutOfBoundsException e) {
				}
			}
			
			gameWindow.updateField(positionY, positionX, level.get(positionY, positionX));
			level.set(positionY, positionX, (char) (level.get(positionY, positionX) + 17));
		} else {
			gameWindow.updateField(positionY, positionX, level.get(positionY, positionX));
			level.set(positionY, positionX, (char) (level.get(positionY, positionX) + 17));
		}
		
		safeFields--;
		
		gameWindow.updateFlagCounter("" + numFlags);
		gameWindow.updateStatusLabel("Size: " + sizeY + "x" + sizeX + " Bombs: " + numBombs);
		
		if (safeFields == 0)
			gameVictory();
	}
	
	/**
	 * player clicked on a bomb-field, too bad...
	 */
	private static void gameOver() {
		stopTimer();
		
		gameRunning = false;
		victory = false;
		
		gameWindow.updateAllFields(level.getLevelData());
		gameWindow.updateSmilie(3);
		
		Game.threadPool.execute(() -> GameDialogs.showDefeatDialog(gameWindow));
	}
	
	/**
	 * player has revealed all save fields in the level.
	 */
	private static void gameVictory() {
		stopTimer();
		
		gameRunning = false;
		victory = true;
		
		gameWindow.updateAllFields(level.getLevelData());
		gameWindow.updateSmilie(2);
		
		Game.threadPool.execute(() -> GameDialogs.showVictoryDialog(gameWindow));
	}
	
	/**
	 * saves the current level to a file on disc and produces some kind of
	 * 'savegame'. if the file already exists it will be ovewritten, if not a new
	 * file will be created.
	 * 
	 * @param path a path to a file on disc
	 */
	public static void saveToFile() {
		Path path = GameDialogs.showSaveGameDialog(gameWindow);
		
		try {
			SaveGameUtility.saveToFile(path, level.getLevelData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * loads a level from a file
	 */
	public static void loadFromFile() {
		stopTimer();
		
		gameRunning = false;
		
		sizeY = 0;
		sizeX = 0;
		numBombs = 0;
		numFlags = 0;
		safeFields = 0;
		
		Path filePath = GameDialogs.showLoadGameDialog(gameWindow);
		
		if (filePath == null)
			System.exit(0);
		
		if (Game.DEBUG)
			System.out.println("Loading level from file: " + filePath);
		
		try {
			level = Level.fromExistingData(SaveGameUtility.readFromFile(filePath));
		} catch (IOException e) {
			if (Game.DEBUG)
				e.printStackTrace();
			System.exit(1);
		} finally {
			if (level == null)
				System.exit(1);
		}
		
		sizeY = level.getSizeY();
		sizeX = level.getSizeX();
		
		numFlags = (sizeY * sizeX) / 2;
		
		touchedFields = new LinkedHashMap<>();
		
		// determine the state of the savegame.
		// count bombs and safe fields, determine wich fields are
		// allready 'touched'
		for (int y = 0; y < sizeY; y++) {
			for (int x = 0; x < sizeX; x++) {
				if (level.get(y, x) == BOMB)
					numBombs++;
				else if (level.get(y, x) == FLAGGED_BOMB) {
					touchedFields.put(y + "-" + x, level.get(y, x));
					numBombs++;
				} else if (level.get(y, x) == FLAGGED) {
					touchedFields.put(y + "-" + x, level.get(y, x));
				} else if (level.get(y, x) >= 'A') {
					touchedFields.put(y + "-" + x, (char) (level.get(y, x) - 17));
					level.set(y, x, (char) (level.get(y, x) - 17));
				} else
					safeFields++;
			}
		}
		
		gameWindow.newBoard(sizeY, sizeX);
		
		if (DEBUG)
			gameWindow.debugView(level.getLevelData());
		
		gameWindow.updateTouchedFields(touchedFields);
		gameWindow.updateTimer("000");
		gameWindow.updateFlagCounter("" + numFlags);
		gameWindow.updateStatusLabel("Size: " + sizeY + "x" + sizeX + " Bombs: " + numBombs);
		
		gameRunning = true;
		
		startTimer();
	}
	
	/**
	 * <p>
	 * start a new game and present a dialog letting the player make the desired
	 * settings.
	 * </p>
	 * 
	 * <p>
	 * deligates to newGame(int, int, int).
	 * </p>
	 * 
	 * @see game.Game.newGame(int,int,int)
	 */
	public static void newGame() {
		int[] newSettings = GameDialogs.showNewGameDialog(gameWindow);
		
		if (newSettings == null)
			return;
		
		newGame(newSettings[0], newSettings[1], newSettings[2]);
	}
	
	/**
	 * starts a new game with the settings passed as arguments.
	 * 
	 * @param y the vertical size
	 * @param x the horizontal size
	 * @param b the number of bombs
	 */
	public static void newGame(final int y, final int x, final int b) {
		stopTimer();
		
		gameRunning = false;
		
		sizeY = y;
		sizeX = x;
		numBombs = b;
		numFlags = (y * x) / 2;
		
		safeFields = (sizeY * sizeX) - numBombs;
		
		level = Level.generateNew(sizeY, sizeX, numBombs);
		
		threadPool.execute(() -> calculateFields());
		
		gameWindow.newBoard(sizeY, sizeX);
		
		gameWindow.updateTimer("000");
		gameWindow.updateFlagCounter("" + numFlags);
		gameWindow.updateStatusLabel("Size: " + sizeY + "x" + sizeX + " Bombs: " + numBombs);
		
		gameRunning = true;
		
		startTimer();
		
		if (DEBUG)
			gameWindow.debugView(level.getLevelData());
	}
	
	/**
	 * prints the current level-layout to console. only used when DEBUG=true.
	 */
	public static void printLevel() {
		DebugView.printLevel(level.getLevelData());
	}
	
	/**
	 * wrapper-method around System.exit()
	 * 
	 * @see java.lang.System.exit
	 */
	public static void exitGame() {
		System.exit(0);
	}
	
	/**
	 * measures the elapsed time for the current level in seconds.
	 */
	private static void startTimer() {
		timer = threadPool.submit(() -> {
			Thread.currentThread().setName("Time-Counter");
			
			if (DEBUG)
				System.out.println(Thread.currentThread().getName() + " started!");
			
			int time = 0;
			
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(1000);
					gameWindow.updateTimer("" + time++);
				} catch (InterruptedException e) {
					break;
				}
			}
			
			if (DEBUG)
				System.out.println(Thread.currentThread().getName() + " stopped!");
			
			return;
		});
	}
	
	/**
	 * resets the timer by setting time to 0 (zero)
	 */
	private static void stopTimer() {
		if (timer != null)
			timer.cancel(true);
	}
	
	/**
	 * entry point of the game
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws BrokenBarrierException
	 */
	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
		barrier = new CyclicBarrier(2, () -> {
			if (DEBUG)
				System.out.println("Barrier - - - GAME START - - - breached");
		});
		
		threadPool = Executors.newFixedThreadPool(4);
		Thread.currentThread().setName("Main-Thread");
		gameWindow = new GameWindow();
		threadPool.execute(gameWindow);
		
		// at startup always start a default game.
		// y=8, x=8, bombs=10
		newGame(8, 8, 10);
		
		barrier.await();
	}
}
