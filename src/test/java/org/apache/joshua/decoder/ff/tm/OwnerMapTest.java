package org.apache.joshua.decoder.ff.tm;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
public class OwnerMapTest {
  
  @BeforeMethod
  public void setUp() throws Exception {
    OwnerMap.clear();
  }
  
  @AfterMethod
  public void tearDown() throws Exception {
    OwnerMap.clear();
  }
  
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void given_invalidId_thenThrowsException() {
    OwnerMap.getOwner(new OwnerId(3));
  }
  
  @Test
  public void givenOwner_whenRegisteringOwner_thenMappingIsCorrect() {
    // GIVEN
    String owner = "owner";
    
    // WHEN
    OwnerId id = OwnerMap.register(owner);
    OwnerId id2 = OwnerMap.register(owner);
    
    // THEN
    assertEquals(id, id2);
    assertEquals(owner, OwnerMap.getOwner(id));
  }

}
