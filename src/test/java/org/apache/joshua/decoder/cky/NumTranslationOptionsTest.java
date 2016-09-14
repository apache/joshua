package org.apache.joshua.decoder.cky;

import static org.apache.joshua.decoder.cky.TestUtil.decodeList;
import static org.apache.joshua.decoder.cky.TestUtil.loadStringsFromFile;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests that num_translation_options is enforced for hierarchical decoders
 */
public class NumTranslationOptionsTest {
  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }

  @Test
  public void givenInput_whenDecodingWithNumTranslationOptions3_thenScoreAndTranslationCorrect()
      throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/input");

    // When
    configureDecoder("src/test/resources/decoder/num_translation_options/joshua.config", true);
    List<String> decodedStrings = decodeList(inputStrings, decoder, joshuaConfig);

    // Then
    List<String> goldStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/output.gold");
    assertEquals(decodedStrings, goldStrings);
  }

  @Test
  public void givenInput_whenDecodingWithNumTranslationOptions3AndNoDotChart_thenScoreAndTranslationCorrect()
      throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/input");

    // When
    configureDecoder("src/test/resources/decoder/num_translation_options/joshua.config", false);
    List<String> decodedStrings = decodeList(inputStrings, decoder, joshuaConfig);

    // Then
    List<String> goldStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/output-no-dot-chart.gold");
    assertEquals(decodedStrings, goldStrings);
  }

  @Test
  public void givenInput_whenDecodingWithNumTranslationOptions3AndPacked_thenScoreAndTranslationCorrect()
      throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/input");

    // When
    configureDecoder("src/test/resources/decoder/num_translation_options/joshua-packed.config",
        true);
    List<String> decodedStrings = decodeList(inputStrings, decoder, joshuaConfig);

    // Then
    List<String> goldStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/output-packed.gold");
    assertEquals(decodedStrings, goldStrings);
  }

  public void configureDecoder(String pathToConfig, boolean useDotChart) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(pathToConfig);
    joshuaConfig.use_dot_chart = useDotChart;
    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
  }
}
