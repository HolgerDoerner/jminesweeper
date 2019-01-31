package game;

final class DebugView {
	private DebugView() {
	}
	
	public static final void printLevel(char[][] level) {
		if (level == null)
			throw new IllegalArgumentException("Argument can't be NULL!");
		
		for (int i = 0; i < level.length; i++) {
			for (int j = 0; j < level[i].length; j++) {
				System.out.print(level[i][j] + " ");
			}
			
			System.out.println();
		}
		
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
	}
}
