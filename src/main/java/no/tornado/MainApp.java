package no.tornado;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.swing.SwingUtilities.invokeLater;

public class MainApp extends Application {
	private Stage stage;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
	    this.stage = stage;
        FXMLLoader loader = new FXMLLoader();
        Parent rootNode = loader.load(getClass().getResourceAsStream("/fxml/MainWindow.fxml"));
	    invokeLater(this::addAppToTray);
        Scene scene = new Scene(rootNode, 800, 400);
        stage.setTitle("Tornado DynDNS Client 2.0");
        stage.setScene(scene);
	    Platform.setImplicitExit(false);
        stage.show();
    }

	/**
	 * Sets up a system tray icon for the application.
	 */
	private void addAppToTray() {
		try {
			// ensure AWT toolkit is initialized
			Toolkit.getDefaultToolkit();

			SystemTray tray = SystemTray.getSystemTray();
			URL imageLoc = getClass().getResource("/images/tornado_icon_gray.png");
			Image image = ImageIO.read(imageLoc);
			TrayIcon trayIcon = new TrayIcon(image);
			trayIcon.setImageAutoSize(true);

			trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

			MenuItem openItem = new MenuItem("Open");
			openItem.addActionListener(event -> Platform.runLater(this::showStage));

			Font defaultFont = Font.decode(null);
			Font boldFont = defaultFont.deriveFont(Font.BOLD);
			openItem.setFont(boldFont);

			MenuItem exitItem = new MenuItem("Exit");
			exitItem.addActionListener(event -> {
				tray.remove(trayIcon);
				Platform.exit();
				System.exit(0);
			});

			PopupMenu popup = new PopupMenu();
			popup.add(openItem);
			popup.addSeparator();
			popup.add(exitItem);
			trayIcon.setPopupMenu(popup);

			tray.add(trayIcon);
		} catch (AWTException | IOException e) {
			Logger.getLogger(getClass().getSimpleName()).log(Level.SEVERE, "Unable to init system tray", e);
		}
	}

	private void showStage() {
		if (stage != null) {
			stage.show();
			stage.toFront();
		}
	}
}
