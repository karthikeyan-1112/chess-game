package com.karthi.chess.game.model;

import java.util.List;
import java.util.Random;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;




public class ChessAI {

    public static Move getGreedyMove(Board board, Side side) {
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int maxValue = Integer.MIN_VALUE;

        for (Move move : legalMoves) {
            if (board.getSideToMove() != side) continue;

            Piece target = board.getPiece(move.getTo());
            int value = 0;
            if (target != null && target != Piece.NONE && target.getPieceSide() != side) {
                value = getPieceValue(target.getPieceType());
            }

            // Add small random factor to avoid always choosing the first best move
            value += new Random().nextInt(5);

            if (value > maxValue) {
                maxValue = value;
                bestMove = move;
            }
        }

        if (bestMove == null && !legalMoves.isEmpty()) {
            return legalMoves.get(new Random().nextInt(legalMoves.size()));
        }

        return bestMove;
    }

    private static int getPieceValue(PieceType type) {
        if (type == null) return 0;
        return switch (type) {
            case PAWN -> 10;
            case KNIGHT, BISHOP -> 30;
            case ROOK -> 50;
            case QUEEN -> 90;
            case KING -> 1000;
            default -> 0;
        };
    }
}