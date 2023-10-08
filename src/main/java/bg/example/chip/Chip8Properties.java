package bg.example.chip;

import bg.example.clock.Clock;
import bg.example.counter.Counter;
import bg.example.display.Display;
import bg.example.font.FontLoader;
import bg.example.keyboard.KeyboardInformation;
import bg.example.memory.Memory;
import bg.example.register.Register;
import bg.example.rom.ROMLoader;

public record Chip8Properties(
    Counter programCounter,
    Counter delayCounter,
    Counter soundCounter,
    Clock clock,
    Memory memory,
    FontLoader fontLoader,
    ROMLoader romLoader,
    Display display,
    KeyboardInformation keyboardInformation,
    Register[] registers,
    Register indexRegister
) { }
