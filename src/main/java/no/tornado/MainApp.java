package no.tornado;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.DAYS;
import static javafx.scene.control.Alert.AlertType.ERROR;
import static javafx.scene.control.Alert.AlertType.WARNING;
import static javax.swing.SwingUtilities.invokeLater;

public class MainApp extends Application {
	public static final String Version = "2.0.1";
	private static Boolean showingVersionWarning = false;
	private static Logger log = Logger.getLogger(MainApp.class.getSimpleName());
	private Stage stage;
	private Timer timer;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
	    this.stage = stage;

	    FXMLLoader loader = new FXMLLoader();
        Parent rootNode = loader.load(getClass().getResourceAsStream("/fxml/MainWindow.fxml"));
	    invokeLater(this::addAppToTray);
        Scene scene = new Scene(rootNode, 800, 400);
        stage.setTitle("Tornado DynDNS Client 2.0.1");
        stage.setScene(scene);
	    Platform.setImplicitExit(false);
	    scheduleVersionCheck();
	    versionCheck();
	    stage.show();
    }

	private void scheduleVersionCheck() {
		timer = new Timer("VersionCheck", true);
		timer.schedule(new TimerTask() {
			public void run() {
				Platform.runLater(MainApp::versionCheck);
			}
		}, DAYS.toMillis(7), DAYS.toMillis(7));

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				timer.cancel();
			}
		});
	}

	public static void versionCheck() {
		try {
			if (showingVersionWarning)
				return;

			TXTRecord latest = (TXTRecord) new Lookup("latest.dyndns.tornado.no", Type.TXT).run()[0];
			TXTRecord earliest = (TXTRecord) new Lookup("earliest.dyndns.tornado.no", Type.TXT).run()[0];

			Platform.runLater(() -> {
				Hyperlink downloadLink = new Hyperlink("https://static.tornado.no/dyndns/");
				downloadLink.setOnMouseClicked(e -> {
					try {
						Desktop.getDesktop().browse(new URI(downloadLink.getText()));
					} catch (Exception ex) {
						log.log(Level.SEVERE, "Unable to browse to download page", ex);
					}
				});

				if (isOlderThan((String) earliest.getStrings().get(0))) {
					Alert alert = new Alert(ERROR, "", ButtonType.OK);
					alert.setTitle("Outdated software");
					alert.setHeaderText("This version of the DynDNS Client is too old. Please update immediately. Press OK to quit.");
					alert.getDialogPane().setContent(downloadLink);
					showingVersionWarning = true;
					alert.showAndWait();
					Platform.exit();
					System.exit(1);
				}

				if (isOlderThan((String) latest.getStrings().get(0))) {
					Alert alert = new Alert(WARNING, "", ButtonType.OK);
					alert.setTitle("Outdated software");
					alert.setHeaderText("This version of the DynDNS Client is outdated. Please update as soon as possible.");
					alert.getDialogPane().setContent(downloadLink);
					showingVersionWarning = true;
					alert.showAndWait();
					showingVersionWarning = false;
				}
			});

		} catch (Exception e) {
			log.log(Level.WARNING, "Version check failed", e);
		}
	}

	private static boolean isOlderThan(String remoteVersion) {
		DefaultArtifactVersion our = new DefaultArtifactVersion(Version);
		DefaultArtifactVersion remote = new DefaultArtifactVersion(remoteVersion);
		return our.compareTo(remote) == -1;
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
			log.log(Level.SEVERE, "Unable to init system tray", e);
		}
	}

	private void showStage() {
		if (stage != null) {
			stage.show();
			stage.toFront();
		}
	}
}
