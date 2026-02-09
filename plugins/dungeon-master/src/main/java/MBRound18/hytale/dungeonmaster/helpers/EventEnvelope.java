package MBRound18.hytale.dungeonmaster.helpers;

import com.google.gson.JsonElement;
import javax.annotation.Nonnull;

public record EventEnvelope(long id, long timestamp, @Nonnull String type, @Nonnull JsonElement payload) {
}
