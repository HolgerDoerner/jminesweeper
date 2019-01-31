package game;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SaveGameUtility {
	public static char[][] readFromFile(Path filePath) throws IOException {
		if (filePath == null)
			return null;
		
		return Files.lines(filePath).map(s -> s.toCharArray()).toArray(char[][]::new);
	}
	
	public static void saveToFile(Path filePath, char[][] data) throws IOException {
		if (filePath == null || data == null)
			throw new IllegalStateException("Parameters can not be null!");
		
		try (BufferedWriter out = Files.newBufferedWriter(filePath)) {
			for (char[] c : data) {
				out.write(c);
				out.newLine();
			}
		}
	}
}
