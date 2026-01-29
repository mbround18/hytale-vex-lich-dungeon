package MBRound18.hytale.tools;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Validates Hytale UI syntax in .ui files.
 * Checks for common syntax errors that cause client crashes.
 */
public class UiValidator {

  private final Path baseDir;
  private final List<Path> importRoots;
  private final List<Path> assetRoots;
  private final Path coreUiRoot;
  private final Set<Path> textureIndex = new HashSet<>();
  private CoreUiRules coreRules;
  private final List<ValidationError> errors = new ArrayList<>();

  public UiValidator(Path baseDir) {
    this(baseDir, List.of(baseDir), List.of(baseDir), null);
  }

  public UiValidator(Path baseDir, List<Path> importRoots, List<Path> assetRoots) {
    this(baseDir, importRoots, assetRoots, null);
  }

  public UiValidator(Path baseDir, List<Path> importRoots, List<Path> assetRoots, Path coreUiRoot) {
    this.baseDir = baseDir;
    this.importRoots = importRoots;
    this.assetRoots = assetRoots;
    this.coreUiRoot = coreUiRoot;
  }

  public static class ValidationError {
    public final Path file;
    public final int line;
    public final String message;

    public ValidationError(Path file, int line, String message) {
      this.file = file;
      this.line = line;
      this.message = message;
    }

    @Override
    public String toString() {
      return file.getFileName() + ":" + line + " - " + message;
    }
  }

  /**
   * Validate all .ui files in the plugins directory
   */
  public List<ValidationError> validateAll() throws IOException {
    errors.clear();
    buildTextureIndex();
    if (coreUiRoot != null) {
      buildCoreRules();
    }

    try (Stream<Path> paths = Files.walk(baseDir)) {
      paths.filter(p -> p.toString().endsWith(".ui"))
          .forEach(this::validateFile);
    }

    return new ArrayList<>(errors);
  }

  /**
   * Validate a single UI file
   */
  private void validateFile(Path file) {
    try {
      List<String> lines = Files.readAllLines(file);
      if (!lines.isEmpty()) {
        lines.set(0, stripBom(lines.get(0)));
      }
      String content = String.join("\n", lines);
      content = stripBom(content);

      // Check syntax structure (delimiters + statements)
      checkSyntaxStructure(file, content);

      // Check for invalid property wrapping (common mistake)
      checkPropertyWrapping(file, lines);

      // Check imports exist
      checkImports(file, lines);

      // Check import aliases are referenced consistently
      checkImportAliases(file, lines);

      // Check for unsupported node types
      checkUnsupportedNodes(file, lines);

      // Check against core UI rules
      checkCoreRules(file, content);

      // Check color syntax
      checkColorSyntax(file, lines);

      // Check for common typos
      checkCommonTypos(file, lines);

      // Check texture path existence
      checkTexturePaths(file, lines);

    } catch (IOException e) {
      errors.add(new ValidationError(file, 0, "Failed to read file: " + e.getMessage()));
    }
  }

  private enum StatementType {
    UNKNOWN,
    PROPERTY,
    ASSIGNMENT,
    ELEMENT
  }

  private static class StatementContext {
    final int startLine;
    final int startBraceDepth;
    final int startParenDepth;
    int topLevelSeparators = 0;
    StatementType type = StatementType.UNKNOWN;
    final StringBuilder text = new StringBuilder();

    StatementContext(int startLine, int startBraceDepth, int startParenDepth) {
      this.startLine = startLine;
      this.startBraceDepth = startBraceDepth;
      this.startParenDepth = startParenDepth;
    }
  }

  /**
   * Check syntax structure: balanced delimiters + statement boundaries
   */
  private void checkSyntaxStructure(Path file, String content) {
    int braceDepth = 0;
    int parenDepth = 0;
    int bracketDepth = 0;
    int line = 1;
    int charIndex = 0;
    boolean inString = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;
    boolean escaped = false;
    StatementContext current = null;

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      char next = (i + 1 < content.length()) ? content.charAt(i + 1) : '\0';
      charIndex++;

      if (c == '\n') {
        line++;
        charIndex = 0;
        if (inLineComment) {
          inLineComment = false;
        }
        if (current != null) {
          current.text.append(c);
        }
        continue;
      }

      if (inBlockComment) {
        if (c == '*' && next == '/') {
          inBlockComment = false;
          i++;
        }
        continue;
      }

      if (inLineComment) {
        continue;
      }

      if (inString) {
        if (!escaped && c == '"') {
          inString = false;
        }
        escaped = (!escaped && c == '\\');
        continue;
      }

      if (c == '/' && next == '*') {
        inBlockComment = true;
        i++;
        continue;
      }

      if (c == '/' && next == '/') {
        inLineComment = true;
        i++;
        continue;
      }

      if (c == '"') {
        inString = true;
        escaped = false;
        continue;
      }

      if (current == null && !Character.isWhitespace(c) && c != ';' && c != '}') {
        current = new StatementContext(line, braceDepth, parenDepth);
      }

      if (current != null) {
        current.text.append(c);
      }

      if (c == '(') {
        parenDepth++;
        continue;
      }

      if (c == ')') {
        parenDepth--;
        if (parenDepth < 0) {
          errors.add(new ValidationError(file, line,
              "Unmatched closing parenthesis at position " + charIndex));
          parenDepth = 0;
        }
        continue;
      }

      if (c == '[') {
        bracketDepth++;
        continue;
      }

      if (c == ']') {
        bracketDepth--;
        if (bracketDepth < 0) {
          errors.add(new ValidationError(file, line,
              "Unmatched closing bracket at position " + charIndex));
          bracketDepth = 0;
        }
        continue;
      }

      if (c == '{') {
        if (current != null
          && parenDepth == current.startParenDepth
          && braceDepth == current.startBraceDepth
          && current.type == StatementType.UNKNOWN) {
          current.type = StatementType.ELEMENT;
          current = null;
        }
        braceDepth++;
        continue;
      }

      if (c == '}') {
        braceDepth--;
        if (braceDepth < 0) {
          errors.add(new ValidationError(file, line,
            "Unmatched closing brace at position " + charIndex));
          braceDepth = 0;
        }
        continue;
      }

      if (current != null
        && parenDepth == current.startParenDepth
        && braceDepth == current.startBraceDepth
        && (c == ':' || c == '=')) {
        if (current.type == StatementType.UNKNOWN) {
          current.type = (c == ':') ? StatementType.PROPERTY : StatementType.ASSIGNMENT;
        }
        current.topLevelSeparators++;
        continue;
      }

      if (current != null
        && parenDepth == current.startParenDepth
        && braceDepth == current.startBraceDepth
        && c == ';') {
        analyzeStatement(file, current.text.toString(), current.startLine, current.type, current.topLevelSeparators);
        current = null;
      }
    }

    if (current != null && current.type != StatementType.ELEMENT && current.text.length() > 0) {
      errors.add(new ValidationError(file, current.startLine,
        "Missing semicolon at end of statement"));
    }

    if (braceDepth != 0) {
      errors.add(new ValidationError(file, line,
          "Unbalanced braces: " + braceDepth + " unclosed"));
    }

    if (parenDepth != 0) {
      errors.add(new ValidationError(file, line,
          "Unbalanced parentheses: " + parenDepth + " unclosed"));
    }

    if (bracketDepth != 0) {
      errors.add(new ValidationError(file, line,
          "Unbalanced brackets: " + bracketDepth + " unclosed"));
    }

    if (inString) {
      errors.add(new ValidationError(file, line,
          "Unterminated string literal"));
    }

    if (inBlockComment) {
      errors.add(new ValidationError(file, line,
          "Unterminated block comment"));
    }
  }


  private void analyzeStatement(Path file, String statement, int line, StatementType type, int topLevelSeparators) {
    String trimmed = statement.trim();
    if (trimmed.isEmpty()) {
      return;
    }

    if (trimmed.startsWith("...")) {
      errors.add(new ValidationError(file, line,
          "Standalone spread is not supported; use tuple spread or template instantiation"));
      return;
    }

    if (trimmed.endsWith(";")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
    }

    if (type == StatementType.ASSIGNMENT || type == StatementType.PROPERTY) {
      if (topLevelSeparators > 1) {
        errors.add(new ValidationError(file, line,
            "Multiple top-level assignments/properties in one statement (missing semicolon?)"));
        return;
      }

      if (type == StatementType.ASSIGNMENT) {
        Pattern assignment = Pattern.compile("^\\s*[@$][A-Za-z][A-Za-z0-9_]*\\s*=");
        if (!assignment.matcher(trimmed).find()) {
          errors.add(new ValidationError(file, line,
              "Invalid assignment target (expected @Name or $Name)"));
        }
      } else {
        Pattern property = Pattern.compile("^\\s*[A-Za-z][A-Za-z0-9_]*\\s*:");
        if (!property.matcher(trimmed).find()) {
          errors.add(new ValidationError(file, line,
              "Invalid property name"));
        }
      }
    }
  }

  /**
   * Check for invalid nested property instantiation (removed - this is actually
   * valid)
   * Template properties like @DecoratedContainer ARE meant to be instantiated
   * with { }
   */
  private void checkPropertyWrapping(Path file, List<String> lines) {
    // Previously checked for $V.@Property { but this is valid Hytale UI syntax
    // Templates can be instantiated and customized
  }

  /**
   * Check that imported files exist
   */
  private void checkImports(Path file, List<String> lines) {
    Pattern importPattern = Pattern.compile("^\\$\\w+\\s*=\\s*\"([^\"]+)\";?$");
    Path fileDir = file.getParent();

    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      Matcher m = importPattern.matcher(line);

      if (m.matches()) {
        String importPath = m.group(1);
        Path resolvedPath = fileDir.resolve(importPath).normalize();
        if (!Files.exists(resolvedPath)) {
          boolean found = false;
          for (Path root : importRoots) {
            Path candidate = root.resolve(importPath).normalize();
            if (Files.exists(candidate)) {
              found = true;
              break;
            }
          }
          if (!found) {
          errors.add(new ValidationError(file, i + 1,
              "Import not found: " + importPath + " (resolved to: " + resolvedPath + ")"));
          }
        }
      }
    }
  }

  /**
   * Check that any $Alias.@Template usage refers to a declared import
   */
  private void checkImportAliases(Path file, List<String> lines) {
    Pattern importPattern = Pattern.compile("^\\s*\\$(\\w+)\\s*=\\s*\"([^\"]+)\"\\s*;?\\s*$");
    Set<String> imports = new HashSet<>();

    for (String line : lines) {
      Matcher m = importPattern.matcher(line);
      if (m.matches()) {
        imports.add(m.group(1));
      }
    }

    Pattern usagePattern = Pattern.compile("\\$([A-Za-z][A-Za-z0-9_]*)\\.");
    boolean[] inBlockComment = new boolean[] { false };
    for (int i = 0; i < lines.size(); i++) {
      String line = stripComments(lines.get(i), inBlockComment);
      if (inBlockComment[0]) {
        continue;
      }
      String cleaned = stripStringLiterals(line);
      Matcher usage = usagePattern.matcher(cleaned);
      while (usage.find()) {
        String alias = usage.group(1);
        if (!imports.contains(alias)) {
          errors.add(new ValidationError(file, i + 1,
              "Unknown import alias: $" + alias + " (missing $" + alias + " = \"...\";)"));
        }
      }
    }
  }

  private void checkUnsupportedNodes(Path file, List<String> lines) {
    Pattern elementStart = Pattern.compile("^\\s*([A-Za-z][A-Za-z0-9_]*)\\s*(#\\w+)?\\s*\\{");
    boolean[] inBlockComment = new boolean[] { false };
    for (int i = 0; i < lines.size(); i++) {
      String line = stripComments(lines.get(i), inBlockComment);
      if (inBlockComment[0]) {
        continue;
      }
      String cleaned = stripStringLiterals(line).trim();
      Matcher m = elementStart.matcher(cleaned);
      if (m.find()) {
        String node = m.group(1);
        if ("Image".equals(node)) {
          errors.add(new ValidationError(file, i + 1,
              "Unsupported node type: Image (use Group with Background or BackgroundImage)"));
        }
      }
    }
  }

  private void checkCoreRules(Path file, String content) {
    if (coreRules == null) {
      return;
    }
    scanUi(file, content, new UiScanHandler() {
      @Override
      public void onNode(String type, boolean explicit, int line) {
        if (!explicit || type == null) {
          return;
        }
        if (!coreRules.nodeTypes.contains(type)) {
          errors.add(new ValidationError(file, line,
              "Unknown node type: " + type + " (not observed in core UI)"));
        }
      }

      @Override
      public void onProperty(String name, boolean explicitNode, String nodeType, int line) {
        if (!explicitNode || nodeType == null || name == null) {
          return;
        }
        Set<String> allowed = coreRules.nodeProperties.get(nodeType);
        if (allowed == null || !allowed.contains(name)) {
          errors.add(new ValidationError(file, line,
              "Unknown property '" + name + "' on node type " + nodeType + " (not observed in core UI)"));
        }
      }
    });
  }

  private static class CoreUiRules {
    final Set<String> nodeTypes = new HashSet<>();
    final Map<String, Set<String>> nodeProperties = new HashMap<>();
  }

  private static class NodeFrame {
    final String type;
    final boolean explicit;
    final int braceDepth;

    NodeFrame(String type, boolean explicit, int braceDepth) {
      this.type = type;
      this.explicit = explicit;
      this.braceDepth = braceDepth;
    }
  }

  private interface UiScanHandler {
    void onNode(String type, boolean explicit, int line);
    void onProperty(String name, boolean explicitNode, String nodeType, int line);
  }

  private void buildCoreRules() throws IOException {
    coreRules = new CoreUiRules();
    if (!Files.exists(coreUiRoot)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(coreUiRoot)) {
      paths.filter(p -> p.toString().endsWith(".ui"))
          .forEach(p -> {
            try {
              String content = Files.readString(p);
              content = stripBom(content);
              scanUi(p, content, new UiScanHandler() {
                @Override
                public void onNode(String type, boolean explicit, int line) {
                  if (!explicit || type == null) {
                    return;
                  }
                  coreRules.nodeTypes.add(type);
                }

                @Override
                public void onProperty(String name, boolean explicitNode, String nodeType, int line) {
                  if (!explicitNode || nodeType == null || name == null) {
                    return;
                  }
                  coreRules.nodeProperties
                      .computeIfAbsent(nodeType, k -> new HashSet<>())
                      .add(name);
                }
              });
            } catch (IOException ignored) {
            }
          });
    }
  }

  private void scanUi(Path file, String content, UiScanHandler handler) {
    int braceDepth = 0;
    int parenDepth = 0;
    int line = 1;
    boolean inString = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;
    boolean escaped = false;
    StatementContext current = null;
    Deque<NodeFrame> nodeStack = new ArrayDeque<>();

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      char next = (i + 1 < content.length()) ? content.charAt(i + 1) : '\0';

      if (c == '\n') {
        line++;
        if (inLineComment) {
          inLineComment = false;
        }
        if (current != null) {
          current.text.append(c);
        }
        continue;
      }

      if (inBlockComment) {
        if (c == '*' && next == '/') {
          inBlockComment = false;
          i++;
        }
        continue;
      }

      if (inLineComment) {
        continue;
      }

      if (inString) {
        if (!escaped && c == '"') {
          inString = false;
        }
        escaped = (!escaped && c == '\\');
        continue;
      }

      if (c == '/' && next == '*') {
        inBlockComment = true;
        i++;
        continue;
      }

      if (c == '/' && next == '/') {
        inLineComment = true;
        i++;
        continue;
      }

      if (c == '"') {
        inString = true;
        escaped = false;
        continue;
      }

      if (current == null && !Character.isWhitespace(c) && c != ';' && c != '}') {
        current = new StatementContext(line, braceDepth, parenDepth);
      }

      if (current != null) {
        current.text.append(c);
      }

      if (c == '(') {
        parenDepth++;
        continue;
      }

      if (c == ')') {
        parenDepth--;
        continue;
      }

      if (c == '{') {
        if (current != null
            && parenDepth == current.startParenDepth
            && braceDepth == current.startBraceDepth
            && (current.type == StatementType.UNKNOWN || current.type == StatementType.ASSIGNMENT)) {
          NodeFrame frame = parseNodeFrame(current.text.toString(), braceDepth);
          if (frame != null) {
            nodeStack.push(frame);
            handler.onNode(frame.type, frame.explicit, current.startLine);
          } else {
            nodeStack.push(new NodeFrame(null, false, braceDepth));
          }
          current = null;
        }
        braceDepth++;
        continue;
      }

      if (c == '}') {
        braceDepth--;
        if (!nodeStack.isEmpty() && nodeStack.peek().braceDepth == braceDepth) {
          nodeStack.pop();
        }
        continue;
      }

      if (current != null
          && parenDepth == current.startParenDepth
          && braceDepth == current.startBraceDepth
          && (c == ':' || c == '=')) {
        if (current.type == StatementType.UNKNOWN) {
          current.type = (c == ':') ? StatementType.PROPERTY : StatementType.ASSIGNMENT;
        }
        current.topLevelSeparators++;
        continue;
      }

      if (current != null
          && parenDepth == current.startParenDepth
          && braceDepth == current.startBraceDepth
          && c == ';') {
        if (current.type == StatementType.PROPERTY && !nodeStack.isEmpty()) {
          String prop = extractPropertyName(current.text.toString());
          NodeFrame frame = nodeStack.peek();
          handler.onProperty(prop, frame.explicit, frame.type, current.startLine);
        }
        current = null;
      }
    }
  }

  private NodeFrame parseNodeFrame(String header, int braceDepth) {
    String trimmed = header.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    char first = trimmed.charAt(0);
    if (trimmed.startsWith("...")) {
      return new NodeFrame(null, false, braceDepth);
    }

    // Template definition: @Name = Group { ... }
    Matcher assign = Pattern.compile("^[@$][A-Za-z0-9_.]+\\s*=\\s*([A-Za-z][A-Za-z0-9_]*)").matcher(trimmed);
    if (assign.find()) {
      String type = assign.group(1);
      return new NodeFrame(type, true, braceDepth);
    }

    if (first == '#' || first == '@' || first == '$') {
      return new NodeFrame(null, false, braceDepth);
    }

    Matcher m = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*)").matcher(trimmed);
    if (!m.find()) {
      return new NodeFrame(null, false, braceDepth);
    }
    String type = m.group(1);
    return new NodeFrame(type, true, braceDepth);
  }

  private String extractPropertyName(String statement) {
    Matcher m = Pattern.compile("^\\s*([A-Za-z][A-Za-z0-9_]*)\\s*:").matcher(statement.trim());
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

  /**
   * Check color syntax is valid
   */
  private void checkColorSyntax(Path file, List<String> lines) {
    // Valid color: #RGB/#RGBA/#RRGGBB/#RRGGBBAA with optional (alpha)
    // Element IDs: #NameLikeThis (letters after #)
    Pattern validColor = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})(\\([0-9.]+\\))?");
    Pattern elementId = Pattern.compile("#[A-Za-z][A-Za-z0-9_]*");
    boolean[] inBlockComment = new boolean[] { false };

    for (int i = 0; i < lines.size(); i++) {
      String line = stripComments(lines.get(i), inBlockComment);
      if (inBlockComment[0]) {
        continue;
      }
      line = stripStringLiterals(line);
      if (!line.contains("#"))
        continue;

      // Remove all valid colors and element IDs
      String cleaned = validColor.matcher(line).replaceAll("");
      cleaned = elementId.matcher(cleaned).replaceAll("");

      // Now check if there are any remaining malformed # color patterns
      // (hex digits after # that aren't valid 3 or 6 digit colors)
      Pattern malformed = Pattern.compile("#[0-9a-fA-F]+");
      Matcher bad = malformed.matcher(cleaned);
      if (bad.find()) {
        String badCode = bad.group();
        errors.add(new ValidationError(file, i + 1,
            "Malformed color code: " + badCode + " (must be #RGB/#RGBA/#RRGGBB/#RRGGBBAA)"));
      }
    }
  }

  /**
   * Check for common typos
   */
  private void checkCommonTypos(Path file, List<String> lines) {
    Map<Pattern, String> typos = Map.of(
        Pattern.compile("Hieght"), "Did you mean 'Height'?",
        Pattern.compile("Widht"), "Did you mean 'Width'?",
        Pattern.compile("Backgroun[^d]"), "Did you mean 'Background'?",
        Pattern.compile("Padd?ig[^n]"), "Did you mean 'Padding'?");

    boolean[] inBlockComment = new boolean[] { false };
    for (int i = 0; i < lines.size(); i++) {
      String line = stripComments(lines.get(i), inBlockComment);
      if (inBlockComment[0]) {
        continue;
      }
      line = stripStringLiterals(line);
      for (Map.Entry<Pattern, String> entry : typos.entrySet()) {
        if (entry.getKey().matcher(line).find()) {
          errors.add(new ValidationError(file, i + 1, entry.getValue()));
        }
      }
    }
  }

  private void checkTexturePaths(Path file, List<String> lines) {
    Pattern texturePath = Pattern.compile("TexturePath\\s*:\\s*\"([^\"]+)\"");
    Pattern backgroundString = Pattern.compile("Background\\s*:\\s*\"([^\"]+)\"");
    boolean[] inBlockComment = new boolean[] { false };

    List<Path> candidates = getTextureBases(file);

    for (int i = 0; i < lines.size(); i++) {
      String line = stripComments(lines.get(i), inBlockComment);
      if (inBlockComment[0]) {
        continue;
      }
      String cleaned = stripStringLiterals(line);
      String original = line;

      checkTextureMatch(file, i + 1, original, texturePath, candidates);
      checkTextureMatch(file, i + 1, original, backgroundString, candidates);
    }
  }

  private void checkTextureMatch(Path file, int lineNumber, String line, Pattern pattern, List<Path> bases) {
    Matcher m = pattern.matcher(line);
    while (m.find()) {
      String path = m.group(1);
      if (path.startsWith("http://") || path.startsWith("https://")) {
        continue;
      }
      if (path.isBlank()) {
        continue;
      }
      boolean found = false;
      for (Path base : bases) {
        Path resolved = base.resolve(path).normalize();
        if (textureIndex.contains(resolved)) {
          found = true;
          break;
        }
      }
      if (!found) {
        errors.add(new ValidationError(file, lineNumber,
            "Texture not found: " + path));
      }
    }
  }

  private List<Path> getTextureBases(Path file) {
    List<Path> bases = new ArrayList<>();
    Path dir = file.getParent();
    if (dir != null) {
      bases.add(dir);
    }

    Path uiCustom = findAncestor(dir, "UI", "Custom");
    if (uiCustom != null) {
      bases.add(uiCustom);
    }

    Path ui = findAncestor(dir, "UI");
    if (ui != null) {
      bases.add(ui);
    }

    Path iface = findAncestor(dir, "Interface");
    if (iface != null) {
      bases.add(iface);
    }

    for (Path root : assetRoots) {
      bases.add(root);
    }

    LinkedHashSet<Path> uniq = new LinkedHashSet<>(bases);
    return new ArrayList<>(uniq);
  }

  private Path findAncestor(Path dir, String... names) {
    if (dir == null) {
      return null;
    }
    Path current = dir;
    while (current != null) {
      if (names.length == 1) {
        if (current.getFileName() != null && current.getFileName().toString().equals(names[0])) {
          return current;
        }
      } else if (names.length == 2) {
        if (current.getFileName() != null && current.getFileName().toString().equals(names[1])) {
          Path parent = current.getParent();
          if (parent != null && parent.getFileName() != null && parent.getFileName().toString().equals(names[0])) {
            return current;
          }
        }
      }
      current = current.getParent();
    }
    return null;
  }

  private void buildTextureIndex() throws IOException {
    textureIndex.clear();
    Set<String> exts = Set.of(".png", ".jpg", ".jpeg", ".webp", ".dds");
    for (Path root : assetRoots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> paths = Files.walk(root)) {
        paths.filter(Files::isRegularFile).forEach(p -> {
          String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
          for (String ext : exts) {
            if (name.endsWith(ext)) {
              textureIndex.add(p.normalize());
              break;
            }
          }
        });
      }
    }
  }

  private String stripComments(String line, boolean[] inBlockComment) {
    StringBuilder cleaned = new StringBuilder();
    boolean inString = false;
    boolean escaped = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      char next = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';

      if (inBlockComment[0]) {
        if (c == '*' && next == '/') {
          inBlockComment[0] = false;
          i++;
        }
        continue;
      }

      if (inString) {
        cleaned.append(c);
        if (!escaped && c == '"') {
          inString = false;
        }
        escaped = (!escaped && c == '\\');
        continue;
      }

      if (c == '"') {
        inString = true;
        escaped = false;
        cleaned.append(c);
        continue;
      }

      if (c == '/' && next == '*') {
        inBlockComment[0] = true;
        i++;
        continue;
      }

      if (c == '/' && next == '/') {
        break;
      }

      cleaned.append(c);
    }
    return cleaned.toString();
  }

  private String stripStringLiterals(String line) {
    StringBuilder cleaned = new StringBuilder();
    boolean inString = false;
    boolean escaped = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (inString) {
        if (!escaped && c == '"') {
          inString = false;
        }
        escaped = (!escaped && c == '\\');
        continue;
      }
      if (c == '"') {
        inString = true;
        escaped = false;
        continue;
      }
      cleaned.append(c);
    }
    return cleaned.toString();
  }

  private String stripBom(String value) {
    if (value != null && !value.isEmpty() && value.charAt(0) == '\ufeff') {
      return value.substring(1);
    }
    return value;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: UiValidator <scan-dir> [--import-root <path> ...] [--asset-root <path> ...] [--core-ui-root <path>]");
      System.exit(1);
    }

    Path scanDir = Paths.get(args[0]);
    if (!Files.isDirectory(scanDir)) {
      System.err.println("Not a directory: " + scanDir);
      System.exit(1);
    }

    List<Path> importRoots = new ArrayList<>();
    List<Path> assetRoots = new ArrayList<>();
    Path coreUiRoot = null;
    for (int i = 1; i < args.length; i++) {
      String arg = args[i];
      if ("--import-root".equals(arg) && i + 1 < args.length) {
        importRoots.add(Paths.get(args[++i]));
      } else if ("--asset-root".equals(arg) && i + 1 < args.length) {
        assetRoots.add(Paths.get(args[++i]));
      } else if ("--core-ui-root".equals(arg) && i + 1 < args.length) {
        coreUiRoot = Paths.get(args[++i]);
      }
    }
    if (importRoots.isEmpty()) {
      importRoots.add(scanDir);
    }
    if (assetRoots.isEmpty()) {
      assetRoots.add(scanDir);
    }

    UiValidator validator = new UiValidator(scanDir, importRoots, assetRoots, coreUiRoot);
    try {
      List<ValidationError> errors = validator.validateAll();

      if (errors.isEmpty()) {
        System.out.println("✅ All UI files are valid!");
        System.exit(0);
      } else {
        System.out.println("❌ Found " + errors.size() + " validation error(s):\n");
        for (ValidationError error : errors) {
          System.out.println("  " + error);
        }
        System.exit(1);
      }
    } catch (IOException e) {
      System.err.println("Error scanning files: " + e.getMessage());
      e.printStackTrace();
      System.exit(2);
    }
  }
}
