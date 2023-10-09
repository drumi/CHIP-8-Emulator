package bg.example.chip;

import bg.example.clock.Clock;
import bg.example.counter.Counter;
import bg.example.display.Display;
import bg.example.loader.font.FontLoader;
import bg.example.keyboard.KeyboardInformation;
import bg.example.loader.program.ProgramLoader;
import bg.example.memory.Memory;
import bg.example.register.Register;
import bg.example.loader.rom.ROMLoader;
import javafx.scene.input.KeyCode;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Chip8 implements Runnable {

    public static final byte BYTE_SIZE = 8;
    public static final byte NIBBLE_SHIFT_OFFSET = 4;

    public static final byte DISPLAY_WIDTH = 64;
    public static final byte DISPLAY_HEIGHT = 32;

    public static final int VF_REGISTER_INDEX = 15;

    public static final int NORMAL_REGISTER_BITS = 8;
    public static final int INDEX_REGISTER_BITS = 16;
    public static final int NORMAL_REGISTERS_COUNT = 16;

    public static final int TIMER_FREQUENCY = 60;

    public static final int FIRST_INSTRUCTION_OFFSET = 0x200;
    public static final int CHIP8_MEMORY_SIZE = 4096;
    public static final int FONT_OFFSET = 0;

    public static final Set<KeyCode> LEGAL_KEYS;

    static {
        LEGAL_KEYS =
            Set.of(
                KeyCode.DIGIT0,
                KeyCode.DIGIT1,
                KeyCode.DIGIT2,
                KeyCode.DIGIT3,
                KeyCode.DIGIT4,
                KeyCode.DIGIT5,
                KeyCode.DIGIT6,
                KeyCode.DIGIT7,
                KeyCode.DIGIT8,
                KeyCode.DIGIT9,
                KeyCode.A,
                KeyCode.B,
                KeyCode.C,
                KeyCode.D,
                KeyCode.E,
                KeyCode.F
            );
    }

    private final Counter programCounter;
    private final Counter delayCounter;
    private final Counter soundCounter;

    private final Deque<Integer> programStack;
    private final Clock clock;
    private final Memory memory;

    private final ProgramLoader programLoader;

    private final Display display;
    private final KeyboardInformation keyboardInformation;

    private final Register[] registers;
    private final Register indexRegister;

    private final Map<Integer, Consumer<int[]>> opcodes;
    private final Map<Integer, Consumer<int[]>> opcodes8xyn;
    private final Map<Integer, Consumer<int[]>> opcodesFxnn;

    public Chip8(Chip8Properties properties) {
        this.programCounter = properties.programCounter();
        this.delayCounter = properties.delayCounter();
        this.soundCounter = properties.soundCounter();
        this.clock = properties.clock();
        this.memory = properties.memory();
        this.programLoader = properties.programLoader();
        this.display = properties.display();
        this.keyboardInformation = properties.keyboardInformation();
        this.registers = properties.registers();
        this.indexRegister = properties.indexRegister();

        programStack = new ArrayDeque<>();
        opcodes = new HashMap<>();
        opcodes8xyn = new HashMap<>();
        opcodesFxnn = new HashMap<>();

        initOpcodesMap();
        initOpcodes8xynMap();
        initOpcodesFxnnMap();

        programLoader.load(memory);
    }

    private void initOpcodesMap() {
        opcodes.put(0x0, this::opcode_0UUU);
        opcodes.put(0x1, this::opcode_1NNN);
        opcodes.put(0x2, this::opcode_2NNN);
        opcodes.put(0x3, this::opcode_3XNN);
        opcodes.put(0x4, this::opcode_4XNN);
        opcodes.put(0x5, this::opcode_5XY0);
        opcodes.put(0x6, this::opcode_6XNN);
        opcodes.put(0x7, this::opcode_7XNN);
        opcodes.put(0x8, this::opcode_8XYN);
        opcodes.put(0x9, this::opcode_9XY0);
        opcodes.put(0xA, this::opcode_ANNN);
        opcodes.put(0xB, this::opcode_BNNN);
        opcodes.put(0xC, this::opcode_CXNN);
        opcodes.put(0xD, this::opcode_DXYN);
        opcodes.put(0xE, this::opcode_EXNN);
        opcodes.put(0xF, this::opcode_FXNN);
    }

    private void initOpcodes8xynMap() {
        opcodes8xyn.put(0x0, this::opcode_8XY0);
        opcodes8xyn.put(0x1, this::opcode_8XY1);
        opcodes8xyn.put(0x2, this::opcode_8XY2);
        opcodes8xyn.put(0x3, this::opcode_8XY3);
        opcodes8xyn.put(0x4, this::opcode_8XY4);
        opcodes8xyn.put(0x5, this::opcode_8XY5);
        opcodes8xyn.put(0x6, this::opcode_8XY6);
        opcodes8xyn.put(0x7, this::opcode_8XY7);
        opcodes8xyn.put(0xE, this::opcode_8XYE);
    }

    private void initOpcodesFxnnMap() {
        opcodesFxnn.put(0x3, this::opcode_FX33);
        opcodesFxnn.put(0x7, this::opcode_FX07);
        opcodesFxnn.put(0x8, this::opcode_FX18);
        opcodesFxnn.put(0x9, this::opcode_FX29);
        opcodesFxnn.put(0xA, this::opcode_FX0A);
        opcodesFxnn.put(0xE, this::opcode_FX1E);

        opcodesFxnn.put(0x5,
            nibbles -> {
                if (nibbles[2] == 0x1) {
                    opcode_FX15(nibbles);
                } else if (nibbles[2] == 0x5) {
                    opcode_FX55(nibbles);
                } else {
                    opcode_FX65(nibbles);
                }
            }
        );
    }

    private int fetch() {
        int fetchedInstruction = 0;

        fetchedInstruction |= memory.get(programCounter.get()) << BYTE_SIZE;
        programCounter.increment();

        fetchedInstruction |= memory.get(programCounter.get());
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
     * Either display clear instruction or subroutine end instruction, depending on last nibble
     */
    private void opcode_0UUU(int[] nibbles) {
        if (nibbles[3] == 0x0) {
            display.clear();
        } else {
            subroutineEnd();
        }
    }

    /**
     * Jump instruction. Sets program counter to address NNN
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
        registers[nibbles[1]].set(
            combine(nibbles[2], nibbles[3])
        );
    }

    /**
     * Adds NN to the value at register X. Overflowing does not set the flag at register VF
     */
    private void opcode_7XNN(int[] nibbles) {
        int oldValue = registers[nibbles[1]].get();

        registers[nibbles[1]].set(
            oldValue + combine(nibbles[2], nibbles[3])
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

        boolean overflow = registers[nibbles[1]].set(Xvalue + Yvalue);

        registers[VF_REGISTER_INDEX].set(
            overflow ? 1 : 0
        );
    }

    /**
     * VX is set to the value of VX - VY
     */
    private void opcode_8XY5(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        boolean underflow = registers[nibbles[1]].set(Xvalue - Yvalue);

        registers[VF_REGISTER_INDEX].set(
            underflow ? 0 : 1
        );
    }

    /**
     * VX is set to the value of VY right shift. VF is set to dropped bit
     */
    private void opcode_8XY6(int[] nibbles) {
        int Yvalue = registers[nibbles[2]].get();

        int newValue = Yvalue >> 1;
        registers[nibbles[1]].set(newValue);

        registers[VF_REGISTER_INDEX].set(Yvalue & 1);
    }

    /**
     * VX is set to the value of VY - VX
     */
    private void opcode_8XY7(int[] nibbles) {
        int Xvalue = registers[nibbles[1]].get();
        int Yvalue = registers[nibbles[2]].get();

        boolean underflow = registers[nibbles[1]].set(Yvalue - Xvalue);

        registers[VF_REGISTER_INDEX].set(
            underflow ? 0 : 1
        );
    }

    /**
     * VX is set to the value of VY left shift. VF is set to dropped bit
     */
    private void opcode_8XYE(int[] nibbles) {
        int Yvalue = registers[nibbles[2]].get();

        int newValue = (Yvalue << 1) % 256;
        registers[nibbles[1]].set(newValue);

        if ((Yvalue & 128) != 0) {
            registers[VF_REGISTER_INDEX].set(1);
        } else {
            registers[VF_REGISTER_INDEX].set(0);
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
        indexRegister.set(
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
        KeyCode key = fromIntegerToKeyCode(
            registers[nibbles[1]].get()
        );

        if (nibbles[3] == 0xE) {
            if (keyboardInformation.isPressed(key)) {
                skipInstruction();
            }
        } else if (nibbles[3] == 0x1) {
            if (!keyboardInformation.isPressed(key)) {
                skipInstruction();
            }
        }
    }

    /**
     * Miscellaneous instructions
     */
    private void opcode_FXNN(int[] nibbles) {
        opcodesFxnn.get(nibbles[3]).accept(nibbles);
    }

    /**
     * Sets VX to the current value of the delay timer
     */
    private void opcode_FX07(int[] nibbles) {
        registers[nibbles[1]].set(
            delayCounter.get()
        );
    }

    /**
     * Sets the delay timer to the value of VX
     */
    private void opcode_FX15(int[] nibbles) {
        delayCounter.set(
            registers[nibbles[1]].get()
        );
    }

    /**
     * Sets the sound timer to the value of VX
     */
    private void opcode_FX18(int[] nibbles) {
        soundCounter.set(
            registers[nibbles[1]].get()
        );
    }

    /**
     * Add the value of VX to the index register. Set VF to one if result is bigger than 0xFFF
     */
    private void opcode_FX1E(int[] nibbles) {
        int newValue = indexRegister.get() + registers[nibbles[1]].get();

        indexRegister.set(newValue);

        if (newValue > 0xFFF) {
            registers[VF_REGISTER_INDEX].set(1);
        }
    }

    /**
     * Block till key is pressed and released
     */
    private void opcode_FX0A(int[] nibbles) {
        KeyCode lastPressed = keyboardInformation.getLastPressedKey();
        KeyCode newestPressed = lastPressed;

        while (lastPressed == newestPressed) {
            newestPressed = keyboardInformation.getLastPressedKey();

            if (newestPressed != null && !LEGAL_KEYS.contains(newestPressed)) {
                newestPressed = lastPressed;
            }

            Thread.yield();
        }

        while (keyboardInformation.isPressed(newestPressed)) {
            Thread.yield();
        }

        registers[nibbles[1]].set(fromKeyCodeToInteger(newestPressed));
    }

    /**
     * Sets the index register to the value of VX
     */
    private void opcode_FX29(int[] nibbles) {
        int mask = 0xF;

        indexRegister.set(
            registers[nibbles[1]].get() & mask
        );
    }

    /**
     * Binary coded decimal conversion. Converts the value at VX to three
     * decimal digits and puts them at the add the address pointed by the index register
     */
    private void opcode_FX33(int[] nibbles) {
        int value = registers[nibbles[1]].get();

        int firstDigit = value / 100;
        int secondDigit = (value % 100) / 10;
        int thirdDigit = value % 10;

        int address = indexRegister.get();

        memory.set(address, firstDigit);
        memory.set(address + 1, secondDigit);
        memory.set(address + 2, thirdDigit);
    }

    /**
     * Stores the values of V0 to VX inclusive into memory pointed by the index register
     */
    private void opcode_FX55(int[] nibbles) {
        int address = indexRegister.get();

        for (int i = 0; i <= nibbles[1]; i++) {
            memory.set(address + i, registers[i].get());
        }
    }

    /**
     * Loads the memory pointed by the index register into V0 to VX inclusive
     */
    private void opcode_FX65(int[] nibbles) {
        int address = indexRegister.get();

        for (int i = 0; i <= nibbles[1]; i++) {
            registers[i].set(
                memory.get(address + i)
            );
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
                int currentBit = (currentPixelRow & (1 << BYTE_SIZE - bitOffset)) >>> BYTE_SIZE - bitOffset;
                if (currentBit == 1) {
                    wasAnyPixelTurnedOff |= display.flipPixel(x, y);
                }
            }

        }

        if (wasAnyPixelTurnedOff) {
            registers[VF_REGISTER_INDEX].set(1);
        } else {
            registers[VF_REGISTER_INDEX].set(0);
        }

        display.update();
    }

    private void skipIfEqual(int v1, int v2) {
        if (v1 == v2) {
            skipInstruction();
        }
    }

    private void skipIfNotEqual(int v1, int v2) {
        if (v1 != v2) {
            skipInstruction();
        }
    }

    private void skipInstruction() {
        programCounter.increment();
        programCounter.increment();
    }

    private void repeatInstruction() {
        programCounter.decrement();
        programCounter.decrement();
    }

    private KeyCode fromIntegerToKeyCode(int value) {
        return switch (value) {
            case 0 -> KeyCode.DIGIT0;
            case 1 -> KeyCode.DIGIT1;
            case 2 -> KeyCode.DIGIT2;
            case 3 -> KeyCode.DIGIT3;
            case 4 -> KeyCode.DIGIT4;
            case 5 -> KeyCode.DIGIT5;
            case 6 -> KeyCode.DIGIT6;
            case 7 -> KeyCode.DIGIT7;
            case 8 -> KeyCode.DIGIT8;
            case 9 -> KeyCode.DIGIT9;
            case 10 -> KeyCode.A;
            case 11 -> KeyCode.B;
            case 12 -> KeyCode.C;
            case 13 -> KeyCode.D;
            case 14 -> KeyCode.E;
            case 15 -> KeyCode.F;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    private int fromKeyCodeToInteger(KeyCode key) {
        return switch (key) {
            case DIGIT0 -> 0;
            case DIGIT1 -> 1;
            case DIGIT2 -> 2;
            case DIGIT3 -> 3;
            case DIGIT4 -> 4;
            case DIGIT5 -> 5;
            case DIGIT6 -> 6;
            case DIGIT7 -> 7;
            case DIGIT8 -> 8;
            case DIGIT9 -> 9;
            case A -> 10;
            case B -> 11;
            case C -> 12;
            case D -> 13;
            case E -> 14;
            case F -> 15;
            default -> throw new IllegalStateException("Unexpected key: " + key);
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
