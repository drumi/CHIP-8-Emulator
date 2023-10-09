package bg.example.config;

import bg.example.chip.Chip8;
import bg.example.chip.Chip8Properties;
import bg.example.clock.Clock;
import bg.example.clock.SimpleClock;
import bg.example.counter.Counter;
import bg.example.counter.ReducingCounter;
import bg.example.counter.SimpleCounter;
import bg.example.display.Display;
import bg.example.display.WindowDisplay;
import bg.example.loader.font.BasicFontLoader;
import bg.example.loader.font.FontLoader;
import bg.example.keyboard.Keyboard;
import bg.example.keyboard.KeyboardProxy;
import bg.example.loader.program.ProgramLoader;
import bg.example.memory.Memory;
import bg.example.memory.SimpleMemory;
import bg.example.register.Register;
import bg.example.register.SimpleRegister;
import bg.example.loader.rom.BasicROMLoader;
import bg.example.loader.rom.ROMLoader;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.nio.file.Path;
import java.util.Map;

@Configuration
@ComponentScan("bg.example")
public class ProjectConfig {

    private static final Map<KeyCode, KeyCode> KEY_REMAPPING =
        Map.ofEntries(
            Map.entry(KeyCode.DIGIT1, KeyCode.DIGIT1),
            Map.entry(KeyCode.DIGIT2, KeyCode.DIGIT2),
            Map.entry(KeyCode.DIGIT3, KeyCode.DIGIT3),
            Map.entry(KeyCode.DIGIT4, KeyCode.C),
            Map.entry(KeyCode.Q, KeyCode.DIGIT4),
            Map.entry(KeyCode.W, KeyCode.DIGIT5),
            Map.entry(KeyCode.E, KeyCode.DIGIT6),
            Map.entry(KeyCode.R, KeyCode.D),
            Map.entry(KeyCode.A, KeyCode.DIGIT7),
            Map.entry(KeyCode.S, KeyCode.DIGIT8),
            Map.entry(KeyCode.D, KeyCode.DIGIT9),
            Map.entry(KeyCode.F, KeyCode.E),
            Map.entry(KeyCode.Z, KeyCode.A),
            Map.entry(KeyCode.X, KeyCode.DIGIT0),
            Map.entry(KeyCode.C, KeyCode.B),
            Map.entry(KeyCode.V, KeyCode.F)
        );

    @Bean
    @Scope("prototype")
    public Clock clock() {
        return new SimpleClock();
    }

    @Bean
    @Scope("prototype")
    @Primary
    public Counter simpleCounter() {
        return new SimpleCounter(Chip8.FIRST_INSTRUCTION_OFFSET);
    }

    @Bean
    @Scope("prototype")
    @Qualifier("reducingTimer")
    public Counter reducingCounter() {
        return new ReducingCounter(Chip8.FIRST_INSTRUCTION_OFFSET, Chip8.TIMER_FREQUENCY);
    }

    @Bean
    @Scope("prototype")
    public Memory memory() {
        return new SimpleMemory(new int[Chip8.CHIP8_MEMORY_SIZE]);
    }

    @Bean
    @Scope("prototype")
    public FontLoader fontLoader() {
        return new BasicFontLoader();
    }

    @Bean
    @Scope("prototype")
    public ROMLoader romLoader() {
        return new BasicROMLoader();
    }

    @Bean
    @Scope("prototype")
    public ProgramLoader programLoader(String programLocation) {
        return memory -> {
            fontLoader().load(memory, Chip8.FONT_OFFSET);
            romLoader().load(Path.of(programLocation), memory, Chip8.FIRST_INSTRUCTION_OFFSET);
        };
    }

    @Bean
    public Display windowDisplay(Stage stage) {
        return new WindowDisplay(
            new boolean[Chip8.DISPLAY_HEIGHT][Chip8.DISPLAY_WIDTH],
            stage,
            keyboard(),
            "Chip-8-Emulator"
        );
    }

    @Bean
    public Keyboard keyboard() {
        return new KeyboardProxy(KEY_REMAPPING);
    }

    @Bean
    @Scope("prototype")
    public Register normalRegister() {
        return new SimpleRegister(Chip8.NORMAL_REGISTER_BITS);
    }

    @Bean
    @Scope("prototype")
    public Register indexRegister() {
        return new SimpleRegister(Chip8.INDEX_REGISTER_BITS);
    }

    @Bean
    public Chip8 chip8(ProgramLoader loader, Stage stage) {
        Register[] registers = new Register[Chip8.NORMAL_REGISTERS_COUNT];

        for (int i = 0; i < registers.length; i++) {
            registers[i] = normalRegister();
        }

        return new Chip8(
            new Chip8Properties(
                simpleCounter(),
                reducingCounter(),
                reducingCounter(),
                clock(),
                memory(),
                loader,
                windowDisplay(stage),
                keyboard(),
                registers,
                indexRegister()
            )
        );
    }
}
