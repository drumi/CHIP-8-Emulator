package bg.example;

import bg.example.chip.Chip8;
import bg.example.config.ProjectConfig;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        String chipProgramLocation = getUserInput();
        System.out.println(chipProgramLocation);

        if (chipProgramLocation.isBlank()) {
            return;
        }

        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean("stage", Stage.class, () -> stage);
            ctx.registerBean("programLocation", String.class, () -> chipProgramLocation);
            ctx.register(ProjectConfig.class);
            ctx.refresh();

            Chip8 chip = ctx.getBean(Chip8.class, stage);

            var thread = new Thread(chip);

            thread.setDaemon(true);
            thread.start();
        }

        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();
    }

    private String getUserInput() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setContentText("Enter absolute path for chip8 program: ");
        dialog.setTitle("Chip 8 emulator");
        dialog.setHeaderText("");
        dialog.showAndWait();

        var result = dialog.getResult();

        return result == null ? "" : result.replace("\"", "");
    }
}
