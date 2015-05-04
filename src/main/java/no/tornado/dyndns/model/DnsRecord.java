package no.tornado.dyndns.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import no.tornado.MainApp;
import org.xbill.DNS.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.xbill.DNS.DClass.IN;

@XmlAccessorType(XmlAccessType.NONE)
public class DnsRecord {
	private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private StringProperty master;
	private StringProperty domain;
	private StringProperty hostname;
	private StringProperty content;
	private StringProperty password;
	private StringProperty status;
	private String soaServer;

	public DnsRecord() {
		master = new SimpleStringProperty();
		domain = new SimpleStringProperty();
		hostname = new SimpleStringProperty();
		content = new SimpleStringProperty();
		password = new SimpleStringProperty();
		status = new SimpleStringProperty();
	}

	public DnsRecord(String master, String domain, String hostname, String content, String password) {
		this.master = new SimpleStringProperty(master);
		this.domain = new SimpleStringProperty(domain);
		this.hostname = new SimpleStringProperty(hostname);
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
	public String getHostname() {
		return hostname.get();
	}

	public StringProperty hostnameProperty() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname.set(hostname);
	}

	@XmlAttribute
	public String getDomain() {
		return domain.get();
	}

	public StringProperty domainProperty() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain.set(domain);
	}

	@XmlAttribute
	public String getMaster() {
		return master.get();
	}

	public StringProperty masterProperty() {
		return master;
	}

	public void setMaster(String master) {
		this.master.set(master);
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
			"hostname=" + hostname.getValue() +
			", domain=" + domain.getValue() +
			", content=" + content.getValue() +
			", password=" + password.getValue() +
			'}';
	}

	public void update(InetAddress ip) {
		try {
			Name zone = Name.fromString(getDomain().concat("."));
			Name hostname = Name.fromString(getHostname().concat("."));

			Resolver resolver = new SimpleResolver(getMaster());
			resolver.setTSIGKey(new TSIG(zone.toString(true), getPassword()));
			resolver.setTCP(true);
			resolver.setTimeout(60);

			Update update = new Update(zone);
			update.delete(hostname, Type.A);
			update.add(new ARecord(hostname, IN, 300, ip));

			Message message = resolver.send(update);

			if (message.getRcode() == Rcode.NOERROR) {
				setContent(ip.toString().replace("/", ""));
				setStatus("OK");
			} else {
				setStatus("ERROR ".concat(Rcode.string(message.getRcode())));
			}
		} catch (Exception ex) {
			status.setValue("ERROR: " + ex.getMessage());
			MainApp.versionCheck();
		}
	}

	public static String getMasterFromSoa(String domain) throws TextParseException {
		Record[] result = new Lookup(Name.fromString(domain.concat(".")), Type.SOA).run();
		return result != null ? ((SOARecord) result[0]).getHost().toString(true) : null;
	}
}
