package bg.example.ROM;

import java.nio.file.Path;

@FunctionalInterface
public interface ROMLoader {

    void load(Path path, int[] memory, int offset);

}
