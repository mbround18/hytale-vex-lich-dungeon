package MBRound18.ImmortalEngine.api.ui;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public final class UiTemplate {
  private final String id;
  private final String path;
  private final List<String> vars;

  public UiTemplate(@Nonnull String id, @Nonnull String path, @Nonnull List<String> vars) {
    this.id = id;
    this.path = path;
    this.vars = List.copyOf(vars);
  }

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public List<String> getVars() {
    return Collections.unmodifiableList(vars);
  }
}
