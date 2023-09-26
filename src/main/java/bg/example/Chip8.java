package bg.example;

import java.util.Deque;

public class Chip8 {

    short programCounter;
    short indexRegister;

    byte delayTimer;
    byte soundTimer;

    Deque<Short> stack;

    byte[] registers; //16 registers
    byte[] memory; //4096 bytes
    byte[][] display; //64 wide 32 tall

    private void fetch() {

    }

    private void decode() {

    }

    private void execute() {

    }

}