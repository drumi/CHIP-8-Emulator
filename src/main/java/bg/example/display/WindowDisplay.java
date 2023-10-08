package bg.example.display;

import bg.example.keyboard.Keyboard;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class WindowDisplay extends BaseDisplay {

    private static final int PIXEL_HEIGHT = 10;
    private static final int PIXEL_WIDTH = 10;

    private static final int BORDER_SPACE_PIXEL_SIZE = 4;

    private final int windowHeight;
    private final int windowWidth;
    private final String title;

    private final Stage stage;
    private final Keyboard keyboard;

    public WindowDisplay(boolean[][] pixels, Stage stage, Keyboard keyboard, String title) {
        super(pixels);
        this.stage = stage;
        this.keyboard = keyboard;
        this.title = title;

        windowHeight = (pixels.length  + BORDER_SPACE_PIXEL_SIZE) * PIXEL_HEIGHT;
        windowWidth = (pixels[0].length + BORDER_SPACE_PIXEL_SIZE) * PIXEL_WIDTH;

        stage.setHeight(windowHeight);
        stage.setWidth(windowWidth);
        stage.setTitle(title);
        stage.resizableProperty().setValue(Boolean.FALSE);
    }

    @Override
    public void update() {

        Group group = new Group();
        Scene scene = new Scene(group);

        scene.setOnKeyPressed(
            ke -> keyboard.press(ke.getCode())
        );

        scene.setOnKeyReleased(
            ke -> keyboard.release(ke.getCode())
        );

        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[0].length; x++) {
                if (pixels[y][x]) {
                    Rectangle rectangle = new Rectangle();
                    rectangle.setWidth(PIXEL_WIDTH);
                    rectangle.setHeight(PIXEL_HEIGHT);

                    rectangle.setX(x * PIXEL_WIDTH);
                    rectangle.setY(y * PIXEL_HEIGHT);

                    rectangle.setFill(Color.BLACK);
                    group.getChildren().add(rectangle);
                }
            }
        }

        Platform.runLater(
            () -> {
                stage.setScene(scene);
                stage.show();
            }
        );
    }
}
