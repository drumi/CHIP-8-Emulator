package bg.example.keyboard;

import javafx.scene.input.KeyCode;

import java.util.Map;


/**
 * Class used for remapping of keys
 */
public class KeyboardProxy extends Keyboard {

    private final Map<KeyCode, KeyCode> remappedKeys;

    public KeyboardProxy(Map<KeyCode, KeyCode> remappedKeys) {
        this.remappedKeys = remappedKeys;
    }

    @Override
    public void press(KeyCode key) {
        KeyCode newKey = remappedKeys.getOrDefault(key, key);
        super.press(newKey);
    }

    @Override
    public void release(KeyCode key) {
        KeyCode newKey = remappedKeys.getOrDefault(key, key);
        super.release(newKey);
    }
}
