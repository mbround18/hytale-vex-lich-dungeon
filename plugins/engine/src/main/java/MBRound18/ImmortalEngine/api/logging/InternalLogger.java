package MBRound18.ImmortalEngine.api.logging;

import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * Backwards-compatible alias for {@link EngineInternalLogger}.
 */
public class InternalLogger extends EngineInternalLogger {
  public InternalLogger(@Nonnull Path dataDirectory) {
    super(dataDirectory);
  }

  public InternalLogger(@Nonnull Path dataDirectory, @Nonnull String baseName) {
    super(dataDirectory, baseName);
  }
}
