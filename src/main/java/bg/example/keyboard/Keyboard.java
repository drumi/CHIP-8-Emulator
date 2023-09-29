package bg.example.keyboard;

import javafx.scene.input.KeyCode;

import java.util.HashSet;
import java.util.Set;

public class Keyboard implements KeyboardInformation {

    private Set<KeyCode> pressedKeys;
    private KeyCode lastPressed;

    public Keyboard() {
        pressedKeys = new HashSet<>();
    }

    @Override
    public boolean isPressed(KeyCode key) {
        return pressedKeys.contains(key);
    }

    @Override
    public KeyCode getLastPressedKey() {
        return lastPressed;
    }

    public void press(KeyCode key) {
        pressedKeys.add(key);
        lastPressed = key;
    }

    public void release(KeyCode key) {
        pressedKeys.remove(key);
    }

    public void clear() {
        pressedKeys.clear();
    }
}
