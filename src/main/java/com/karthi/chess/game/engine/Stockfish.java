package com.karthi.chess.game.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Stockfish {
    private Process engineProcess;
    private BufferedReader processReader;
    private BufferedWriter processWriter;
    private final String enginePath;

    public Stockfish() {
        // Since Stockfish is in PATH, we can call it directly
        this.enginePath = "stockfish";
    }

    public Stockfish(String enginePath) {
        this.enginePath = enginePath;
    }

    public boolean startEngine() {
        try {
            engineProcess = new ProcessBuilder(enginePath).start();
            processReader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            processWriter = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
            sendCommand("uci");
            waitReady();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void waitReady() throws IOException {
        String line;
        while ((line = processReader.readLine()) != null) {
            if (line.equals("uciok")) break;
        }
    }

    public void sendCommand(String command) throws IOException {
        processWriter.write(command + "\n");
        processWriter.flush();
    }

    public String getOutput(int waitTimeMs) throws IOException, InterruptedException {
        Thread.sleep(waitTimeMs);
        StringBuilder output = new StringBuilder();
        while (processReader.ready()) {
            output.append(processReader.readLine()).append("\n");
        }
        return output.toString();
    }

    public String getBestMove(String fen, int timeMs) throws IOException, InterruptedException {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + timeMs);
        String line;
        String bestMove = null;
        while ((line = processReader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                bestMove = line.split(" ")[1];
                break;
            }
        }
        return bestMove;
    }

    public void stopEngine() throws IOException {
        sendCommand("quit");
        processReader.close();
        processWriter.close();
        engineProcess.destroy();
    }
}
