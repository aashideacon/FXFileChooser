package net.raumzeitfalle.fx;

import java.nio.file.Path;
import java.util.Optional;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import net.raumzeitfalle.fx.filechooser.FXFileChooserDialog;
import net.raumzeitfalle.fx.filechooser.Skin;

public class DemoJavaFxDialog extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXFileChooserDialog fc = FXFileChooserDialog.create(Skin.DARK);

		Button showDialog = new Button("Show JavaFX Dialog (FXFileChooserDialog.class)");
		showDialog.setOnAction(a -> {

			Optional<Path> path = fc.showOpenDialog(primaryStage);
			System.out.println(path.map(String::valueOf).orElse("Nothing selected"));

		});

		Scene scene = new Scene(showDialog);
		primaryStage.setScene(scene);
		primaryStage.setWidth(400);
		primaryStage.setHeight(400);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}
