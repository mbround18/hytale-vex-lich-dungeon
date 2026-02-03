package MBRound18.hytale.shared.interfaces.ui;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class UiTemplate {
  private final @Nonnull String id;
  private final @Nonnull String rawPath;
  private final @Nonnull String clientPath;
  private final @Nonnull List<String> vars;

  public UiTemplate(@Nonnull String id, @Nonnull String path, @Nonnull List<String> vars) {
    this.id = Objects.requireNonNull(id, "id");
    this.rawPath = Objects.requireNonNull(path, "path");
    String normalized = UiPath.normalizeForClient(this.rawPath);
    this.clientPath = normalized != null ? normalized : this.rawPath;
    this.vars = Objects.requireNonNull(vars, "vars");
  }

  @Nonnull
  public String getId() {
    return Objects.requireNonNull(id, "id");
  }

  @Nonnull
  public String getPath() {
    return Objects.requireNonNull(clientPath, "path");
  }

  @Nonnull
  public String getRawPath() {
    return Objects.requireNonNull(rawPath, "rawPath");
  }

  @Nonnull
  public List<String> getVars() {
    return Objects.requireNonNull(vars, "vars");
  }
}
