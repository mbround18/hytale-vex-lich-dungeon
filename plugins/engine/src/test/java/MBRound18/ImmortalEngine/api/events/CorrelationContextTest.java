package MBRound18.ImmortalEngine.api.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CorrelationContextTest {
  private static final class TestEvent extends DebugEvent {
  }

  @Test
  public void debugEvent_inheritsCorrelationIdFromContext() {
    CorrelationContext.set("test-correlation");
    try {
      TestEvent event = new TestEvent();
      assertEquals("test-correlation", event.getCorrelationId());
    } finally {
      CorrelationContext.clear();
    }
  }

  @Test
  public void debugEvent_generatesCorrelationIdWhenMissing() {
    CorrelationContext.clear();
    TestEvent event = new TestEvent();
    assertNotNull(event.getCorrelationId());
  }
}
