package com.karthi.chess.game.Controller;

import java.util.HashMap;
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
import com.karthi.chess.game.model.ChessAI;

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

    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> move(@RequestBody Map<String, Object> moveData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Input validation
            if (!moveData.containsKey("fromRow") || !moveData.containsKey("fromCol") || 
                !moveData.containsKey("toRow") || !moveData.containsKey("toCol") || 
                !moveData.containsKey("color")) {
                response.put("success", false);
                response.put("message", "Missing required fields in request");
                return ResponseEntity.badRequest().body(response);
            }

            int fromRow = (int) moveData.get("fromRow");
            int fromCol = (int) moveData.get("fromCol");
            int toRow = (int) moveData.get("toRow");
            int toCol = (int) moveData.get("toCol");
            
            if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7 ||
                toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
                response.put("success", false);
                response.put("message", "Invalid board coordinates");
                return ResponseEntity.badRequest().body(response);
            }

            // Color validation
            String color = moveData.get("color").toString().toLowerCase();
            if (!color.equals("white") && !color.equals("black")) {
                response.put("success", false);
                response.put("message", "Invalid color specified");
                return ResponseEntity.badRequest().body(response);
            }

            Side playerSide = color.equals("white") ? Side.WHITE : Side.BLACK;

            if (board.getSideToMove() != playerSide) {
                response.put("success", false);
                response.put("message", "It's not " + color + "'s turn!");
                return ResponseEntity.ok().body(response);
            }

            // Square conversion
            String fromNotation = String.format("%s%d", (char)('a' + fromCol), 8 - fromRow).toUpperCase();
            String toNotation = String.format("%s%d", (char)('a' + toCol), 8 - toRow).toUpperCase();
            
            Square from = Square.valueOf(fromNotation);
            Square to = Square.valueOf(toNotation);
            
            Move move = new Move(from, to);

            if (board.isMoveLegal(move, true)) {
                board.doMove(move);
                response.put("success", true);
                response.put("message", "Move successful");
                response.put("move", move.toString());
                return ResponseEntity.ok().body(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid move");
                return ResponseEntity.ok().body(response);
            }
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid square notation: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing move: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/bot-move")
    public ResponseEntity<Map<String, Object>> botMove(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!request.containsKey("color")) {
                response.put("success", false);
                response.put("message", "Missing 'color' in request");
                return ResponseEntity.badRequest().body(response);
            }

            String color = request.get("color").toLowerCase();
            if (!color.equals("white") && !color.equals("black")) {
                response.put("success", false);
                response.put("message", "Invalid color specified");
                return ResponseEntity.badRequest().body(response);
            }

            Side aiSide = color.equals("white") ? Side.WHITE : Side.BLACK;

            if (board.getSideToMove() != aiSide) {
                response.put("success", false);
                response.put("message", "Not " + color + "'s turn!");
                return ResponseEntity.ok().body(response);
            }

            Move bestMove = ChessAI.getGreedyMove(board, aiSide);
            if (bestMove != null) {
                board.doMove(bestMove);
                response.put("success", true);
                response.put("message", "AI move successful");
                response.put("move", bestMove.toString());
                return ResponseEntity.ok().body(response);
            } else {
                response.put("success", false);
                response.put("message", "No valid AI moves available");
                return ResponseEntity.ok().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing AI move: " + e.getMessage());
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
}