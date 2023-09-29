package bg.example.keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import java.util.HashSet;
import java.util.Set;

public class Keyboard implements KeyboardInformation {

    private Set<KeyCode> pressedKeys;

    public Keyboard() {
        pressedKeys = new HashSet<>();
    }

    @Override
    public boolean isPressed(KeyCode key) {
        return pressedKeys.contains(key);
    }

    public void press(KeyCode key) {
        pressedKeys.add(key);
    }

    public void release(KeyCode key) {
        pressedKeys.remove(key);
    }

    public void clear() {
        pressedKeys.clear();
    }
}
