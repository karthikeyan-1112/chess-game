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
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.karthi.chess.game.model.StockfishClient;

@RestController
public class ChessController implements InitializingBean {

    private final Board board = new Board();
    private final List<String> moveHistory = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        resetBoard();
    }

    private void resetBoard() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        moveHistory.clear();
    }

    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>[][]> getBoard() {
        @SuppressWarnings("unchecked")
        Map<String, Object>[][] boardState = (Map<String, Object>[][]) new HashMap[8][8];
        for (Square square : Square.values()) {
            if (square.ordinal() >= 64) continue;

            int row = 7 - square.getRank().ordinal();
            int col = square.getFile().ordinal();
            Piece piece = board.getPiece(square);

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
        return ResponseEntity.ok(boardState);
    }

    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> move(@RequestBody Map<String, Object> moveData) {
        Map<String, Object> response = new HashMap<>();
        try {
            int fromRow = (int) moveData.get("fromRow");
            int fromCol = (int) moveData.get("fromCol");
            int toRow = (int) moveData.get("toRow");
            int toCol = (int) moveData.get("toCol");
            String color = moveData.get("color").toString().toLowerCase();
            Side playerSide = color.equals("white") ? Side.WHITE : Side.BLACK;

            if (board.getSideToMove() != playerSide) {
                response.put("success", false);
                response.put("message", "It's not " + color + "'s turn!");
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                response.put("moveHistory", new ArrayList<>(moveHistory));
                response.put("gameStatus", getCurrentGameStatus());
                return ResponseEntity.ok(response);
            }

            Square from = Square.valueOf(
                String.format("%c%d", (char) ('a' + fromCol), 8 - fromRow).toUpperCase()
            );
            Square to = Square.valueOf(
                String.format("%c%d", (char) ('a' + toCol), 8 - toRow).toUpperCase()
            );

            Move move;
            // Handle Promotion
            if (moveData.containsKey("promotion")) {
                String promoStr = moveData.get("promotion").toString().toUpperCase();
                String fullPieceName = (playerSide == Side.WHITE ? "WHITE_" : "BLACK_") + promoStr;
                Piece promotedPiece;

                try {
                    promotedPiece = Piece.valueOf(fullPieceName);
                } catch (IllegalArgumentException e) {
                    response.put("success", false);
                    response.put("message", "Invalid promotion piece.");
                    response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                    response.put("moveHistory", new ArrayList<>(moveHistory));
                    response.put("gameStatus", getCurrentGameStatus());
                    return ResponseEntity.ok(response);
                }
                move = new Move(from, to, promotedPiece);
            } else {
                move = new Move(from, to);
            }

            // Validate and Make Move
            if (board.isMoveLegal(move, true)) {
                board.doMove(move);
                moveHistory.add(move.toString());
                response.put("success", true);
                response.put("move", move.toString());
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                response.put("moveHistory", new ArrayList<>(moveHistory));
                response.put("gameStatus", getCurrentGameStatus());

                if (board.isMated()) {
                    response.put("checkmate", true);
                    response.put("winner", color);
                }
            } else {
                response.put("success", false);
                response.put("message", "Invalid move");
                response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                response.put("moveHistory", new ArrayList<>(moveHistory));
                response.put("gameStatus", getCurrentGameStatus());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing move: " + e.getMessage());
            response.put("currentTurn", board.getSideToMove().toString().toLowerCase());
            response.put("moveHistory", new ArrayList<>(moveHistory));
            response.put("gameStatus", getCurrentGameStatus());
            return ResponseEntity.internalServerError().body(response);
        }
    }

   @PostMapping("/bot-move")
public ResponseEntity<Map<String, Object>> botMove(@RequestBody Map<String, String> request) {
    Map<String, Object> res = new HashMap<>();
    try {
        String fen = board.getFen(); // Current position
        StockfishClient sf = new StockfishClient("stockfish"); // or full path if needed
        sf.start();
        String bestMoveStr = sf.getBestMove(fen);
        sf.stop();

        if (bestMoveStr != null && bestMoveStr.length() >= 4) {
            Square from = Square.valueOf(bestMoveStr.substring(0, 2).toUpperCase());
            Square to = Square.valueOf(bestMoveStr.substring(2, 4).toUpperCase());
            Move move;

            // Promotion handling
            if (bestMoveStr.length() == 5) {
                char promoChar = bestMoveStr.charAt(4);
                PieceType promoType = switch (promoChar) {
                    case 'q' -> PieceType.QUEEN;
                    case 'r' -> PieceType.ROOK;
                    case 'b' -> PieceType.BISHOP;
                    case 'n' -> PieceType.KNIGHT;
                    default -> null;
                };
                if (promoType != null) {
                    Piece fromPiece = board.getPiece(from);
                    move = new Move(from, to, Piece.make(fromPiece.getPieceSide(), promoType));
                } else {
                    move = new Move(from, to);
                }
            } else {
                move = new Move(from, to);
            }

            if (board.isMoveLegal(move, true)) {
                board.doMove(move);
                moveHistory.add(move.toString());
                res.put("success", true);
                res.put("move", move.toString());
                res.put("moveHistory", new ArrayList<>(moveHistory));
                res.put("currentTurn", board.getSideToMove().toString().toLowerCase());
                res.put("gameStatus", getCurrentGameStatus());
            } else {
                res.put("success", false);
                res.put("message", "Illegal move returned by Stockfish");
            }
        } else {
            res.put("success", false);
            res.put("message", "No valid move found");
        }
    } catch (Exception e) {
        res.put("success", false);
        res.put("message", "Error: " + e.getMessage());
    }
    return ResponseEntity.ok(res);
}


    @PostMapping("/valid-moves")
    public ResponseEntity<Map<String, Object>> getValidMoves(@RequestBody Map<String, Integer> input) {
        Map<String, Object> response = new HashMap<>();
        try {
            int fromRow = input.get("row");
            int fromCol = input.get("col");
            String fromNotation = String.format("%c%d", (char) ('a' + fromCol), 8 - fromRow).toUpperCase();
            Square fromSquare = Square.valueOf(fromNotation);

            List<Move> legalMoves = board.legalMoves();
            List<Map<String, Integer>> validMoves = new ArrayList<>();

            for (Move move : legalMoves) {
                if (move.getFrom().equals(fromSquare)) {
                    Square to = move.getTo();
                    Map<String, Integer> moveMap = new HashMap<>();
                    moveMap.put("row", 7 - to.getRank().ordinal());
                    moveMap.put("col", to.getFile().ordinal());
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
        resetBoard();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Game reset");
        response.put("currentTurn", "white");
        response.put("moveHistory", new ArrayList<>(moveHistory));
        response.put("gameStatus", getCurrentGameStatus());
        return ResponseEntity.ok(response);
    }

    private String getCurrentGameStatus() {
        if (board.isMated()) {
            return board.isKingAttacked()
                ? "checkmate-" + (board.getSideToMove() == Side.WHITE ? "black" : "white")
                : "stalemate";
        }
        if (board.isDraw()) {
            return "draw";
        }
        if (board.isKingAttacked()) {
            return "check-" + (board.getSideToMove() == Side.WHITE ? "white" : "black");
        }
        return "normal";
    }
}
