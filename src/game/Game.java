package game;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

public class Game {
	// game constants
	/////////////////
	static final char		BOMB			= '@';
	static final char		EMPTY			= '0';
	static final char		UNTOUCHED		= 'O';
	static final char		FLAGGED			= 'P';
	static final char		FLAGGED_BOMB	= '#';
	static final boolean	DEBUG			= false;
	
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
	
	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
		threadPool = Executors.newFixedThreadPool(4);
		barrier = new CyclicBarrier(3);
		
		int userChoice = JOptionPane.showOptionDialog(null, "Choose wisely...", "New Game",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] { "New", "Load", "Cancel" }, null);
		
		switch (userChoice) {
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
			
			default:
				System.exit(0);
		}
		
		safeFields = (sizeY * sizeX) - numBombs;
		
		threadPool.execute(gameBoard);
		threadPool.execute(() -> calculateFields());
		
		// wait here for the previous tasks to finish to make sure
		// everithing is nice and safe to proceed
		barrier.await();
		
		if (Game.DEBUG) {
			System.out
					.println("Level: " + (sizeY * sizeX) + " (Safe : " + safeFields + " Bombs: " + numBombs + ")");
			gameBoard.debugView(level);
		}
	}
	
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
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
	
	public static synchronized void reveralField(final GameBoard.Field field) {
		if (!field.isActive())
			return;
		
		switch (level[field.getValueY()][field.getValueX()]) {
			case Game.BOMB:
			case Game.FLAGGED_BOMB:
				field.updateField(level[field.getValueY()][field.getValueX()]);
				gameOver();
				break;
			
			case Game.EMPTY:
				// process the neighbour fields
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
			
			default:
				field.updateField(level[field.getValueY()][field.getValueX()]);
				break;
		}
		
		safeFields--;
		
		if (safeFields == 0)
			gameVictory();
	}
	
	private static void gameOver() {
		isGameRunning = false;
		gameBoard.updateSmilie(3);
		gameBoard.updateAllFields(level);
		
		JOptionPane.showMessageDialog(gameBoard, "Dude, you had ONE job...", "GAME OVER", JOptionPane.ERROR_MESSAGE);
		
		System.exit(0);
	}
	
	private static void gameVictory() {
		isGameRunning = false;
		isVictory = true;
		
		gameBoard.updateSmilie(2);
		gameBoard.updateAllFields(level);
		
		JOptionPane.showMessageDialog(gameBoard, "You have WON this level !!!", "VICTORY !!!",
				JOptionPane.INFORMATION_MESSAGE);
		
		System.exit(0);
	}
}
