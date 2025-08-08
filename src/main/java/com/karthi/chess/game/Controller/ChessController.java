package com.karthi.chess.game.Controller;

import com.karthi.chess.game.engine.Stockfish;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class ChessController {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private String currentFen = STARTING_FEN;
    private final List<String> moveHistory = new ArrayList<>();
    private final Stack<String> fenStack = new Stack<>();

    private static final Map<Character, String> pieceNameMap = new HashMap<>();
    static {
        pieceNameMap.put('p', "PAWN");
        pieceNameMap.put('r', "ROOK");
        pieceNameMap.put('n', "KNIGHT");
        pieceNameMap.put('b', "BISHOP");
        pieceNameMap.put('q', "QUEEN");
        pieceNameMap.put('k', "KING");
    }

    public ChessController() {
        fenStack.push(currentFen);
    }

    @GetMapping("/board")
    public Map<String, Object> getBoard() {
        Map<String, Object> out = new HashMap<>();
        out.put("board", fenToBoardArray(currentFen));
        out.put("moveHistory", new ArrayList<>(moveHistory));
        out.put("currentTurn", currentFen.split(" ")[1].equals("w") ? "white" : "black");
        out.put("fen", currentFen);
        return out;
    }

    private Object[][] fenToBoardArray(String fen) {
        String[] rows = fen.split(" ")[0].split("/");
        Object[][] board = new Object[8][8];
        for (int r = 0; r < 8; r++) {
            int file = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    file += Character.getNumericValue(ch);
                } else {
                    String color = Character.isUpperCase(ch) ? "white" : "black";
                    String type = pieceNameMap.get(Character.toLowerCase(ch));
                    Map<String, String> piece = new HashMap<>();
                    piece.put("type", type);
                    piece.put("color", color);
                    board[r][file] = piece;
                    file++;
                }
            }
        }
        return board;
    }

    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> playerMove(@RequestBody Map<String, Object> moveData) {
        Map<String, Object> response = new HashMap<>();
        try {
            int fromRow = ((Number) moveData.get("fromRow")).intValue();
            int fromCol = ((Number) moveData.get("fromCol")).intValue();
            int toRow = ((Number) moveData.get("toRow")).intValue();
            int toCol = ((Number) moveData.get("toCol")).intValue();
            String promotion = moveData.containsKey("promotion") ? (String) moveData.get("promotion") : null;

            String move = indexToAlgebraic(fromRow, fromCol) + indexToAlgebraic(toRow, toCol);
            if (promotion != null) {
                move += promotion.substring(0, 1).toLowerCase();
            }

            Stockfish sf = new Stockfish();
            if (!sf.startEngine()) {
                response.put("success", false);
                response.put("message", "Failed to start Stockfish");
                return ResponseEntity.ok(response);
            }

            List<String> legalMoves = getLegalMoves(currentFen, sf);
            if (!legalMoves.contains(move)) {
                sf.stopEngine();
                response.put("success", false);
                response.put("message", "Illegal move: " + move);
                return ResponseEntity.ok(response);
            }

            currentFen = getFenAfterMove(currentFen, move, sf);
            fenStack.push(currentFen);
            moveHistory.add(move);

            sf.stopEngine();

            response.put("success", true);
            response.put("board", fenToBoardArray(currentFen));
            response.put("currentTurn", currentFen.split(" ")[1].equals("w") ? "white" : "black");
            response.put("moveHistory", new ArrayList<>(moveHistory));
            response.put("gameStatus", "normal");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bot-move")
    public ResponseEntity<Map<String, Object>> botMove(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String color = request.get("color");
            String fenTurn = currentFen.split(" ")[1].equals("w") ? "white" : "black";
            if (!color.equalsIgnoreCase(fenTurn)) {
                response.put("success", false);
                response.put("message", "Not " + color + "'s turn");
                return ResponseEntity.ok(response);
            }

            Stockfish sf = new Stockfish();
            if (!sf.startEngine()) {
                response.put("success", false);
                response.put("message", "Failed to start Stockfish");
                return ResponseEntity.ok(response);
            }

            String bestMove = sf.getBestMove(currentFen, 500);
            if (bestMove == null) {
                sf.stopEngine();
                response.put("success", false);
                response.put("message", "No move found");
                return ResponseEntity.ok(response);
            }

            currentFen = getFenAfterMove(currentFen, bestMove, sf);
            fenStack.push(currentFen);
            moveHistory.add(bestMove);

            sf.stopEngine();

            response.put("success", true);
            response.put("board", fenToBoardArray(currentFen));
            response.put("currentTurn", currentFen.split(" ")[1].equals("w") ? "white" : "black");
            response.put("moveHistory", new ArrayList<>(moveHistory));
            response.put("gameStatus", "normal");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/valid-moves")
    public Map<String, Object> getValidMoves(@RequestBody Map<String, Integer> input) {
        Map<String, Object> response = new HashMap<>();
        try {
            int row = input.get("row");
            int col = input.get("col");

            String sourceSquare = indexToAlgebraic(row, col);
            List<Map<String, Integer>> validMovesList = new ArrayList<>();

            Stockfish sf = new Stockfish();
            if (!sf.startEngine()) {
                response.put("success", false);
                response.put("message", "Failed to start Stockfish");
                return response;
            }

            List<String> allMoves = getLegalMoves(currentFen, sf);
            for (String move : allMoves) {
                if (move.startsWith(sourceSquare)) {
                    int destCol = move.charAt(2) - 'a';
                    int destRow = 8 - Character.getNumericValue(move.charAt(3));
                    Map<String, Integer> mv = new HashMap<>();
                    mv.put("row", destRow);
                    mv.put("col", destCol);
                    validMovesList.add(mv);
                }
            }
            sf.stopEngine();

            response.put("success", true);
            response.put("validMoves", validMovesList);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/reset")
    public Map<String, Object> resetGame() {
        currentFen = STARTING_FEN;
        fenStack.clear();
        fenStack.push(currentFen);
        moveHistory.clear();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("board", fenToBoardArray(currentFen));
        response.put("currentTurn", "white");
        response.put("moveHistory", new ArrayList<>());
        response.put("gameStatus", "normal");
        return response;
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, Object>> undoMove() {
        Map<String, Object> response = new HashMap<>();
        if (fenStack.size() > 1) {
            fenStack.pop();
            currentFen = fenStack.peek();
            if (!moveHistory.isEmpty())
                moveHistory.remove(moveHistory.size() - 1);
            response.put("success", true);
            response.put("board", fenToBoardArray(currentFen));
            response.put("currentTurn", currentFen.split(" ")[1].equals("w") ? "white" : "black");
            response.put("moveHistory", new ArrayList<>(moveHistory));
            response.put("gameStatus", "normal");
        } else {
            response.put("success", false);
            response.put("message", "No move to undo");
        }
        return ResponseEntity.ok(response);
    }

    // ===== Helper methods =====

    private String indexToAlgebraic(int row, int col) {
        char file = (char) ('a' + col);
        int rank = 8 - row;
        return "" + file + rank;
    }

    private List<String> getLegalMoves(String fen, Stockfish sf) throws Exception {
        sf.sendCommand("position fen " + fen);
        sf.sendCommand("d");
        String output = sf.getOutput(100);
        for (String line : output.split("\\n")) {
            if (line.startsWith("Legal moves: ")) {
                String legalMovesStr = line.substring("Legal moves: ".length());
                return Arrays.asList(legalMovesStr.trim().split(" "));
            }
        }
        return Collections.emptyList();
    }

    private String getFenAfterMove(String fen, String move, Stockfish sf) throws Exception {
        sf.sendCommand("position fen " + fen + " moves " + move);
        sf.sendCommand("d");
        String output = sf.getOutput(100);
        for (String line : output.split("\\n")) {
            if (line.startsWith("Fen: ")) {
                return line.substring("Fen: ".length());
            }
        }
        return fen;
    }
}
