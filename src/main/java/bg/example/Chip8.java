package bg.example;

import bg.example.clock.Clock;
import bg.example.counter.Counter;
import bg.example.display.Display;
import bg.example.memory.Memory;
import bg.example.register.Register;

import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

public class Chip8 implements Runnable{

    private static final byte BYTE_SIZE = 8;
    private static final byte NIBBLE_SHIFT_OFFSET = 4;

    private static final byte DISPLAY_WIDTH = 64;
    private static final byte DISPLAY_HEIGHT = 32;

    private Counter programCounter;
    private Counter delayCounter;
    private Counter soundCounter;

    private Deque<Integer> programStack;

    private Clock clock;
    private Memory memory;
    private Display display; //64 wide 32 tall
    private Register[] registers; //16 registers
    private Register indexRegister;

    public Chip8(Counter programCounter, Counter delayCounter, Counter soundCounter,
                 Deque<Integer> programStack, Clock clock, Memory memory, Display display,
                 Register[] registers, Register indexRegister) {
        this.programCounter = programCounter;
        this.delayCounter = delayCounter;
        this.soundCounter = soundCounter;
        this.programStack = programStack;
        this.clock = clock;
        this.memory = memory;
        this.display = display;
        this.registers = registers;
        this.indexRegister = indexRegister;
    }

    private int fetch() {
        int fetchedInstruction = 0;

        fetchedInstruction |= ( memory.get( programCounter.get() ) & 0xFF ) << BYTE_SIZE;
        programCounter.increment();

        fetchedInstruction |= memory.get( programCounter.get() ) & 0xFF;
        programCounter.increment();

        return fetchedInstruction;
    }

    private int[] split(int instruction) {
        int[] nibbles = new int[4];

        for (int i = 0; i < 4; i++) {
            int mask = 0xF << (3 - i) * NIBBLE_SHIFT_OFFSET;
            nibbles[i] = (instruction & mask) >>> (3 - i) * NIBBLE_SHIFT_OFFSET;
        }

        return nibbles;
    }

    private void execute(int[] nibbles) {
        switch (nibbles[0]) {
            case 0x0:
                if (nibbles[3] == 0x0) {
                    display.clear();
                } else {
                    subroutineEnd();
                }
                break;

            case 0x1:
                jump(
                    combine(
                        nibbles[1],
                        nibbles[2],
                        nibbles[3]
                    )
                );
                break;

            case 0x2:
                subroutineCall(
                    combine(
                        nibbles[1],
                        nibbles[2],
                        nibbles[3]
                    )
                );
                break;

            case 0x3:
                skipIfEqual(
                    registers[nibbles[1]].get(),
                    combine(nibbles[2], nibbles[3])
                );
                break;

            case 0x4:
                skipIfNotEqual(
                    registers[nibbles[1]].get(),
                    combine(nibbles[2], nibbles[3])
                );
                break;

            case 0x5:
                skipIfEqual(
                    registers[nibbles[1]].get(),
                    registers[nibbles[2]].get()
                );
                break;

            case 0x6:
                setRegister(
                    nibbles[1],
                    combine(nibbles[2], nibbles[3])
                );
                break;

            case 0x7:
                addToRegister(
                    nibbles[1],
                    combine(nibbles[2], nibbles[3])
                );
                break;

            case 0x8:
                //todo
                break;

            case 0x9:
                skipIfNotEqual(
                    registers[nibbles[1]].get(),
                    registers[nibbles[2]].get()
                );
                break;

            case 0xA:
                setIndexRegister(
                    combine(
                        nibbles[1],
                        nibbles[2],
                        nibbles[3]
                    )
                );
                break;

            case 0xB:
                int address = registers[0].get();

                address += combine(
                    nibbles[1],
                    nibbles[2],
                    nibbles[3]
                );

                jump(address);
                break;

            case 0xC:
                int random = ThreadLocalRandom.current().nextInt() & combine(nibbles[2], nibbles[3]);
                registers[nibbles[1]].set(random);
                break;

            case 0xD:
                drawSprite(
                    nibbles[1],
                    nibbles[2],
                    nibbles[3]
                );
                break;

            case 0xE:
                break;

            case 0xF:
                break;

            default:
                throw new RuntimeException("Instruction could not be decoded");
        }
    }

    private int combine(int firstNibble, int secondNibble) {
        int result = firstNibble << NIBBLE_SHIFT_OFFSET;
        result += secondNibble;
        return result;
    }

    private int combine(int firstNibble, int secondNibble, int thirdNibble) {
        int result = firstNibble << NIBBLE_SHIFT_OFFSET * 2;
        result += secondNibble << NIBBLE_SHIFT_OFFSET;
        result += thirdNibble;
        return result;
    }

    private void jump(int address) {
        programCounter.set(address);
    }

    private void subroutineCall(int address) {
        programStack.push(programCounter.get());
        programCounter.set(address);
    }

    private void subroutineEnd() {
        int returnAddress = programStack.pop();
        programCounter.set(returnAddress);
    }

    private void setRegister(int index, int value) {
        registers[index].set(value);
    }

    private void addToRegister(int index, int value) {
        Register register = registers[index];

        int oldValue = register.get();
        int newValue = value + oldValue;

        register.set(newValue);
    }

    private void setIndexRegister(int value) {
        indexRegister.set(value);
    }

    private void drawSprite(int indexX, int indexY, int pixelCountHigh) {
        int Xcoord = registers[indexX].get() % DISPLAY_WIDTH;
        int Ycoord = registers[indexY].get() % DISPLAY_HEIGHT;

        int spriteAddress = indexRegister.get() & 0xFFFF;
        boolean wasAnyPixelTurnedOff = false;

        for (int y = Ycoord; y < Ycoord + pixelCountHigh; y++) {
            if (y == DISPLAY_HEIGHT){
                break;
            }

            int pixelSprite = memory.get(spriteAddress + y - Ycoord);

            for (int x = Xcoord; x < Xcoord + BYTE_SIZE; x++) {
                if (x == DISPLAY_WIDTH) {
                    break;
                }

                int bitOffset = x - Xcoord;
                if (((pixelSprite & 1 << BYTE_SIZE - bitOffset) >>>  BYTE_SIZE - bitOffset) == 1) {
                    wasAnyPixelTurnedOff |= display.flipPixel(x, y);
                }
            }

        }

        if (wasAnyPixelTurnedOff) {
            registers[15].set(1);
        } else {
            registers[15].set(0);
        }

        display.update();
    };

    private void skipIfEqual(int v1, int v2) {
        if (v1 == v2) {
            programCounter.increment();
        }
    }

    private void skipIfNotEqual(int v1, int v2) {
        if (v1 != v2) {
            programCounter.increment();
        }
    }

    @Override
    public void run() {
        while (true) {
            clock.tick();
            int instruction = fetch();
            int[] nibbles = split(instruction);
            execute(nibbles);
        }
    }

}