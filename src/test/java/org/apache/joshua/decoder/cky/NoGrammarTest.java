package org.apache.joshua.decoder.cky;

import static org.apache.joshua.decoder.cky.TestUtil.translate;
import static org.testng.Assert.assertEquals;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class NoGrammarTest {

	private static final String INPUT = "those who hurt others hurt themselves";
	private static final String GOLD = "0 ||| those_OOV who_OOV hurt_OOV others_OOV hurt_OOV themselves_OOV ||| tm_glue_0=6.000 ||| 0.000";
	
	private JoshuaConfiguration joshuaConfig = null;
	private Decoder decoder = null;
	
	@BeforeMethod
	public void setUp() throws Exception {
		joshuaConfig = new JoshuaConfiguration();
		joshuaConfig.mark_oovs = true;
		KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
	}

	@AfterMethod
	public void tearDown() throws Exception {
		decoder.cleanUp();
		decoder = null;
	}
	
	@Test
	public void givenInput_whenDecodingWithoutGrammar_thenOutputAllOOV() {
		String output = translate(INPUT, decoder, joshuaConfig);
		assertEquals(output.trim(), GOLD);
	}
	
	
	
	
}
