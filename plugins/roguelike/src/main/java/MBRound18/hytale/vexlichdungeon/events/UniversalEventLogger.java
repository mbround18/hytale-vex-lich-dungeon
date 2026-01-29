package MBRound18.hytale.vexlichdungeon.events;

import MBRound18.ImmortalEngine.api.logging.EngineLog;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.IBaseEvent;
import java.lang.reflect.Method;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import javax.annotation.Nonnull;

/**
 * Temporary universal event logger to debug which events fire during instance
 * creation/player join.
 */
public class UniversalEventLogger {

    private final EngineLog log;

    public UniversalEventLogger(@Nonnull EngineLog log) {
        this.log = log;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void register(@Nonnull EventBus eventBus) {
        // Log PlayerConnectEvent
        eventBus.register(
                (Class) PlayerConnectEvent.class,
                (java.util.function.Consumer) (Object e) -> log.info("🔴 EVENT: %s", describeEvent(e)));

        // Log PlayerDisconnectEvent
        eventBus.register(
                (Class) PlayerDisconnectEvent.class,
                (java.util.function.Consumer) (Object e) -> log.info("🔴 EVENT: %s", describeEvent(e)));

        // Log AddPlayerToWorldEvent
        eventBus.register(
                (Class) AddPlayerToWorldEvent.class,
                (java.util.function.Consumer) (Object e) -> {
                    log.info("🔴 EVENT: %s", describeEvent(e));
                });

        // Log DrainPlayerFromWorldEvent (removal from world)
        eventBus.register(
                (Class) DrainPlayerFromWorldEvent.class,
                (java.util.function.Consumer) (Object e) -> {
                    log.info("🔴 EVENT: %s", describeEvent(e));
                });

        // Log StartWorldEvent
        eventBus.register(
                (Class) StartWorldEvent.class,
                (java.util.function.Consumer) (Object e) -> {
                    log.info("🔴 EVENT: %s", describeEvent(e));
                });

        // Log AddWorldEvent
        eventBus.register(
                (Class) AddWorldEvent.class,
                (java.util.function.Consumer) (Object e) -> {
                    log.info("🔴 EVENT: %s", describeEvent(e));
                });

        log.info("✅ Universal event logger registered - watching for all events");
    }

    /**
     * Attempts to attach a logger to every event class currently known by the
     * EventBus registry.
     * This relies on EventBus#getRegisteredEventClasses(), so it only covers
     * classes registered so far.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void registerAllKnown(@Nonnull EventBus eventBus) {
        try {
            for (Class<? extends IBaseEvent<?>> eventClass : eventBus.getRegisteredEventClasses()) {
                // Skip a few we already registered explicitly to avoid duplicate log spam
                if (eventClass == PlayerConnectEvent.class ||
                        eventClass == PlayerDisconnectEvent.class ||
                        eventClass == AddPlayerToWorldEvent.class ||
                        eventClass == DrainPlayerFromWorldEvent.class ||
                        eventClass == StartWorldEvent.class ||
                        eventClass == AddWorldEvent.class) {
                    continue;
                }

                eventBus.register(
                        (Class) eventClass,
                        (java.util.function.Consumer) (Object e) -> {
                            try {
                                log.info("🔴 EVENT: %s", describeEvent(e));
                            } catch (Exception ex) {
                                log.info("🔴 EVENT: %s (logging error: %s)", eventClass.getName(), ex.getMessage());
                            }
                        });
            }
            log.info("✅ Universal event logger registered to all known event classes (%d)",
                    eventBus.getRegisteredEventClasses().size());
        } catch (Exception e) {
            log.error("Failed to register all known events: %s", e.getMessage());
        }
    }

    private String describeEvent(Object event) {
        if (event == null) {
            return "null-event";
        }

        Class<?> clazz = event.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append(clazz.getSimpleName()).append(" (").append(clazz.getName()).append(")");

        appendIfPresent(sb, event, "getWorld", "world");
        appendIfPresent(sb, event, "getPlayer", "player");
        appendIfPresent(sb, event, "getTransform", "transform");

        // Add a small sample of other getters for more metadata
        appendGetterSamples(sb, event, 6);

        try {
            sb.append(" toString=").append(event.toString());
        } catch (Exception ignored) {
            sb.append(" toString=<error>");
        }
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, Object target, String methodName, String label) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object value = m.invoke(target);
            if (value != null) {
                sb.append(" ").append(label).append("=");
                try {
                    Method nameMethod = value.getClass().getMethod("getName");
                    Object nameVal = nameMethod.invoke(value);
                    sb.append(String.valueOf(nameVal));
                } catch (Exception ex) {
                    sb.append(String.valueOf(value));
                }
            }
        } catch (NoSuchMethodException ignored) {
            // Method not present on this event type
        } catch (Exception ignored) {
            // Ignore reflection errors to keep logging resilient
        }
    }

    /**
     * Collects a limited set of simple getter values to give more context without
     * dumping entire objects.
     */
    private void appendGetterSamples(StringBuilder sb, Object target, int max) {
        int added = 0;
        Class<?> clazz = target.getClass();
        for (Method method : clazz.getMethods()) {
            if (added >= max) {
                break;
            }
            String name = method.getName();
            if (!name.startsWith("get") && !name.startsWith("is")) {
                continue;
            }
            if (method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
                continue;
            }
            if ("getClass".equals(name) || "getWorld".equals(name) || "getPlayer".equals(name)
                    || "getTransform".equals(name)) {
                continue;
            }
            try {
                Object val = method.invoke(target);
                if (val == null) {
                    continue;
                }
                sb.append(' ').append(name).append('=');
                try {
                    Method nameMethod = val.getClass().getMethod("getName");
                    Object nameVal = nameMethod.invoke(val);
                    sb.append(String.valueOf(nameVal));
                } catch (Exception ex) {
                    sb.append(String.valueOf(val));
                }
                added++;
            } catch (Exception ignored) {
                // Skip getters that throw
            }
        }
    }
}
