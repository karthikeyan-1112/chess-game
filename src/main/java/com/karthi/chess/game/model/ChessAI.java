package com.karthi.chess.game.model;

import java.util.List;
import java.util.Random;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

public class ChessAI {

    public static Move getGreedyMove(Board board, Side side) {
        List<Move> legalMoves = board.legalMoves();
        Move bestMove = null;
        int maxValue = Integer.MIN_VALUE;

        for (Move move : legalMoves) {
            // Skip if not our turn
            if (board.getSideToMove() != side) continue;

            Piece targetPiece = board.getPiece(move.getTo());
            int value = 0;

            if (targetPiece != null &&
                targetPiece != Piece.NONE &&
                targetPiece.getPieceSide() != side) {

                value = getPieceValue(targetPiece);
            }

            if (value > maxValue) {
                maxValue = value;
                bestMove = move;
            }
        }

        if (bestMove == null && !legalMoves.isEmpty()) {
            // If no capturing moves, return a random legal move
            return legalMoves.get(new Random().nextInt(legalMoves.size()));
        }

        return bestMove;
    }

    private static int getPieceValue(Piece piece) {
        // Defensive null check
        if (piece == null || piece == Piece.NONE || piece.getPieceType() == null) {
            return 0;
        }

        return switch (piece.getPieceType()) {
            case PAWN -> 10;
            case KNIGHT -> 30;
            case BISHOP -> 30;
            case ROOK -> 50;
            case QUEEN -> 90;
            case KING -> 900;
            default -> 0;
        };
    }
}
