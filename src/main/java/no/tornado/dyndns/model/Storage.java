package no.tornado.dyndns.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@XmlRootElement
public class Storage {
	List<DnsRecord> records;

	public List<DnsRecord> getRecords() {
		return records;
	}

	public void setRecords(List<DnsRecord> records) {
		this.records = records;
	}

	public static ObservableList<DnsRecord> loadRecords() {
		try {
			Path path = getPath();
			if (Files.exists(path)) {
				Storage storage = JAXB.unmarshal(path.toFile(), Storage.class);
				return FXCollections.observableList(storage.getRecords());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return FXCollections.observableArrayList();
	}

	public static void saveRecords(ObservableList<DnsRecord> records) {
		Path path = getPath();
		Storage storage = new Storage();
		storage.setRecords(records);
		JAXB.marshal(storage, path.toFile());
	}

	private static Path getPath() {
		return Paths.get(System.getProperty("user.home")).resolve(".dyndns.records");
	}
}
