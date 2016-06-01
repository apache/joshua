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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for Node class.
 * 
 * @author Lane Schwartz
 * @since 2008-07-09
 * @version $LastChangedDate$
 */
@Test(groups = { "lattice_node" })
public class NodeTest {

  private final int id = 12345;

  private Node<String> node;

  @Test
  public void constructNode() {
    node = new Node<String>(id);
    Assert.assertEquals((int) node.id(), (int) id);
    Assert.assertTrue(node.getOutgoingArcs().isEmpty());
    Assert.assertEquals(node.size(), 0);
  }


  @Test(dependsOnMethods = { "constructNode" })
  public void getNumber() {

    Assert.assertEquals(node.getNumber(), id);

  }


  @Test(dependsOnMethods = { "constructNode" })
  public void toStringTest() {

    Assert.assertEquals(node.toString(), "Node-"+id);

  }


  @Test(dependsOnMethods = { "constructNode" })
  public void addArc() {

    Node<String> n2 = new Node<String>(2);
    float w2 = (float) 0.123;
    String l2 = "somthing cool";

    Node<String> n3 = new Node<String>(3);
    float w3 = (float) 124.78;
    String l3 = "hurray!";

    Node<String> n4 = new Node<String>(4);
    float w4 = (float) Double.POSITIVE_INFINITY;
    String l4 = "\u0000";

    Assert.assertEquals(node.size(), 0);

    node.addArc(n2,(float) w2, l2);
    Assert.assertEquals(node.size(), 1);
    Arc<String> a2 = node.getOutgoingArcs().get(0);
    Assert.assertEquals(a2.getHead(), n2);
    Assert.assertEquals(a2.getTail(), node);
    Assert.assertEquals(a2.getCost(), w2);
    Assert.assertEquals(a2.getLabel(), l2);

    node.addArc(n3,(float) w3, l3);
    Assert.assertEquals(node.size(), 2);
    Arc<String> a3 = node.getOutgoingArcs().get(1);
    Assert.assertEquals(a3.getHead(), n3);
    Assert.assertEquals(a3.getTail(), node);
    Assert.assertEquals(a3.getCost(), w3);
    Assert.assertEquals(a3.getLabel(), l3);

    node.addArc(n4, (float) w4, l4);
    Assert.assertEquals(node.size(), 3);
    Arc<String> a4 = node.getOutgoingArcs().get(2);
    Assert.assertEquals(a4.getHead(), n4);
    Assert.assertEquals(a4.getTail(), node);
    Assert.assertEquals(a4.getCost(), w4);
    Assert.assertEquals(a4.getLabel(), l4);

  }
}
