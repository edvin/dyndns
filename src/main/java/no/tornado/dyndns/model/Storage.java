package no.tornado.dyndns.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class Storage {
	List<DnsRecord> records;

	public static void main(String[] args) {
//		Storage storage = new Storage();
//		storage.records = new ArrayList<>();
//		storage.records.add(new DnsRecord("x", "y", "z"));
//		storage.records.add(new DnsRecord("æ", "ø", "å"));
//		JAXB.marshal(storage, System.out);

		String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<storage>\n" +
			"    <records content=\"y\" name=\"x\" password=\"z\"/>\n" +
			"    <records content=\"ø\" name=\"æ\" password=\"å\"/>\n" +
			"</storage>";
		ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
		Storage storage = JAXB.unmarshal(input, Storage.class);

		System.out.println(storage.getRecords());

	}

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
