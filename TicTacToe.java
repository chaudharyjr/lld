import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Arrays; // For Arrays.fill

/**
 * Enum to represent the possible states of a cell on the board.
 * Provides type safety and clarity over using raw characters.
 */
enum Symbol {
    X, O, EMPTY;

    @Override
    public String toString() {
        return (this == EMPTY) ? "-" : String.valueOf(this); // Print '-' for empty, 'X' or 'O' otherwise
    }
}

/**
 * Represents a position on the game board with row and column coordinates.
 * Encapsulates coordinates for cleaner method signatures.
 */
class Position {
    int row;
    int col;

    /**
     * Constructor for Position.
     * @param row The row coordinate.
     * @param col The column coordinate.
     */
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }
}

/**
 * Represents a single player in the game.
 */
class Player {
    private String name;
    private Symbol symbol;

    /**
     * Constructor to initialize a player.
     * @param name The name of the player.
     * @param symbol The symbol assigned to the player (e.g., Symbol.X, Symbol.O).
     */
    public Player(String name, Symbol symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    /**
     * Returns the player's name.
     * @return The player's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the player's symbol.
     * @return The player's symbol.
     */
    public Symbol getSymbol() {
        return symbol;
    }
}

/**
 * Manages the game grid and its state.
 */
class Board {
    private Symbol[][] grid;
    private int rows;
    private int cols;

    /**
     * Constructor to initialize the board with specified dimensions.
     * @param rows The number of rows in the board.
     * @param cols The number of columns in the board.
     */
    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Symbol[rows][cols];
        initializeBoard(); // Initialize all cells to empty
    }

    /**
     * Fills all cells of the grid with Symbol.EMPTY.
     */
    public void initializeBoard() {
        for (int i = 0; i < rows; i++) {
            Arrays.fill(grid[i], Symbol.EMPTY); // Use Arrays.fill for conciseness
        }
    }

    /**
     * Attempts to place a symbol at the given Position.
     * @param pos The Position where the symbol should be placed.
     * @param symbol The symbol to place.
     * @return true if the move is valid and placed successfully, false otherwise.
     */
    public boolean placeMove(Position pos, Symbol symbol) {
        // Check if coordinates are within bounds
        if (pos.row < 0 || pos.row >= rows || pos.col < 0 || pos.col >= cols) {
            System.out.println("Error: Move out of board bounds.");
            return false;
        }
        // Check if the cell is empty
        if (grid[pos.row][pos.col] != Symbol.EMPTY) {
            System.out.println("Error: Cell is already occupied.");
            return false;
        }
        // Place the symbol
        grid[pos.row][pos.col] = symbol;
        return true;
    }

    /**
     * Checks if the cell at a given Position is empty.
     * @param pos The Position to check.
     * @return true if empty and coordinates are valid, false otherwise.
     */
    public boolean isCellEmpty(Position pos) {
        return pos.row >= 0 && pos.row < rows && pos.col >= 0 && pos.col < cols && grid[pos.row][pos.col] == Symbol.EMPTY;
    }

    /**
     * Checks if all cells on the board are occupied.
     * @return true if full, false otherwise.
     */
    public boolean isBoardFull() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == Symbol.EMPTY) {
                    return false; // Found an empty cell, board is not full
                }
            }
        }
        return true; // No empty cells found, board is full
    }

    /**
     * Prints the current state of the board to the console in a user-friendly format.
     */
    public void printBoard() {
        System.out.println("\n--- Current Board ---");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(" " + grid[i][j]);
                if (j < cols - 1) {
                    System.out.print(" |");
                }
            }
            System.out.println();
            if (i < rows - 1) {
                for (int k = 0; k < cols; k++) {
                    System.out.print("----");
                }
                System.out.println("-");
            }
        }
        System.out.println("---------------------\n");
    }

    /**
     * Returns the symbol at the specified Position.
     * Returns Symbol.EMPTY if out of bounds.
     * @param pos The Position to get the symbol from.
     * @return The symbol at the cell, or Symbol.EMPTY if out of bounds.
     */
    public Symbol getSymbol(Position pos) {
        if (pos.row < 0 || pos.row >= rows || pos.col < 0 || pos.col >= cols) {
            return Symbol.EMPTY; // Out of bounds
        }
        return grid[pos.row][pos.col];
    }

    /**
     * Returns the number of rows in the board.
     * @return The number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the number of columns in the board.
     * @return The number of columns.
     */
    public int getCols() {
        return cols;
    }
}

/**
 * Orchestrates the game flow, manages turns, checks win/draw conditions, and maintains the overall game state.
 */
class Game {
    private Board board;
    private List<Player> players;
    private int currentPlayerIndex;
    private int winConditionLength;
    private boolean gameOver;
    private Player winner;

    /**
     * Constructor to set up the game with board dimensions, win condition length, and a list of players.
     * @param rows The number of rows for the board.
     * @param cols The number of columns for the board.
     * @param winConditionLength The number of symbols in a row required to win.
     * @param players A list of Player objects participating in the game.
     */
    public Game(int rows, int cols, int winConditionLength, List<Player> players) {
        this.board = new Board(rows, cols);
        this.players = players;
        this.winConditionLength = winConditionLength;
        this.gameOver = false;
        this.winner = null;
        this.currentPlayerIndex = 0; // Start with the first player
    }

    /**
     * Initializes the game by setting up the board, resetting game state flags, and setting the first player.
     */
    public void startGame() {
        board.initializeBoard();
        gameOver = false;
        winner = null;
        currentPlayerIndex = 0;
        System.out.println("Game started! " + players.get(currentPlayerIndex).getName() + " goes first.");
    }

    /**
     * Attempts to make a move for the given player at the specified Position.
     * @param player The player attempting the move.
     * @param pos The Position for the move.
     * @return true if the move was successful and the game state was updated, false otherwise.
     */
    public boolean makeMove(Player player, Position pos) {
        // Check if it's the current player's turn
        if (player != players.get(currentPlayerIndex)) {
            System.out.println("Error: It's not " + player.getName() + "'s turn.");
            return false;
        }

        // Attempt to place the move on the board
        if (board.placeMove(pos, player.getSymbol())) {
            // After a successful move, check for win or draw
            if (checkWin(pos, player.getSymbol())) {
                gameOver = true;
                winner = player;
                System.out.println(player.getName() + " wins!");
            } else if (checkDraw()) {
                gameOver = true;
                System.out.println("It's a draw!");
            } else {
                // If no win or draw, switch to the next player
                switchPlayer();
            }
            return true;
        }
        return false; // Move was invalid (out of bounds or cell occupied)
    }

    /**
     * Checks if the symbol placed at (lastMovePos) results in a win.
     * This method will check horizontal, vertical, and both diagonal lines.
     * @param lastMovePos The Position of the last move.
     * @param symbol The symbol of the player who just moved.
     * @return true if a win condition is met, false otherwise.
     */
    private boolean checkWin(Position lastMovePos, Symbol symbol) {
        // Check all 8 directions (horizontal, vertical, diagonals)
        int[][] directions = {
            {0, 1}, {0, -1}, // Horizontal (right, left)
            {1, 0}, {-1, 0}, // Vertical (down, up)
            {1, 1}, {-1, -1},// Diagonal (down-right, up-left)
            {1, -1}, {-1, 1} // Anti-diagonal (down-left, up-right)
        };

        // For each direction, check if a winning line is formed
        for (int i = 0; i < directions.length; i += 2) { // Check pairs of opposite directions
            int count = 1; // Start with 1 for the last placed symbol
            count += countConsecutiveSymbols(lastMovePos, directions[i][0], directions[i][1], symbol); // One direction
            count += countConsecutiveSymbols(lastMovePos, directions[i+1][0], directions[i+1][1], symbol); // Opposite direction

            if (count >= winConditionLength) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to count consecutive symbols in a given direction from a starting position.
     * @param startPos The starting position for the count.
     * @param dRow The row increment for the direction (-1, 0, or 1).
     * @param dCol The column increment for the direction (-1, 0, or 1).
     * @param symbol The symbol to count.
     * @return The number of consecutive symbols found in that direction (excluding the startPos itself).
     */
    private int countConsecutiveSymbols(Position startPos, int dRow, int dCol, Symbol symbol) {
        int count = 0;
        for (int i = 1; i < winConditionLength; i++) { // Start from 1 to not re-count the startPos
            int r = startPos.row + i * dRow;
            int c = startPos.col + i * dCol;
            Position currentPos = new Position(r, c);

            if (r >= 0 && r < board.getRows() && c >= 0 && c < board.getCols() && board.getSymbol(currentPos) == symbol) {
                count++;
            } else {
                break; // Stop if symbol doesn't match or out of bounds
            }
        }
        return count;
    }


    /**
     * Checks if the game is a draw (board is full and no winner).
     * @return true if draw, false otherwise.
     */
    private boolean checkDraw() {
        return board.isBoardFull() && winner == null;
    }

    /**
     * Advances currentPlayerIndex to the next player in the players list.
     */
    private void switchPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        System.out.println(players.get(currentPlayerIndex).getName() + "'s turn.");
    }

    /**
     * Returns true if the game has ended (win or draw), false otherwise.
     * @return true if game is over, false otherwise.
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Returns the Player object whose turn it currently is.
     * @return The current Player.
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Returns the Player object who won, or null if no winner yet or it's a draw.
     * @return The winning Player, or null.
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Delegates to board.printBoard() to display the current board state.
     */
    public void printCurrentBoard() {
        board.printBoard();
    }
}

/**
 * Main class to run the game. Demonstrates the game flow.
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Setup players
        List<Player> players = new ArrayList<>();
        players.add(new Player("Player 1", Symbol.X));
        players.add(new Player("Player 2", Symbol.O));

        // Setup game (3x3 board, win with 3 in a row)
        // This can be easily changed, e.g., new Game(5, 5, 4, players) for 4-in-a-row on a 5x5 board
        Game game = new Game(3, 3, 3, players);
        game.startGame();

        while (!game.isGameOver()) {
            game.printCurrentBoard();
            Player currentPlayer = game.getCurrentPlayer();
            System.out.println(currentPlayer.getName() + ", enter your move (row and column, e.g., 0 0):");

            int row = -1;
            int col = -1;
            boolean validInput = false;

            while (!validInput) {
                try {
                    row = scanner.nextInt();
                    col = scanner.nextInt();
                    validInput = true;
                } catch (java.util.InputMismatchException e) {
                    System.out.println("Invalid input. Please enter two integers for row and column.");
                    scanner.next(); // Consume the invalid input
                }
            }

            Position movePos = new Position(row, col);
            // Attempt to make the move
            boolean moveMade = game.makeMove(currentPlayer, movePos);

            // If move was not made, it means it was invalid, so prompt again
            if (!moveMade) {
                System.out.println("Invalid move. Please try again.");
            }
        }

        // Game over, print final board and result
        game.printCurrentBoard();
        if (game.getWinner() != null) {
            System.out.println("Game Over! " + game.getWinner().getName() + " wins!");
        } else {
            System.out.println("Game Over! It's a draw!");
        }

        scanner.close();
    }
}
