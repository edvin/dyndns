package no.tornado.dyndns.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import no.tornado.dyndns.model.DnsRecord;
import no.tornado.dyndns.model.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static javafx.scene.control.ButtonType.APPLY;
import static javafx.scene.control.ButtonType.OK;

public class MainController extends TimerTask implements Initializable {
	@FXML TableView<DnsRecord> recordsView;
	@FXML TableColumn<DnsRecord, String> hostnameCol;
	@FXML TableColumn<DnsRecord, String> contentCol;
	@FXML TableColumn<DnsRecord, String> statusCol;
	@FXML Button deleteRecord;
	@FXML Button editRecord;
	ObservableList<DnsRecord> data;
	Timer timer;

	public void initialize(URL location, ResourceBundle resources) {
		hostnameCol.setCellValueFactory(new PropertyValueFactory<>("hostname"));
		statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
		contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
		deleteRecord.disableProperty().bind(Bindings.isEmpty(recordsView.getSelectionModel().getSelectedItems()));
		editRecord.disableProperty().bind(Bindings.isEmpty(recordsView.getSelectionModel().getSelectedItems()));
		recordsView.setOnMouseClicked(e -> {
			if (e.getClickCount() > 1 && !recordsView.getSelectionModel().isEmpty())
				try {
					onEditRecord();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		});
		loadRecords();
		timer = new Timer("DynDNS Updater", true);
		timer.schedule(this, 0, TimeUnit.MINUTES.toMillis(5));
	}

	public void loadRecords() {
		data = Storage.loadRecords();
		recordsView.setItems(data);
	}

	public void onEditRecord() throws IOException {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Edit DynDNS Record");
		alert.setHeaderText("Update record");
		GridPane ui = new FXMLLoader().load(getClass().getResourceAsStream("/fxml/EditRecord.fxml"));

		DnsRecord record = recordsView.getSelectionModel().getSelectedItem();
		TextField domain = (TextField) ui.lookup("#domain");
		TextField hostname = (TextField) ui.lookup("#hostname");
		TextField password = (TextField) ui.lookup("#password");
		domain.setText(record.getDomain());
		hostname.setText(record.getHostname());
		password.setText(record.getPassword());

		alert.getButtonTypes().clear();
		alert.getButtonTypes().add(ButtonType.CANCEL);
		alert.getButtonTypes().add(ButtonType.APPLY);

		alert.getDialogPane().setContent(ui);

		Optional<ButtonType> pressed = alert.showAndWait();
		if (pressed.filter(btn -> btn == APPLY).isPresent()) {
			record.setDomain(domain.getText());
			record.setHostname(hostname.getText());
			record.setPassword(password.getText());
			record.setContent(null);
			String master = DnsRecord.getMasterFromSoa(domain.getText());
			if (master == null) {
				Alert error = new Alert(Alert.AlertType.ERROR);
				error.setTitle("Non existent SOA record");
				error.setHeaderText("Unable to extract master from SOA record, please check domain");
				error.show();
				return;
			} else {
				record.setMaster(DnsRecord.getMasterFromSoa(domain.getText()));
			}
			Storage.saveRecords(data);
			onRefresh();
		}
	}

	public void onAddRecord() throws IOException {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Add new DynDNS Record");
		alert.setHeaderText("Supply name and password for the record to add");
		GridPane ui = new FXMLLoader().load(getClass().getResourceAsStream("/fxml/EditRecord.fxml"));

		alert.getDialogPane().setContent(ui);

		TextField domain = (TextField) ui.lookup("#domain");
		TextField hostname = (TextField) ui.lookup("#hostname");
		TextField password = (TextField) ui.lookup("#password");

		Platform.runLater(domain::requestFocus);

		Optional<ButtonType> pressed = alert.showAndWait();
		if (pressed.filter(btn -> btn == OK).isPresent()) {
			String master = DnsRecord.getMasterFromSoa(domain.getText());
			if (master == null) {
				Alert error = new Alert(Alert.AlertType.ERROR);
				error.setTitle("Non existent SOA record");
				error.setHeaderText("Unable to extract master from SOA record, please check domain");
				error.show();
				return;
			}
			data.add(new DnsRecord(master, domain.getText(), hostname.getText(), "", password.getText()));
			Storage.saveRecords(data);
			onRefresh();
		}
	}

	public void onRefresh() {
		new Thread(this).start();
	}

	public void onDeleteRecord() {
		DnsRecord record = recordsView.getSelectionModel().getSelectedItem();
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Confirm delete");
		alert.setHeaderText("Confirm delete of ".concat(record.getHostname()));
		if (alert.showAndWait().get() == OK) {
			data.remove(record);
			Storage.saveRecords(data);
		}
	}

	public void run() {
		if (data.isEmpty())
			return;

		URL url;
		try { url = new URL("http://api.ipify.org"); } catch (MalformedURLException e) { throw new RuntimeException(e); }

		try (InputStream input = url.openStream();
		     BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

			String ip = reader.readLine();

			// Verify validity
			InetAddress address = InetAddress.getByName(ip);

			for (DnsRecord record : data) {
				if (ip.equals(record.getContent()))
					record.setStatus("OK");
				else
					record.update(address);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
