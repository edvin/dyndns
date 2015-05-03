package no.tornado.dyndns.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.xbill.DNS.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static org.xbill.DNS.DClass.IN;

@XmlAccessorType(XmlAccessType.NONE)
public class DnsRecord {
	private static final DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
	private StringProperty name;
	private StringProperty content;
	private StringProperty password;
	private StringProperty status;

	public DnsRecord() {
		name = new SimpleStringProperty();
		content = new SimpleStringProperty();
		password = new SimpleStringProperty();
		status = new SimpleStringProperty();
	}

	public DnsRecord(String name, String content, String password) {
		this.name = new SimpleStringProperty(name);
		this.content = new SimpleStringProperty(content);
		this.password = new SimpleStringProperty(password);
		this.status = new SimpleStringProperty("PENDING");
	}

	@XmlAttribute
	public String getPassword() {
		return password.get();
	}

	public StringProperty passwordProperty() {
		return password;
	}

	public void setPassword(String password) {
		this.password.set(password);
	}

	@XmlAttribute
	public String getContent() {
		return content.get();
	}

	public StringProperty contentProperty() {
		return content;
	}

	public void setContent(String content) {
		this.content.set(content);
	}

	@XmlAttribute
	public String getName() {
		return name.get();
	}

	public StringProperty nameProperty() {
		return name;
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public String getStatus() {
		return status.get();
	}

	public StringProperty statusProperty() {
		return status;
	}

	public void setStatus(String status) {
		this.status.set(LocalDateTime.now().format(fmt).concat(" ").concat(status));
	}

	public String toString() {
		return "DnsRecord{" +
			"name=" + name.getValue() +
			", content=" + content.getValue() +
			", password=" + password.getValue() +
			'}';
	}

	public void update(InetAddress ip) {
		try {
			Name name = Name.fromString(getName().concat("."));
			Name zone = getName().indexOf(".") == getName().lastIndexOf(".") ? name : Name.fromString(getName().substring(getName().indexOf(".") + 1).concat("."));

			Resolver resolver = new SimpleResolver("ns.syse.no");
			resolver.setTSIGKey(new TSIG(zone.toString(true), getPassword()));
			resolver.setTCP(true);
			resolver.setTimeout(60);

			Update update = new Update(zone);
			update.add(new ARecord(name, IN, 10800, ip));

			Message message = resolver.send(update);
			if (message.getRcode() == Rcode.NOERROR) {
				setContent(ip.toString().replace("/", ""));
				setStatus("OK");
			} else {
				setStatus("ERROR ".concat(Rcode.string(message.getRcode())));
			}
		} catch (Exception ex) {
			status.setValue("ERROR: " + ex.getMessage());
		}
	}
}
