package MBRound18.hytale.vexlichdungeon.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class TestEventHub {
  private final Map<Class<?>, List<Consumer<?>>> handlers = new ConcurrentHashMap<>();

  public <EventType> void register(Class<? super EventType> eventClass, Consumer<EventType> consumer) {
    handlers.computeIfAbsent(eventClass, key -> new ArrayList<>()).add(consumer);
  }

  public void dispatch(Object event) {
    if (event == null) {
      return;
    }
    Class<?> eventClass = event.getClass();
    handlers.forEach((registered, consumers) -> {
      if (registered.isAssignableFrom(eventClass)) {
        for (Consumer<?> consumer : consumers) {
          @SuppressWarnings("unchecked")
          Consumer<Object> casted = (Consumer<Object>) consumer;
          casted.accept(event);
        }
      }
    });
  }
}
