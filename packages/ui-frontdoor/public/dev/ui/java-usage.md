# Java Usage (Shared Interfaces)

These are short, copy‑friendly patterns for wiring UI pages and HUDs. Minimal references, maximum snippets.

## Pages (BasicCustomUIPage)

Minimal page class:

```java
public class DemoInputsPage extends BasicCustomUIPage {
  public DemoInputsPage(@Nonnull PlayerRef playerRef) {
    super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
  }

  @Override
  public void build(UICommandBuilder builder) {
    builder.append("Demos/Pages/DemoInputs.ui");
  }
}
```

Command open pattern:

```java
Player player = store.getComponent(ref, Player.getComponentType());
DemoInputsPage page = new DemoInputsPage(playerRef);
player.getPageManager().openCustomPage(ref, store, page);
```

## HUDs (CustomUIHud)

Minimal HUD class:

```java
public class DemoHudWidgetStripHud extends CustomUIHud {
  public DemoHudWidgetStripHud(@Nonnull PlayerRef playerRef) {
    super(playerRef);
  }

  @Override
  protected void build(UICommandBuilder builder) {
    builder.append("Demos/Huds/DemoHudWidgetStrip.ui");
  }
}
```

Show / reset HUD:

```java
HudManager hudManager = player.getHudManager();
hudManager.setCustomHud(playerRef, new DemoHudWidgetStripHud(playerRef));
// reset
hudManager.resetHud(playerRef);
```

Clear active HUD (safe wipe):

```java
CustomUIHud currentHud = hudManager.getCustomHud();
if (currentHud != null) {
  UICommandBuilder builder = new UICommandBuilder();
  currentHud.update(true, builder);
}
```

## UI Updates (Set Text)

```java
UICommandBuilder builder = new UICommandBuilder();
builder.set("#FriendsListBody.Text", Message.raw("Hello UI"));
page.update(false, builder); // for pages
```

Server translations:

```java
builder.set("#FriendsListBody.Text", Message.translation("server.ID_OF_TRANSLATION"));
```

Translation keys must live in `shared/interfaces/src/main/resources/Server/Languages/en-US/server.lang`.

## Validation & Debug Commands

```java
// Simple usage error
context.sendMessage(Message.raw("Usage: /demo <page>"));
```

```java
// Guard for non-player callers
if (playerRef == null) {
  context.sendMessage(Message.raw("Player only."));
  return;
}
```

## Common Pitfalls

- UI paths are **relative to** `Common/UI/Custom/`.
- If the client crashes, the path is usually wrong or missing in the asset pack.
- Keep demo pages tiny and isolate changes when debugging.
- Clear HUD state before a reset if you suspect stale UI.
