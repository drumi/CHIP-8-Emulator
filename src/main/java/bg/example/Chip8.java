package bg.example;

import bg.example.clock.Clock;
import bg.example.counter.Counter;
import bg.example.display.Display;
import bg.example.keyboard.KeyboardInformation;
import bg.example.memory.Memory;
import bg.example.register.Register;
import javafx.scene.input.KeyCode;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Chip8 implements Runnable {

    private static final byte BYTE_SIZE = 8;
    private static final byte NIBBLE_SHIFT_OFFSET = 4;

    private static final byte DISPLAY_WIDTH = 64;
    private static final byte DISPLAY_HEIGHT = 32;

    private static final int VF_REGISTER = 15;

    private final Counter programCounter;
    private final Counter delayCounter;
    private final Counter soundCounter;

    private final Deque<Integer> programStack;

    private final Clock clock;
    private final Memory memory;
    private final Display display;
    private final KeyboardInformation keyboardInformation;

    private final Register[] registers;
    private final Register indexRegister;

    private final Map<Integer, Consumer<int[]>> opcodes;
    private final Map<Integer, Consumer<int[]>> opcodes8xyn;

    public Chip8(Counter programCounter, Counter delayCounter, Counter soundCounter,
                 Deque<Integer> programStack, Clock clock, Memory memory, Display display,
                 KeyboardInformation keyboardInformation, Register[] registers, Register indexRegister) {
        this.programCounter = programCounter;
        this.delayCounter = delayCounter;
        this.soundCounter = soundCounter;
        this.programStack = programStack;
        this.clock = clock;
        this.memory = memory;
        this.display = display;
        this.keyboardInformation = keyboardInformation;
        this.registers = registers;
        this.indexRegister = indexRegister;

        opcodes = new HashMap<>();
        opcodes8xyn = new HashMap<>();

        initOpcodesMap();
        initOpcodes8xynMap();
    }

    private void initOpcodesMap() {
        opcodes.put(0,  this::opcode_0UUU);
        opcodes.put(1,  this::opcode_1NNN);
        opcodes.put(2,  this::opcode_2NNN);
        opcodes.put(3,  this::opcode_3XNN);
        opcodes.put(4,  this::opcode_4XNN);
        opcodes.put(5,  this::opcode_5XY0);
        opcodes.put(6,  this::opcode_6XNN);
        opcodes.put(7,  this::opcode_7XNN);
        opcodes.put(8,  this::opcode_8XYN);
        opcodes.put(9,  this::opcode_9XY0);
        opcodes.put(10, this::opcode_ANNN);
        opcodes.put(11, this::opcode_BNNN);
        opcodes.put(12, this::opcode_CXNN);
        opcodes.put(13, this::opcode_DXYN);
        opcodes.put(14, this::opcode_EXNN);
        opcodes.put(15, this::opcode_FXNN);
    }

    private void initOpcodes8xynMap() {
        opcodes.put(0,  this::opcode_8XY0);
        opcodes.put(1,  this::opcode_8XY1);
        opcodes.put(2,  this::opcode_8XY2);
        opcodes.put(3,  this::opcode_8XY3);
        opcodes.put(4,  this::opcode_8XY4);
        opcodes.put(5,  this::opcode_8XY5);
        opcodes.put(6,  this::opcode_8XY6);
        opcodes.put(7,  this::opcode_8XY7);
        opcodes.put(14, this::opcode_8XYE);
    }

    private int fetch() {
        int fetchedInstruction = 0;

        fetchedInstruction |=  memory.get( programCounter.get() ) << BYTE_SIZE;
        programCounter.increment();

        fetchedInstruction |= memory.get( programCounter.get() );
        programCounter.increment();

        return fetchedInstruction;
    }

    private int[] splitIntoNibbles(int instruction) {
        int[] nibbles = new int[4];

        for (int i = 0; i < 4; i++) {
            int mask = 0xF << (3 - i) * NIBBLE_SHIFT_OFFSET;
            nibbles[i] = (instruction & mask) >>> (3 - i) * NIBBLE_SHIFT_OFFSET;
        }

        return nibbles;
    }

    private void execute(int[] nibbles) {
        opcodes.get(nibbles[0])
               .accept(nibbles);
    }

    /**
     *  Either display clear instruction or subroutine end instruction, depending on last nibble
     */
    private void opcode_0UUU(int[] nibbles) {
        if (nibbles[3] == 0x0) {
            display.clear();
        } else {
            subroutineEnd();
        }
    }

    /**
     *  Jump instruction. Sets program counter to address NNN
     */
    private void opcode_1NNN(int[] nibbles) {
        jump(
            combine(
                nibbles[1],
                nibbles[2],
                nibbles[3]
            )
        );
    }

    /**
     * Subroutine call instruction. Pushes current program counter to stack and sets it to NNN
     */
    private void opcode_2NNN(int[] nibbles) {
        subroutineCall(
            combine(
                nibbles[1],
                nibbles[2],
                nibbles[3]
            )
        );
    }


    /**
     * Skips one instruction if the value at register X is equal to NN
     */
    private void opcode_3XNN(int[] nibbles) {
        skipIfEqual(
            registers[nibbles[1]].get(),
            combine(nibbles[2], nibbles[3])
        );
    }

    /**
     * Skips one instruction if the value at register X is not equal to NN
     */
    private void opcode_4XNN(int[] nibbles) {
        skipIfNotEqual(
            registers[nibbles[1]].get(),
            combine(nibbles[2], nibbles[3])
        );
    }

    /**
     * Skips one instruction if the value at register X is equal to the value at register Y
     */
    private void opcode_5XY0(int[] nibbles) {
        skipIfEqual(
            registers[nibbles[1]].get(),
            registers[nibbles[2]].get()
        );
    }

    /**
     * Sets the value at register X to NN
     */
    private void opcode_6XNN(int[] nibbles) {
        setRegister(
            nibbles[1],
            combine(nibbles[2], nibbles[3])
        );
    }

    /**
     * Adds NN to the value at register X. Overflowing does not set the flag at register VF
     */
    private void opcode_7XNN(int[] nibbles) {
        addToRegister(
            nibbles[1],
            combine(nibbles[2], nibbles[3])
        );
    }

    /**
     * Logical and arithmetic instructions, depending on the last nibble
     */
    private void opcode_8XYN(int[] nibbles) {
        opcodes8xyn.get(nibbles[3]).accept(nibbles);
    }

    /**
     * VX is set to the value of VY
     */
    private void opcode_8XY0(int[] nibbles) {
      int Yvalue = registers[nibbles[2]].get();
      registers[nibbles[1]].set(Yvalue);
    }

    /**
     * VX is set to the value of VX bitwise OR VY
     */
    private void opcode_8XY1(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        registers[nibbles[1]].set(Xvalue | Yvalue);
    }

    /**
     * VX is set to the value of VX bitwise AND VY
     */
    private void opcode_8XY2(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        registers[nibbles[1]].set(Xvalue & Yvalue);
    }

    /**
     * VX is set to the value of VX bitwise XOR VY
     */
    private void opcode_8XY3(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        registers[nibbles[1]].set(Xvalue ^ Yvalue);
    }

    /**
     * VX is set to the value of VX plus VY
     */
    private void opcode_8XY4(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        registers[nibbles[1]].set(Xvalue + Yvalue);
    }

    /**
     * VX is set to the value of VX - VY
     */
    private void opcode_8XY5(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        registers[nibbles[1]].set(Xvalue - Yvalue);
    }

    /**
     * VX is set to the value of VY right shift. VF is set to dropped bit
     */
    private void opcode_8XY6(int[] nibbles) {
        int Yvalue = registers[nibbles[2]].get();

        int newValue = Yvalue >> 1;
        registers[nibbles[1]].set(newValue);

        registers[VF_REGISTER].set(Yvalue & 1);
    }

    /**
     * VX is set to the value of VY - VX
     */
    private void opcode_8XY7(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        registers[nibbles[1]].set(Xvalue - Yvalue);
    }

    /**
     * VX is set to the value of VY left shift. VF is set to dropped bit
     */
    private void opcode_8XYE(int[] nibbles) {
        int Yvalue = registers[nibbles[2]].get();

        int newValue = (Yvalue << 1) % 256;
        registers[nibbles[1]].set(newValue);

        if ((Yvalue & 128) != 0) {
            registers[VF_REGISTER].set(1);
        } else {
            registers[VF_REGISTER].set(0);
        }
    }

    /**
     * Skips one instruction if the value at register X is not equal to the value at register Y
     */
    private void opcode_9XY0(int[] nibbles) {
        skipIfNotEqual(
            registers[nibbles[1]].get(),
            registers[nibbles[2]].get()
        );
    }

    /**
     * Sets the value at the index register to NNN
     */
    private void opcode_ANNN(int[] nibbles) {
        setIndexRegister(
            combine(
                nibbles[1],
                nibbles[2],
                nibbles[3]
            )
        );
    }

    /**
     * Jumps to address NNN + the value at register V0
     */
    private void opcode_BNNN(int[] nibbles) {
        int address = registers[0].get();

        address += combine(
            nibbles[1],
            nibbles[2],
            nibbles[3]
        );

        jump(address);
    }

    /**
     * Generates a random number between 0 and NN and puts it in register VX
     */
    private void opcode_CXNN(int[] nibbles) {
        int random = ThreadLocalRandom.current()
                                      .nextInt();

        random &= combine(nibbles[2], nibbles[3]);

        registers[nibbles[1]].set(random);
    }

    /**
     * Draws N tall sprite at coordinates (value at register X, value at register Y).
     * if a pixel is turned off by the sprite, VF is set to 1, otherwise VF is set to 0
     */
    private void opcode_DXYN(int[] nibbles) {
        drawSprite(
            nibbles[1],
            nibbles[2],
            nibbles[3]
        );
    }

    /**
     * Instructions related to key presses
     */
    private void opcode_EXNN(int[] nibbles) {
        KeyCode key = toKeyCode(nibbles[1]);

        if (nibbles[3] == 0xE) {
            if (keyboardInformation.isPressed(key)) {
                programCounter.increment();
            }
        } else if (nibbles[3] == 0x1) {
            if (!keyboardInformation.isPressed(key)) {
                programCounter.increment();
            }
        }
    }

    /**
     * Miscellaneous instructions
     */
    private void opcode_FXNN(int[] nibbles) {
        //TODO
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

        int spriteAddress = indexRegister.get();
        boolean wasAnyPixelTurnedOff = false;

        for (int y = Ycoord; y < Ycoord + pixelCountHigh; y++) {
            if (y == DISPLAY_HEIGHT) {
                break;
            }

            int currentPixelRow = memory.get(spriteAddress + y - Ycoord);

            for (int x = Xcoord; x <= Xcoord + BYTE_SIZE; x++) {
                if (x == DISPLAY_WIDTH) {
                    break;
                }

                int bitOffset = x - Xcoord;
                int currentBit = (currentPixelRow & (1 << BYTE_SIZE - bitOffset)) >>>  BYTE_SIZE - bitOffset;
                if (currentBit == 1) {
                    wasAnyPixelTurnedOff |= display.flipPixel(x, y);
                }
            }

        }

        if (wasAnyPixelTurnedOff) {
            registers[VF_REGISTER].set(1);
        } else {
            registers[VF_REGISTER].set(0);
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

    private KeyCode toKeyCode(int value) {
        return switch (value) {
            case 0 -> KeyCode.NUMPAD0;
            case 1 -> KeyCode.NUMPAD1;
            case 2 -> KeyCode.NUMPAD2;
            case 3 -> KeyCode.NUMPAD3;
            case 4 -> KeyCode.NUMPAD4;
            case 5 -> KeyCode.NUMPAD5;
            case 6 -> KeyCode.NUMPAD6;
            case 7 -> KeyCode.NUMPAD7;
            case 8 -> KeyCode.NUMPAD8;
            case 9 -> KeyCode.NUMPAD9;
            case 10 -> KeyCode.A;
            case 11 -> KeyCode.B;
            case 12 -> KeyCode.C;
            case 13 -> KeyCode.D;
            case 14 -> KeyCode.E;
            case 15 -> KeyCode.F;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    public void runOneCycle() {
        clock.tick();
        int instruction = fetch();
        int[] nibbles = splitIntoNibbles(instruction);
        execute(nibbles);
    }

    @Override
    public void run() {
        while (true) {
            runOneCycle();
        }
    }

}