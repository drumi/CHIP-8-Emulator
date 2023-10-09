package bg.example.loader.rom;

import bg.example.memory.Memory;

import java.nio.file.Path;

@FunctionalInterface
public interface ROMLoader {

    void load(Path path, Memory memory, int offset);

}
