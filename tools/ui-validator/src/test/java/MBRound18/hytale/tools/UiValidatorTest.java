package MBRound18.hytale.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UiValidatorTest {

  @Test
  void testValidUiFile(@TempDir Path tempDir) throws IOException {
    // Create the imported file first
    Path commonFile = tempDir.resolve("Common.ui");
    Files.writeString(commonFile, "@PageOverlay = Group { Background: #000; };");

    Path vexDir = tempDir.resolve("Vex");
    Files.createDirectories(vexDir);

    Path testFile = vexDir.resolve("Valid.ui");
    Files.writeString(testFile, """
      $C = "../Common.ui";
      
      Group {
        $C.@PageOverlay { }
        Label { Text: "Hello"; }
      }
      """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.isEmpty(), "Valid UI should have no errors, but got: " + errors);
  }

  @Test
  void testUnbalancedBraces(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("Unbalanced.ui");
    Files.writeString(testFile, """
        Group {
          Label { Text: "Test";
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertFalse(errors.isEmpty(), "Should detect unbalanced braces");
    assertTrue(errors.get(0).message.contains("Unbalanced braces"));
  }

  @Test
  void testMissingImport(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("MissingImport.ui");
    Files.writeString(testFile, """
        $V = "../NonExistent.ui";

        Group { }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertFalse(errors.isEmpty(), "Should detect missing import");
    assertTrue(errors.get(0).message.contains("Import not found"));
  }

  @Test
  void testMalformedColor(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("BadColor.ui");
    Files.writeString(testFile, """
        Group {
          Background: #1;
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertFalse(errors.isEmpty(), "Should detect malformed color");
    assertTrue(errors.get(0).message.contains("Malformed color"));
  }

  @Test
  void testValidTemplateInstantiation(@TempDir Path tempDir) throws IOException {
    // This syntax is VALID - templates can be instantiated
    Path commonFile = tempDir.resolve("Common.ui");
    Files.writeString(commonFile, """
        @Container = Group {
          Background: #000;
        };
        """);

    Path testFile = tempDir.resolve("TestPage.ui");
    Files.writeString(testFile, """
        $C = "Common.ui";

        $C.@Container {
          Label { Text: "Hello"; }
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    // Should not error on template instantiation
    assertTrue(
        errors.isEmpty()
            || errors.stream().noneMatch(e -> e.message.contains("property") && e.message.contains("wrap")),
        "Template instantiation is valid syntax");
  }

  @Test
  void testUnbalancedParentheses(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("BadParen.ui");
    Files.writeString(testFile, """
        Group {
          Anchor: (Width: 10;
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Unbalanced parentheses")
        || e.message.contains("parenthesis")), "Should detect unbalanced parentheses");
  }

  @Test
  void testMissingSemicolonBetweenProperties(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("MissingSemicolon.ui");
    Files.writeString(testFile, """
        Group {
          Anchor: (Width: 10)
          Padding: (Top: 4);
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Missing semicolon")
        || e.message.contains("Multiple top-level")), "Should detect missing semicolon between statements");
  }

  @Test
  void testUnknownImportAlias(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("UnknownAlias.ui");
    Files.writeString(testFile, """
        Group {
          Label { Style: (...$C.@DefaultLabelStyle); }
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Unknown import alias")),
        "Should detect missing import alias");
  }

  @Test
  void testBlockCommentsAreIgnored(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("BlockComment.ui");
    Files.writeString(testFile, """
        Group {
          Anchor: (Width: 10, Height: 20);
          /* This is a block comment
             that should be ignored */
          Padding: (Top: 4);
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.isEmpty(), "Block comments should not cause syntax errors");
  }

  @Test
  void testRgbaHexColors(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("RgbaHex.ui");
    Files.writeString(testFile, """
        Group {
          Background: #c0c0c0bb;
          TextColor: #abcd;
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.isEmpty(), "RGBA and RGBAA hex colors should be valid");
  }

  @Test
  void testBomAtFileStart(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("Bom.ui");
    String content = "\uFEFF@Example = Group { Background: #000; };";
    Files.writeString(testFile, content);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.isEmpty(), "BOM should be ignored at start of file");
  }

  @Test
  void testTexturePathExists(@TempDir Path tempDir) throws IOException {
    Path assetsRoot = tempDir.resolve("assets");
    Files.createDirectories(assetsRoot.resolve("Textures"));
    Files.writeString(assetsRoot.resolve("Textures/icon.png"), "x");

    Path testFile = tempDir.resolve("Texture.ui");
    Files.writeString(testFile, """
        Group {
          Background: (TexturePath: "Textures/icon.png");
        }
        """);

    UiValidator validator = new UiValidator(tempDir, List.of(tempDir), List.of(assetsRoot));
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.isEmpty(), "Existing texture should pass");
  }

  @Test
  void testTexturePathMissing(@TempDir Path tempDir) throws IOException {
    Path assetsRoot = tempDir.resolve("assets");
    Files.createDirectories(assetsRoot);

    Path testFile = tempDir.resolve("Texture.ui");
    Files.writeString(testFile, """
        Group {
          Background: (TexturePath: "Textures/missing.png");
        }
        """);

    UiValidator validator = new UiValidator(tempDir, List.of(tempDir), List.of(assetsRoot));
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Texture not found")),
        "Missing texture should be reported");
  }

  @Test
  void testStandaloneSpreadIsRejected(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("Spread.ui");
    Files.writeString(testFile, """
        Group {
          ...@DefaultLabelStyle;
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Standalone spread")),
        "Standalone spread statements should be rejected");
  }

  @Test
  void testUnbalancedBrackets(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("BadBrackets.ui");
    Files.writeString(testFile, """
        @Hints = [
          "A",
          "B";
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Unbalanced brackets")
        || e.message.contains("bracket")), "Should detect unbalanced brackets");
  }

  @Test
  void testImageNodeIsRejected(@TempDir Path tempDir) throws IOException {
    Path testFile = tempDir.resolve("ImageNode.ui");
    Files.writeString(testFile, """
        Image {
          TexturePath: "Icon.png";
        }
        """);

    UiValidator validator = new UiValidator(tempDir);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Unsupported node type: Image")),
        "Image node type should be rejected");
  }

  @Test
  void testCoreRulesUnknownProperty(@TempDir Path tempDir) throws IOException {
    Path coreRoot = tempDir.resolve("core");
    Files.createDirectories(coreRoot);
    Files.writeString(coreRoot.resolve("Core.ui"), """
        Group { Anchor: (Top: 0); }
        """);

    Path testFile = tempDir.resolve("BadProp.ui");
    Files.writeString(testFile, """
        Group { Border: (Color: #fff); }
        """);

    UiValidator validator = new UiValidator(tempDir, List.of(tempDir), List.of(tempDir), coreRoot);
    List<UiValidator.ValidationError> errors = validator.validateAll();

    assertTrue(errors.stream().anyMatch(e -> e.message.contains("Unknown property 'Border'")),
        "Core rules should flag unknown properties for node type");
  }
}
