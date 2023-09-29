package bg.example.keyboard;

import javafx.scene.input.KeyCode;

public interface KeyboardInformation {

    boolean isPressed(KeyCode key);

    KeyCode getLastPressedKey();
}