package org.apache.joshua.decoder;

import org.apache.joshua.decoder.ff.lm.KenLM;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kellen Sunderland
 */
public class LanguageModelStateManager {

  private Map<UUID, KenLMPool> languageModelPoolMapping = new HashMap<>();

  public KenLMPool getStatePool(UUID languageModelId, KenLM languageModel) {
    KenLMPool statePool = languageModelPoolMapping.get(languageModelId);
    if (statePool == null) {
      statePool = languageModel.createLMPool();
      languageModelPoolMapping.put(languageModelId, statePool);
    }
    return statePool;
  }

  public void clearStatePool() {
    languageModelPoolMapping.values().forEach(KenLMPool::close);
    languageModelPoolMapping.clear();
  }
}
