package MBRound18.ImmortalEngine.api.events;

import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import javax.annotation.Nullable;

public final class EventDispatcher {
  private EventDispatcher() {
  }

  public static boolean dispatch(@Nullable EventBus eventBus, @Nullable IEvent<Void> event) {
    if (eventBus == null || event == null) {
      return false;
    }
    String previousCorrelation = null;
    boolean contextSet = false;
    try {
      if (event instanceof DebugEvent debugEvent) {
        previousCorrelation = CorrelationContext.get();
        CorrelationContext.set(debugEvent.getCorrelationId());
        contextSet = true;
      }
      @SuppressWarnings("unchecked")
      IEventDispatcher<IEvent<Void>, IEvent<Void>> dispatcher = (IEventDispatcher<IEvent<Void>, IEvent<Void>>) (IEventDispatcher<?, ?>) eventBus
          .dispatchFor((Class<? super IEvent<Void>>) event.getClass(), null);
      dispatcher.dispatch(event);
      return true;
    } catch (Exception ignored) {
      return false;
    } finally {
      if (contextSet) {
        if (previousCorrelation == null || previousCorrelation.isBlank()) {
          CorrelationContext.clear();
        } else {
          CorrelationContext.set(previousCorrelation);
        }
      }
    }
  }
}
