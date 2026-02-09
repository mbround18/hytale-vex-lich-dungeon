package MBRound18.hytale.vexlichdungeon.data;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class ArchiveRecord implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String id;
  private final String timestamp;
  private final String dataJson;

  public ArchiveRecord(@Nonnull String id, @Nonnull String timestamp, @Nonnull String dataJson) {
    this.id = Objects.requireNonNull(id, "id");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    this.dataJson = Objects.requireNonNull(dataJson, "dataJson");
  }

  public String getId() {
    return id;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getDataJson() {
    return dataJson;
  }
}
