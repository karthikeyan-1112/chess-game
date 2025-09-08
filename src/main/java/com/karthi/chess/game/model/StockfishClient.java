package com.karthi.chess.game.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class StockfishClient {
    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final String path;

    public StockfishClient(String path) {
        this.path = resolvePath(path);
    }

    /**
     * Resolve engine path depending on environment (local vs Docker).
     */
    private String resolvePath(String originalPath) {
    java.io.File dockerPath = new java.io.File("/app/engine/stockfish");

    if (dockerPath.exists() && dockerPath.canExecute()) {
        System.out.println("✅ Using Docker Stockfish binary: " + dockerPath.getAbsolutePath());
        return dockerPath.getAbsolutePath();
    }

    System.out.println("⚠️ Using local Stockfish binary: " + originalPath);
    return originalPath;
}

    

    /**
     * Start the Stockfish engine process.
     */
    public boolean start() throws IOException {
        engineProcess = new ProcessBuilder(path).redirectErrorStream(true).start();
        reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
        return true;
    }

    public void sendCommand(String cmd) throws IOException {
        writer.write(cmd + "\n");
        writer.flush();
    }

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
            if (System.currentTimeMillis() - start > 1000) break;
        }
        return null;
    }

    public void stop() throws IOException {
        try {
            sendCommand("quit");
        } catch (Exception ignored) {}
        reader.close();
        writer.close();
    }
}
