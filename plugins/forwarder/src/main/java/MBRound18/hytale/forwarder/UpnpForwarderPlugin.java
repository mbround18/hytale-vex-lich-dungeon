package MBRound18.hytale.forwarder;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Attempts to set up UPnP port forwarding for UDP port 5520.
 */
public class UpnpForwarderPlugin extends JavaPlugin {
  private static final int PORT = 5520;
  private static final int SSDP_PORT = 1900;
  private static final String SSDP_ADDRESS = "239.255.255.250";
  private static final int SSDP_TIMEOUT_MS = 2500;
  private static final int MAX_ATTEMPTS = 4;
  private static final String SERVICE_WAN_IP = "urn:schemas-upnp-org:service:WANIPConnection:1";
  private static final String SERVICE_WAN_PPP = "urn:schemas-upnp-org:service:WANPPPConnection:1";
  private static final List<String> DEFAULT_GATEWAYS = List.of(
      "192.168.1.1",
      "192.168.0.1",
      "192.168.1.254",
      "192.168.0.254",
      "10.0.0.1",
      "10.0.1.1",
      "10.0.0.138",
      "172.16.0.1");
  private static final List<String> DEFAULT_DESC_PATHS = List.of(
      "/rootDesc.xml",
      "/igd.xml",
      "/device.xml",
      "/desc.xml",
      "/upnp/igd.xml");

  private final HytaleLogger log;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private Path dataDirectory;
  private Path configPath;
  private UpnpForwarderConfig config;
  private Thread worker;
  private volatile GatewayService lastService;
  private volatile String lastInternalClient;
  private volatile boolean mappingActive;

  public UpnpForwarderPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    this.log = getLogger().getSubLogger("UpnpForwarder");
  }

  @Override
  protected void setup() {
    Path pluginJarPath = getFile().toAbsolutePath();
    Path modsDirectory = pluginJarPath.getParent();
    if (modsDirectory == null) {
      log.atWarning().log("[UPNP] Cannot resolve mods directory; config disabled.");
      return;
    }
    dataDirectory = modsDirectory.resolve("UpnpForwarder");
    configPath = dataDirectory.resolve("config.json");
    loadConfig();
  }

  @Override
  protected void start() {
    worker = new Thread(this::forwardPort, "upnp-forwarder");
    worker.setDaemon(true);
    worker.start();
  }

  @Override
  protected void shutdown() {
    if (worker != null) {
      worker.interrupt();
    }
    removePortMapping();
  }

  private void forwardPort() {
    try {
      log.atInfo().log(String.format("[UPNP] Starting UPnP port forward attempt for UDP %d", PORT));
      for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        if (tryForwardOnce()) {
          return;
        }
        log.atWarning().log(String.format("[UPNP] Attempt %d/%d failed.", attempt, MAX_ATTEMPTS));
      }
      log.atWarning().log("[UPNP] Unable to set up UPnP port forwarding after multiple attempts.");
      log.atWarning().log(String.format(
          "[UPNP] Edit %s with your router IP/URL and restart if needed.",
          configPath != null ? configPath : "config.json"));
    } catch (Exception e) {
      log.atWarning().log(String.format("[UPNP] Port forward attempt failed: %s", e.getMessage()));
    }
  }

  private boolean tryForwardOnce() {
    String location = discoverGatewayLocation();
    if (location == null) {
      location = probeGatewayHints();
    }
    if (location == null) {
      return false;
    }

    GatewayService service = fetchGatewayService(location);
    if (service == null) {
      log.atWarning().log(String.format("[UPNP] No compatible WAN service found at %s", location));
      return false;
    }

    InetAddress localAddress = resolveLocalAddress(service.controlURL);
    if (localAddress == null) {
      log.atWarning().log("[UPNP] Failed to resolve local address for port mapping.");
      return false;
    }

    boolean success = sendAddPortMapping(service, localAddress.getHostAddress());
    if (success) {
      lastService = service;
      lastInternalClient = localAddress.getHostAddress();
      mappingActive = true;
      log.atInfo().log(String.format("[UPNP] Port mapping active: UDP %d -> %s:%d",
          PORT, localAddress.getHostAddress(), PORT));
      return true;
    }
    log.atWarning().log("[UPNP] Port mapping request failed.");
    return false;
  }

  private String discoverGatewayLocation() {
    String request = "M-SEARCH * HTTP/1.1\r\n"
        + "HOST: " + SSDP_ADDRESS + ":" + SSDP_PORT + "\r\n"
        + "MAN: \"ssdp:discover\"\r\n"
        + "MX: 2\r\n"
        + "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n"
        + "\r\n";
    byte[] payload = request.getBytes(StandardCharsets.UTF_8);

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(SSDP_TIMEOUT_MS);
      DatagramPacket packet = new DatagramPacket(
          payload,
          payload.length,
          InetAddress.getByName(SSDP_ADDRESS),
          SSDP_PORT);
      socket.send(packet);

      long deadline = System.currentTimeMillis() + SSDP_TIMEOUT_MS;
      while (System.currentTimeMillis() < deadline) {
        byte[] buf = new byte[2048];
        DatagramPacket response = new DatagramPacket(buf, buf.length);
        socket.receive(response);
        String data = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
        String location = parseHeader(data, "location");
        if (location != null) {
          return location.trim();
        }
      }
    } catch (Exception e) {
      if (!isTimeout(e)) {
        log.atWarning().log(String.format("[UPNP] SSDP discovery failed: %s", e.getMessage()));
      }
    }
    return null;
  }

  private GatewayService fetchGatewayService(String locationUrl) {
    try {
      URL url = new URL(locationUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(2000);
      connection.setReadTimeout(3000);
      connection.setRequestMethod("GET");
      connection.connect();

      if (connection.getResponseCode() != 200) {
        return null;
      }

      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
      doc.getDocumentElement().normalize();

      String base = extractUrlBase(doc, url);
      GatewayService service = findService(doc, SERVICE_WAN_IP, base);
      if (service != null) {
        return service;
      }
      return findService(doc, SERVICE_WAN_PPP, base);
    } catch (Exception e) {
      if (!isTimeout(e)) {
        log.atWarning().log(String.format("[UPNP] Failed to parse gateway description: %s", e.getMessage()));
      }
      return null;
    }
  }

  private GatewayService findService(Document doc, String serviceType, String base) {
    NodeList services = doc.getElementsByTagName("service");
    for (int i = 0; i < services.getLength(); i++) {
      Element service = (Element) services.item(i);
      String type = textOf(service, "serviceType");
      if (type == null || !type.equals(serviceType)) {
        continue;
      }
      String controlUrl = textOf(service, "controlURL");
      if (controlUrl == null) {
        continue;
      }
      String resolved = controlUrl.startsWith("http") ? controlUrl : base + controlUrl;
      return new GatewayService(serviceType, resolved);
    }
    return null;
  }

  private String probeGatewayHints() {
    List<String> hints = new ArrayList<>();
    if (config != null && config.gatewayHints != null) {
      hints.addAll(config.gatewayHints);
    }
    for (String fallback : DEFAULT_GATEWAYS) {
      if (!hints.contains(fallback)) {
        hints.add(fallback);
      }
    }

    for (String hint : hints) {
      if (hint == null || hint.isBlank()) {
        continue;
      }
      String trimmed = hint.trim();
      if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        if (fetchGatewayService(trimmed) != null) {
          rememberGateway(trimmed);
          return trimmed;
        }
        continue;
      }

      for (String path : DEFAULT_DESC_PATHS) {
        String candidate = "http://" + trimmed + ":80" + path;
        if (fetchGatewayService(candidate) != null) {
          rememberGateway(candidate);
          return candidate;
        }
        String alt = "http://" + trimmed + ":1900" + path;
        if (fetchGatewayService(alt) != null) {
          rememberGateway(alt);
          return alt;
        }
      }
    }
    return null;
  }

  private void rememberGateway(String location) {
    if (config == null || location == null) {
      return;
    }
    config.lastGateway = location;
    if (config.gatewayHints == null) {
      config.gatewayHints = new ArrayList<>();
    }
    if (!config.gatewayHints.contains(location)) {
      config.gatewayHints.add(location);
    }
    saveConfig();
  }

  private String extractUrlBase(Document doc, URL fallback) {
    NodeList nodes = doc.getElementsByTagName("URLBase");
    if (nodes.getLength() > 0) {
      String base = nodes.item(0).getTextContent();
      if (base != null && !base.isBlank()) {
        return base.trim();
      }
    }
    return fallback.getProtocol() + "://" + fallback.getHost() + ":" + fallback.getPort();
  }

  private String textOf(Element parent, String tagName) {
    NodeList nodes = parent.getElementsByTagName(tagName);
    if (nodes.getLength() == 0) {
      return null;
    }
    return nodes.item(0).getTextContent();
  }

  private InetAddress resolveLocalAddress(String controlUrl) {
    try {
      URI uri = URI.create(controlUrl);
      try (DatagramSocket socket = new DatagramSocket()) {
        socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 80));
        return socket.getLocalAddress();
      }
    } catch (Exception e) {
      return null;
    }
  }

  private boolean sendAddPortMapping(GatewayService service, String internalClient) {
    String soap = buildSoapBody(service.serviceType, internalClient);
    try {
      URL url = new URL(service.controlURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(2000);
      connection.setReadTimeout(3000);
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
      connection.setRequestProperty("SOAPAction", "\"" + service.serviceType + "#AddPortMapping\"");
      byte[] payload = soap.getBytes(StandardCharsets.UTF_8);
      connection.setFixedLengthStreamingMode(payload.length);
      try (OutputStream out = connection.getOutputStream()) {
        out.write(payload);
      }
      int code = connection.getResponseCode();
      if (code == 200) {
        return true;
      }
      String response = readErrorResponse(connection);
      log.atWarning().log(String.format("[UPNP] AddPortMapping response %d: %s", code, response));
    } catch (Exception e) {
      if (!isTimeout(e)) {
        log.atWarning().log(String.format("[UPNP] AddPortMapping request failed: %s", e.getMessage()));
      }
    }
    return false;
  }

  private void removePortMapping() {
    GatewayService service = lastService;
    String internalClient = lastInternalClient;
    if (!mappingActive || service == null || internalClient == null) {
      return;
    }
    boolean removed = sendDeletePortMapping(service);
    if (removed) {
      log.atInfo().log(String.format("[UPNP] Removed port mapping for UDP %d", PORT));
    } else {
      log.atWarning().log("[UPNP] Failed to remove port mapping on shutdown.");
    }
    mappingActive = false;
  }

  private boolean sendDeletePortMapping(GatewayService service) {
    String soap = buildDeleteSoapBody(service.serviceType);
    try {
      URL url = new URL(service.controlURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(2000);
      connection.setReadTimeout(3000);
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
      connection.setRequestProperty("SOAPAction", "\"" + service.serviceType + "#DeletePortMapping\"");
      byte[] payload = soap.getBytes(StandardCharsets.UTF_8);
      connection.setFixedLengthStreamingMode(payload.length);
      try (OutputStream out = connection.getOutputStream()) {
        out.write(payload);
      }
      int code = connection.getResponseCode();
      if (code == 200) {
        return true;
      }
      String response = readErrorResponse(connection);
      log.atWarning().log(String.format("[UPNP] DeletePortMapping response %d: %s", code, response));
    } catch (Exception e) {
      if (!isTimeout(e)) {
        log.atWarning().log(String.format("[UPNP] DeletePortMapping request failed: %s", e.getMessage()));
      }
    }
    return false;
  }

  private String buildSoapBody(String serviceType, String internalClient) {
    return "<?xml version=\"1.0\"?>"
        + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "<s:Body>"
        + "<u:AddPortMapping xmlns:u=\"" + serviceType + "\">"
        + "<NewRemoteHost></NewRemoteHost>"
        + "<NewExternalPort>" + PORT + "</NewExternalPort>"
        + "<NewProtocol>UDP</NewProtocol>"
        + "<NewInternalPort>" + PORT + "</NewInternalPort>"
        + "<NewInternalClient>" + internalClient + "</NewInternalClient>"
        + "<NewEnabled>1</NewEnabled>"
        + "<NewPortMappingDescription>Hytale UPnP Forwarder</NewPortMappingDescription>"
        + "<NewLeaseDuration>0</NewLeaseDuration>"
        + "</u:AddPortMapping>"
        + "</s:Body>"
        + "</s:Envelope>";
  }

  private String buildDeleteSoapBody(String serviceType) {
    return "<?xml version=\"1.0\"?>"
        + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "<s:Body>"
        + "<u:DeletePortMapping xmlns:u=\"" + serviceType + "\">"
        + "<NewRemoteHost></NewRemoteHost>"
        + "<NewExternalPort>" + PORT + "</NewExternalPort>"
        + "<NewProtocol>UDP</NewProtocol>"
        + "</u:DeletePortMapping>"
        + "</s:Body>"
        + "</s:Envelope>";
  }

  private String readErrorResponse(HttpURLConnection connection) {
    if (connection.getErrorStream() == null) {
      return "no response";
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
      String line;
      StringBuilder builder = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
      return builder.toString();
    } catch (Exception e) {
      return "no response";
    }
  }

  private String parseHeader(String raw, String name) {
    String[] lines = raw.split("\r\n");
    for (String line : lines) {
      int idx = line.indexOf(':');
      if (idx <= 0) {
        continue;
      }
      String key = line.substring(0, idx).trim();
      if (key.equalsIgnoreCase(name)) {
        return line.substring(idx + 1).trim();
      }
    }
    return null;
  }

  private boolean isTimeout(Exception e) {
    if (e == null || e.getMessage() == null) {
      return false;
    }
    String message = e.getMessage().toLowerCase();
    return message.contains("timed out") || message.contains("timeout");
  }

  private void loadConfig() {
    try {
      if (dataDirectory == null || configPath == null) {
        return;
      }
      if (!Files.exists(dataDirectory)) {
        Files.createDirectories(dataDirectory);
      }
      if (Files.exists(configPath)) {
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
          UpnpForwarderConfig loaded = gson.fromJson(reader, UpnpForwarderConfig.class);
          config = loaded != null ? loaded : UpnpForwarderConfig.defaults();
        }
      } else {
        config = UpnpForwarderConfig.defaults();
      }
      if (config.gatewayHints == null || config.gatewayHints.isEmpty()) {
        config.gatewayHints = new ArrayList<>(DEFAULT_GATEWAYS);
      }
      saveConfig();
    } catch (Exception e) {
      log.atWarning().log(String.format("[UPNP] Failed to load config: %s", e.getMessage()));
      config = UpnpForwarderConfig.defaults();
    }
  }

  private void saveConfig() {
    if (configPath == null || config == null) {
      return;
    }
    try {
      Files.createDirectories(configPath.getParent());
      Files.writeString(configPath, gson.toJson(config), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.atWarning().log(String.format("[UPNP] Failed to save config: %s", e.getMessage()));
    }
  }

  private static final class GatewayService {
    private final String serviceType;
    private final String controlURL;

    private GatewayService(String serviceType, String controlURL) {
      this.serviceType = serviceType;
      this.controlURL = controlURL;
    }
  }

  private static final class UpnpForwarderConfig {
    private List<String> gatewayHints;
    private String lastGateway;

    private static UpnpForwarderConfig defaults() {
      UpnpForwarderConfig config = new UpnpForwarderConfig();
      config.gatewayHints = new ArrayList<>(DEFAULT_GATEWAYS);
      return config;
    }
  }
}
