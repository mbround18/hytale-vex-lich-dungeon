package MBRound18.ImmortalEngine.runtime.offheap;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryUtil;

/**
 * Off-heap storage for a single run using LWJGL MemoryUtil.
 */
public final class RunNativeBuffers implements AutoCloseable {

  private final int maxPlayers;
  private final IntBuffer kills;
  private final IntBuffer deaths;
  private final FloatBuffer scores;
  private final LongBuffer lastEventTime;
  private final IntBuffer flags;

  public RunNativeBuffers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
    this.kills = MemoryUtil.memAllocInt(maxPlayers);
    this.deaths = MemoryUtil.memAllocInt(maxPlayers);
    this.scores = MemoryUtil.memAllocFloat(maxPlayers);
    this.lastEventTime = MemoryUtil.memAllocLong(maxPlayers);
    this.flags = MemoryUtil.memAllocInt(maxPlayers);
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setKill(int slot, int value) {
    kills.put(slot, value);
  }

  public int getKill(int slot) {
    return kills.get(slot);
  }

  public void setDeath(int slot, int value) {
    deaths.put(slot, value);
  }

  public int getDeath(int slot) {
    return deaths.get(slot);
  }

  public void setScore(int slot, float value) {
    scores.put(slot, value);
  }

  public float getScore(int slot) {
    return scores.get(slot);
  }

  public void setLastEventTime(int slot, long value) {
    lastEventTime.put(slot, value);
  }

  public long getLastEventTime(int slot) {
    return lastEventTime.get(slot);
  }

  public void setFlag(int slot, int value) {
    flags.put(slot, value);
  }

  public int getFlag(int slot) {
    return flags.get(slot);
  }

  @Override
  public void close() {
    MemoryUtil.memFree(kills);
    MemoryUtil.memFree(deaths);
    MemoryUtil.memFree(scores);
    MemoryUtil.memFree(lastEventTime);
    MemoryUtil.memFree(flags);
  }
}