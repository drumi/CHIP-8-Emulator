package bg.example.ROM;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class BasicROMLoader implements ROMLoader {

    @Override
    public void load(Path path, int[] memory, int offset) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            byte[] readBytes = in.readAllBytes();

            for (int i = 0; i < readBytes.length; i++) {
                memory[i + offset] = readBytes[i] & 0xFF;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
