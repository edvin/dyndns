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
			// ensure awt toolkit is initialized.
			java.awt.Toolkit.getDefaultToolkit();

			// set up a system tray icon.
			SystemTray tray = SystemTray.getSystemTray();
			URL imageLoc = getClass().getResource("/images/tornado_icon_gray.png");
			Image image = ImageIO.read(imageLoc);
			TrayIcon trayIcon = new TrayIcon(image);
			trayIcon.setImageAutoSize(true);

			// if the user double-clicks on the tray icon, show the main app stage.
			trayIcon.addActionListener(event -> javafx.application.Platform.runLater(this::showStage));

			// if the user selects the default menu item (which includes the app name),
			// show the main app stage.
			MenuItem openItem = new MenuItem("Open");
			openItem.addActionListener(event -> javafx.application.Platform.runLater(this::showStage));

			// the convention for tray icons seems to be to set the default icon for opening
			// the application stage in a bold font.
			Font defaultFont = Font.decode(null);
			Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
			openItem.setFont(boldFont);

			// to really exit the application, the user must go to the system tray icon
			// and select the exit option, this will shutdown JavaFX and remove the
			// tray icon (removing the tray icon will also shut down AWT).
			MenuItem exitItem = new MenuItem("Exit");
			exitItem.addActionListener(event -> {
				tray.remove(trayIcon);
				javafx.application.Platform.exit();
				System.exit(0);
			});

			// setup the popup menu for the application.
			final PopupMenu popup = new PopupMenu();
			popup.add(openItem);
			popup.addSeparator();
			popup.add(exitItem);
			trayIcon.setPopupMenu(popup);

			// add the application tray icon to the system tray.
			tray.add(trayIcon);
		} catch (AWTException | IOException e) {
			System.out.println("Unable to init system tray");
			e.printStackTrace();
		}
	}

	private void showStage() {
		if (stage != null) {
			stage.show();
			stage.toFront();
		}
	}
}
