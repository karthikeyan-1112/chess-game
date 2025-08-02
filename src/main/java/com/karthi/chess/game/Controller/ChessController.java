package com.karthi.chess.game.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.karthi.chess.game.model.ChessAI;

@RestController
public class ChessController {

    private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private final Board board = new Board();
    private final List<String> moveHistory = new ArrayList<>();
    private final Stack<String> historyFenStack = new Stack<>();

    public ChessController() {
        board.loadFromFen(STARTING_FEN);
        historyFenStack.push(STARTING_FEN);
    }
    
    @GetMapping("/board")
    @SuppressWarnings("unchecked")
    public Map<String, Object>[][] getBoard() {
        Map<String, Object>[][] boardState = (Map<String, Object>[][]) new HashMap[8][8];
        for (Square sq : Square.values()) {
            if (sq.ordinal() >= 64) continue;
            int row = 7 - sq.getRank().ordinal();
            int col = sq.getFile().ordinal();
            Piece piece = board.getPiece(sq);
            if (!piece.equals(Piece.NONE)) {
                boardState[row][col] = new HashMap<>();
                boardState[row][col].put("type", piece.getPieceType().toString());
                boardState[row][col].put("color", piece.getPieceSide().toString().toLowerCase());
                if (piece.getPieceType() == PieceType.KING) {
                    boardState[row][col].put("inCheck", board.isKingAttacked());
                }
            } else {
                boardState[row][col] = null;
            }
        }
        return boardState;
    }

    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> move(@RequestBody Map<String, Object> moveData) {
        Map<String, Object> response = new HashMap<>();
        try {
            int fromRow = intFromObj(moveData.get("fromRow"));
            int fromCol = intFromObj(moveData.get("fromCol"));
            int toRow = intFromObj(moveData.get("toRow"));
            int toCol = intFromObj(moveData.get("toCol"));
            String color = moveData.get("color").toString().toLowerCase();
            Side playerSide = "white".equals(color) ? Side.WHITE : Side.BLACK;
            
            if (board.getSideToMove() != playerSide) {
                response.put("success", false);
                response.put("message", "It's not your turn.");
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                return ResponseEntity.ok(response);
            }
            
            Square from = Square.valueOf((char)('A' + fromCol) + "" + (8 - fromRow));
            Square to = Square.valueOf((char)('A' + toCol) + "" + (8 - toRow));
            Move move;
            // Handle promotion if present
            if (moveData.containsKey("promotion")) {
                String promotionStr = moveData.get("promotion").toString().toUpperCase();
                if (!Set.of("QUEEN", "ROOK", "BISHOP", "KNIGHT").contains(promotionStr)) {
                    response.put("success", false);
                    response.put("message", "Invalid promotion piece: " + promotionStr);
                    response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                    return ResponseEntity.ok(response);
                }
                Piece promotedPiece = Piece.make(playerSide, PieceType.valueOf(promotionStr));
                move = new Move(from, to, promotedPiece);
            } else {
                move = new Move(from, to);
            }
            
            if (board.isMoveLegal(move, true)) {
                board.doMove(move);
                moveHistory.add(move.toString());
                historyFenStack.push(board.getFen());
                response.put("success", true);
                response.put("move", move.toString());
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                response.put("gameStatus", getCurrentGameStatus());
                response.put("moveHistory", new ArrayList<>(moveHistory));
            } else {
                response.put("success", false);
                response.put("message", "Invalid move");
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/bot-move")
    public ResponseEntity<Map<String, Object>> botMove(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String color = request.get("color").toLowerCase();
            Side aiSide = "white".equals(color) ? Side.WHITE : Side.BLACK;

            if (board.getSideToMove() != aiSide) {
                response.put("success", false);
                response.put("message", "Not " + color + "'s turn!");
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                return ResponseEntity.ok(response);
            }

            Move bestMove = ChessAI.getGreedyMove(board, aiSide);
            if (bestMove != null && board.isMoveLegal(bestMove, true)) {
                board.doMove(bestMove);
                moveHistory.add(bestMove.toString());
                historyFenStack.push(board.getFen());
                response.put("success", true);
                response.put("move", bestMove.toString());
                response.put("gameStatus", getCurrentGameStatus());
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                response.put("moveHistory", new ArrayList<>(moveHistory));
            } else {
                response.put("success", false);
                response.put("message", "No legal moves available.");
                response.put("gameStatus", getCurrentGameStatus());
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/valid-moves")
    public ResponseEntity<Map<String, Object>> getValidMoves(@RequestBody Map<String, Integer> input) {
        Map<String, Object> response = new HashMap<>();
        try {
            int fromRow = intFromObj(input.get("row"));
            int fromCol = intFromObj(input.get("col"));
            Square fromSquare = Square.valueOf((char)('A' + fromCol) + "" + (8 - fromRow));
            List<Move> legalMoves = board.legalMoves();
            List<Map<String, Integer>> validMoves = new ArrayList<>();
            for (Move move : legalMoves) {
                if (move.getFrom().equals(fromSquare)) {
                    Square to = move.getTo();
                    int toRow = 7 - to.getRank().ordinal();
                    int toCol = to.getFile().ordinal();
                    Map<String, Integer> moveMap = new HashMap<>();
                    moveMap.put("row", toRow);
                    moveMap.put("col", toCol);
                    validMoves.add(moveMap);
                }
            }
            response.put("success", true);
            response.put("validMoves", validMoves);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetGame() {
        board.loadFromFen(STARTING_FEN);
        moveHistory.clear();
        historyFenStack.clear();
        historyFenStack.push(STARTING_FEN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Game reset.");
        response.put("currentTurn", "white");
        response.put("gameStatus", "normal");
        response.put("moveHistory", new ArrayList<>(moveHistory));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, Object>> undoMove() {
        Map<String, Object> response = new HashMap<>();
        if (historyFenStack.size() > 1) {
            historyFenStack.pop();
            String fen = historyFenStack.peek();
            board.loadFromFen(fen);
            if (!moveHistory.isEmpty())
                moveHistory.remove(moveHistory.size() - 1);
            
            response.put("success", true);
            response.put("message", "Move undone.");
            response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
            response.put("gameStatus", getCurrentGameStatus());
            response.put("moveHistory", new ArrayList<>(moveHistory));
        } else {
            response.put("success", false);
            response.put("message", "No moves to undo.");
            response.put("moveHistory", new ArrayList<>(moveHistory));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/move-history")
    public List<String> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    // Utility for returning current game status string
    private String getCurrentGameStatus() {
        if (board.isMated()) {
            return board.isKingAttacked()
                ? "checkmate-" + (board.getSideToMove() == Side.WHITE ? "black" : "white")
                : "stalemate";
        }
        if (board.isStaleMate())
            return "stalemate";
        if (board.isKingAttacked())
            return "check-" + (board.getSideToMove() == Side.WHITE ? "white" : "black");
        if (board.isDraw())
            return "draw";
        return "normal";
    }

    // Convert various numeric types to int
    private int intFromObj(Object o) {
        if (o instanceof Integer i) return i;
        if (o instanceof Double d) return d.intValue();
        return Integer.parseInt(o.toString());
    }
}
