package com.karthi.chess.game.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

@RestController
public class ChessController implements InitializingBean {

    private final Board board = new Board();

    @Override
    public void afterPropertiesSet() throws Exception {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    @GetMapping("/board")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>[][]> getBoard() {
        Map<String, Object>[][] boardState = (Map<String, Object>[][]) new HashMap[8][8];
        Map<String, Object> response = new HashMap<>();

        try {
            for (Square square : Square.values()) {
                if (square.ordinal() >= 64) continue;

                int row = 7 - square.getRank().ordinal();
                int col = square.getFile().ordinal();

                var piece = board.getPiece(square);
                if (piece != com.github.bhlangonijr.chesslib.Piece.NONE) {
                    boardState[row][col] = new HashMap<>();
                    boardState[row][col].put("type", piece.getPieceType().toString());
                    boardState[row][col].put("color", piece.getPieceSide().toString().toLowerCase());
                } else {
                    boardState[row][col] = null;
                }
            }

            response.put("success", true);
            response.put("board", boardState);
            return ResponseEntity.ok(boardState);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error getting board state: " + e.getMessage());
            return ResponseEntity.internalServerError().body(boardState);
        }
    }
    @GetMapping("/test-chesslib")

public String testChesslib() {
    try {
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        return "Chesslib is working. Side to move: " + board.getSideToMove();
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}


   @PostMapping("/move")
public ResponseEntity<Map<String, Object>> move(@RequestBody Map<String, Object> moveData) {
    Map<String, Object> response = new HashMap<>();

    try {
        // validate inputs...
        int fromRow = (int) moveData.get("fromRow");
        int fromCol = (int) moveData.get("fromCol");
        int toRow = (int) moveData.get("toRow");
        int toCol = (int) moveData.get("toCol");
        String color = moveData.get("color").toString().toLowerCase();
        Side playerSide = color.equals("white") ? Side.WHITE : Side.BLACK;

        if (board.getSideToMove() != playerSide) {
            response.put("success", false);
            response.put("message", "It's not " + color + "'s turn!");
            return ResponseEntity.ok(response);
        }

        Square from = Square.valueOf(String.format("%s%d", (char)('a' + fromCol), 8 - fromRow).toUpperCase());
        Square to = Square.valueOf(String.format("%s%d", (char)('a' + toCol), 8 - toRow).toUpperCase());
        Move move = new Move(from, to);

        if (board.isMoveLegal(move, true)) {
            board.doMove(move);
            response.put("success", true);
            response.put("message", "Move successful");
            response.put("move", move.toString());

            if (board.isMated()) {
                response.put("checkmate", true);
                response.put("winner", color);
            }

            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid move");
            return ResponseEntity.ok(response);
        }

    } catch (Exception e) {
        response.put("success", false);
        response.put("message", "Error processing move: " + e.getMessage());
        return ResponseEntity.internalServerError().body(response);
    }
}


    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetGame() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            response.put("success", true);
            response.put("message", "Game reset to starting position");
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error resetting game: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @PostMapping("/valid-moves")
    public ResponseEntity<Map<String, Object>> getValidMoves(@RequestBody Map<String, Integer> input) {
        Map<String, Object> response = new HashMap<>();
        try {
            int fromRow = input.get("row");
            int fromCol = input.get("col");
    
            String fromNotation = String.format("%s%d", (char)('a' + fromCol), 8 - fromRow).toUpperCase();
            Square fromSquare = Square.valueOf(fromNotation);
    
            List<Move> legalMoves = board.legalMoves();
            List<Map<String, Integer>> validMoves = new ArrayList<>();
    
            for (Move move : legalMoves) {
                if (move.getFrom() == fromSquare) {
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
    
    
}