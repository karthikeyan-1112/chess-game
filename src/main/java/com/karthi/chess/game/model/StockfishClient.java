package com.karthi.chess.game.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Simple UCI client wrapper for Stockfish engine.
 */
public class StockfishClient {
    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final String path;

    public StockfishClient(String path) {
        this.path = path;
    }

    /**
     * Start the Stockfish engine process.
     */
    public boolean start() throws IOException {
        // Using ProcessBuilder is safer than Runtime.exec
        engineProcess = new ProcessBuilder(path).start();
        reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
        return true;
    }

    /**
     * Send a raw command to the Stockfish process.
     */
    public void sendCommand(String cmd) throws IOException {
        writer.write(cmd + "\n");
        writer.flush();
    }

    /**
     * Get the best move from the current position (FEN).
     *
     * @param fen Position in Forsyth–Edwards Notation.
     * @return Best move in UCI format, or null if not found.
     */
    public String getBestMove(String fen) throws IOException, InterruptedException {
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go movetime 500");

        String line;
        long start = System.currentTimeMillis();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                return line.split(" ")[1];
            }
            // Timeout safeguard: stop waiting if > 1 sec
            if (System.currentTimeMillis() - start > 1000) break;
        }
        return null;
    }

    /**
     * Stop the Stockfish engine process gracefully.
     */
    public void stop() throws IOException {
        sendCommand("quit");
        reader.close();
        writer.close();
    }
}
