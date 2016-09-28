package org.apache.joshua.decoder;

import org.apache.joshua.decoder.ff.lm.KenLM;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kellen Sunderland
 */
public class LanguageModelStateManager {

  private Map<UUID, LmPool> languageModelPoolMapping = new HashMap<>();

  public LmPool getStatePool(UUID languageModelId, KenLM languageModel) {
    LmPool statePool = languageModelPoolMapping.get(languageModelId);
    if (statePool == null) {
      statePool = languageModel.createLMPool();
      languageModelPoolMapping.put(languageModelId, statePool);
    }
    return statePool;
  }

  public void clearStatePool() {
    languageModelPoolMapping.values().forEach(LmPool::close);
    languageModelPoolMapping.clear();
  }
}
