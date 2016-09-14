package org.apache.joshua.decoder;

import org.apache.joshua.decoder.segment_file.Sentence;

public class TranslationRequest {
  
  private final Sentence sentence;
  private final DecoderConfig decoderConfig;
  
  public TranslationRequest(final Sentence sentence, final DecoderConfig decoderConfig) {
    this.sentence = sentence;
    this.decoderConfig = decoderConfig;
  }

  public Sentence getSentence() {
    return sentence;
  }

  public DecoderConfig getDecoderConfig() {
    return decoderConfig;
  }
  

}
