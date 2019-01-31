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
	private static int			sizeX;
	private static int			sizeY;
	private static int			numBombs;
	private static int			safeFields;
	private static GameBoard	gameBoard;
	
	static ExecutorService	threadPool;
	static CyclicBarrier	barrier;
	static char[][]			level;
	static boolean			isGameRunning	= true;
	
	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
		threadPool = Executors.newFixedThreadPool(4);
		barrier = new CyclicBarrier(2);
		
		int userChoice = JOptionPane.showOptionDialog(null, "Choose wisely...", "New Game",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] { "New", "Load", "Cancel" }, null);
		
		switch (userChoice) {
			case 0:
				int[] newSettings = GameDialogs.showNewGameDialog();
				
				if (newSettings == null)
					System.exit(0);
				
				sizeX = newSettings[0];
				sizeY = newSettings[1];
				numBombs = newSettings[2];
				
				level = Level.newLevel(sizeX, sizeY, numBombs);
				gameBoard = new GameBoard(sizeX, sizeY);
				break;
			
			case 1:
				Path filePath = GameDialogs.showLoadGameDialog();
				
				if (GameConstants.DEBUG)
					System.out.println(filePath);
				
				if (filePath == null)
					System.exit(0);
				
				try {
					level = SaveGameUtility.readFromFile(filePath);
				} catch (IOException e) {
					if (GameConstants.DEBUG)
						e.printStackTrace();
					System.exit(1);
				} finally {
					if (level == null)
						System.exit(1);
				}
				
				sizeX = level.length;
				sizeY = level[0].length;
				
				for (char[] c1 : level) {
					for (char c2 : c1) {
						if (c2 == '@')
							numBombs++;
					}
				}
				
				System.out.println(numBombs);
				
				gameBoard = new GameBoard(sizeX, sizeY);
				break;
			
			default:
				System.exit(0);
		}
		
		safeFields = (sizeX * sizeY) - numBombs;
		
		threadPool.execute(gameBoard);
		threadPool.execute(() -> calculateFields());
		
		barrier.await();
		
		if (GameConstants.DEBUG)
			DebugView.printLevel(level);
	}
	
	private static synchronized void calculateFields() {
		for (int x = 0; x < level.length; x++) {
			for (int y = 0; y < level[x].length; y++) {
				if (level[x][y] == 'O') {
					level[x][y] = '0';
					
					for (int i = -1, j = 1; i <= 1; i++, j--) {
						try {
							if (level[x + i][y + j] == '@') {
								level[x][y]++;
							}
						} catch (Exception e) {
						}
						try {
							if (level[x + i][y + i] == '@') {
								level[x][y]++;
							}
						} catch (Exception e) {
						}
						try {
							if (level[x][y + i] == '@') {
								level[x][y]++;
							}
						} catch (Exception e) {
						}
						try {
							if (level[x + i][y] == '@') {
								level[x][y]++;
							}
						} catch (Exception e) {
						}
					}
				}
			}
		}
		
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
			switch (level[field.getValueX()][field.getValueY()]) {
				case GameConstants.BOMB:
					level[field.getValueX()][field.getValueY()] = GameConstants.FLAGGED_BOMB;
					if (GameConstants.DEBUG)
						DebugView.printLevel(level);
					
					field.updateField();
					break;
				
				default:
					level[field.getValueX()][field.getValueY()] = GameConstants.FLAGGED;
					safeFields--;
					
					if (GameConstants.DEBUG)
						DebugView.printLevel(level);
					
					field.updateField();
					break;
			}
		});
	}
	
	public static synchronized void reveralField(final GameBoard.Field field) {
		if (GameConstants.DEBUG)
			DebugView.printLevel(level);
		
		if (!field.isActive())
			return;
		
		switch (level[field.getValueX()][field.getValueY()]) {
			case GameConstants.BOMB:
			case GameConstants.FLAGGED_BOMB:
				field.updateField();
				gameOver();
				break;
			
			case GameConstants.EMPTY:
				// process the neighbour fields
				for (Component c : gameBoard.getGameFields()) {
					if (c instanceof GameBoard.Field) {
						GameBoard.Field other = (GameBoard.Field) c;
						
						for (int i = -1; i <= +1; i++) {
							if (other.getValueX() == field.getValueX() & other.getValueY() == field.getValueY()) {
								continue;
							}
							
							if ((other.getValueX() == field.getValueX() + i && other.getValueY() == field.getValueY())
									| (other.getValueY() == field.getValueY() + i
											&& other.getValueX() == field.getValueX())) {
								if (level[other.getValueX()][other.getValueY()] == GameConstants.BOMB
										| level[other.getValueX()][other.getValueY()] == GameConstants.FLAGGED_BOMB) {
									break;
								}
								
								threadPool.execute(() -> reveralField(other));
							}
							
						}
					}
				}
				
				field.updateField();
				break;
			
			default:
				field.updateField();
				break;
		}
		
		safeFields--;
		
		if (GameConstants.DEBUG)
			System.out
					.println("Game Board: " + (sizeX * sizeY) + " (Safe : " + safeFields + " Bombs: " + numBombs + ")");
		
		if (safeFields == 0)
			gameVictory();
	}
	
	private static void gameOver() {
		isGameRunning = false;
		gameBoard.updateSmilie(3);
		gameBoard.updateAllFields();
		
		JOptionPane.showMessageDialog(gameBoard, "Dude, you had ONE job...", "GAME OVER", JOptionPane.ERROR_MESSAGE);
		
		System.exit(0);
	}
	
	private static void gameVictory() {
		isGameRunning = false;
		gameBoard.updateSmilie(2);
		gameBoard.updateAllFields();
		
		JOptionPane.showMessageDialog(gameBoard, "You have WON this level !!!", "VICTORY !!!",
				JOptionPane.INFORMATION_MESSAGE);
		
		System.exit(0);
	}
}
