package bg.example.chip;

import bg.example.clock.Clock;
import bg.example.counter.Counter;
import bg.example.display.Display;
import bg.example.keyboard.KeyboardInformation;
import bg.example.loader.program.ProgramLoader;
import bg.example.memory.Memory;
import bg.example.register.Register;

public record Chip8Properties(
    Counter programCounter,
    Counter delayCounter,
    Counter soundCounter,
    Clock clock,
    Memory memory,
    ProgramLoader programLoader,
    Display display,
    KeyboardInformation keyboardInformation,
    Register[] registers,
    Register indexRegister
) { }
