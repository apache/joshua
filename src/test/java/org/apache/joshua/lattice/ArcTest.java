/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.lattice;

import org.apache.joshua.lattice.Arc;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for Arc class.
 * 
 * @author Lane Schwartz
 * @since 2008-07-09
 * @version $LastChangedDate$
 */
@Test(groups = { "lattice_arc" })
public class ArcTest {

  private final Node<String> head = new Node<String>(1);
  private final Node<String> tail = new Node<String>(2);
  private final float cost = (float) Math.PI;
  private final String label = "pi";

  private Arc<String> arc;

  @Test(dependsOnMethods = { "org.apache.joshua.lattice.NodeTest.constructNode" })
  //@Test(dependsOnGroups = {"lattice_node" })
  public void constructArc() {

    arc = new Arc<String>(tail, head, (float)cost, label);

    Assert.assertEquals(arc.getHead(), head);
    Assert.assertEquals(arc.getTail(), tail);
    Assert.assertEquals(arc.getCost(), cost);
    Assert.assertEquals(arc.getLabel(), label);

  }

  @Test(dependsOnMethods = { "constructArc" })
  public void getHead() {

    Assert.assertEquals(arc.getHead(), head);

  }


  @Test(dependsOnMethods = { "constructArc" })
  public void getTail() {

    Assert.assertEquals(arc.getTail(), tail);

  }


  @Test(dependsOnMethods = { "constructArc" })
  public void getCost() {

    Assert.assertEquals(arc.getCost(), cost);

  }


  @Test(dependsOnMethods = { "constructArc" })
  public void getLabel() {

    Assert.assertEquals(arc.getLabel(), label);

  }
}
