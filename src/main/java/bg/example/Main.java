package bg.example;

import bg.example.ROM.BasicROMLoader;
import bg.example.ROM.ROMLoader;
import bg.example.clock.Clock;
import bg.example.clock.SimpleClock;
import bg.example.counter.Counter;
import bg.example.counter.ReducingCounter;
import bg.example.counter.SimpleCounter;
import bg.example.display.Display;
import bg.example.display.WindowDisplay;
import bg.example.font.FontLoader;
import bg.example.keyboard.Keyboard;
import bg.example.keyboard.KeyboardProxy;
import bg.example.memory.Memory;
import bg.example.memory.SimpleMemory;
import bg.example.register.Register;
import bg.example.register.SimpleRegister;

import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Main extends Application {

    private static final Path PROGRAM_LOCATION = Path.of("src/main/resources/Breakout [Carmelo Cortez, 1979].ch8");

    private static final int DISPLAY_WIDTH = 64;
    private static final int DISPLAY_HEIGHT = 32;

    private static final int REGISTER_BITS= 8;
    private static final int INDEX_REGISTER_BITS = 16;
    private static final int REGISTER_COUNT = 16;

    private static final int TIMER_FREQUENCY = 60;

    private static final int FIRST_INSTRUCTION_OFFSET = 0x200;
    private static final int CHIP8_MEMORY_SIZE = 4096;
    private static final int FONT_OFFSET = 0;

    private static final Map<KeyCode, KeyCode> KEY_REMAPPING = new HashMap<>() {
        {
            put(KeyCode.DIGIT1, KeyCode.DIGIT1);
            put(KeyCode.DIGIT2, KeyCode.DIGIT2);
            put(KeyCode.DIGIT3, KeyCode.DIGIT3);
            put(KeyCode.DIGIT4, KeyCode.C);
            put(KeyCode.Q, KeyCode.DIGIT4);
            put(KeyCode.W, KeyCode.DIGIT5);
            put(KeyCode.E, KeyCode.DIGIT6);
            put(KeyCode.R, KeyCode.D);
            put(KeyCode.A, KeyCode.DIGIT7);
            put(KeyCode.S, KeyCode.DIGIT8);
            put(KeyCode.D, KeyCode.DIGIT9);
            put(KeyCode.F, KeyCode.E);
            put(KeyCode.Z, KeyCode.A);
            put(KeyCode.X, KeyCode.DIGIT0);
            put(KeyCode.C, KeyCode.B);
            put(KeyCode.V, KeyCode.F);
        }
    };

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ROMLoader ROMloader = new BasicROMLoader();
        FontLoader fontLoader = new FontLoader();

        int[] bytes = new int[CHIP8_MEMORY_SIZE];
        fontLoader.load(bytes, FONT_OFFSET);
        ROMloader.load(PROGRAM_LOCATION, bytes, FIRST_INSTRUCTION_OFFSET);

        Counter programCounter = new SimpleCounter(FIRST_INSTRUCTION_OFFSET);
        Counter delayCounter = new ReducingCounter(0, TIMER_FREQUENCY);
        Counter soundCounter = new ReducingCounter(0, TIMER_FREQUENCY);

        Deque<Integer> programStack = new ArrayDeque<>();

        Clock clock = new SimpleClock();
        Memory memory = new SimpleMemory(bytes);

        Keyboard keyboard = new KeyboardProxy(KEY_REMAPPING);
        Display display = new WindowDisplay(new boolean[DISPLAY_HEIGHT][DISPLAY_WIDTH], stage, keyboard);

        Register indexRegister = new SimpleRegister(INDEX_REGISTER_BITS);
        Register[] registers = new SimpleRegister[REGISTER_COUNT];

        for (int i = 0; i < REGISTER_COUNT; i++) {
            registers[i] = new SimpleRegister(REGISTER_BITS);
        }

        Chip8 chip = new Chip8(
            programCounter,
            delayCounter,
            soundCounter,
            programStack,
            clock,
            memory,
            display,
            keyboard,
            registers,
            indexRegister
        );

        var thread = new Thread(chip);

        thread.setDaemon(true);
        thread.start();
    }
}
