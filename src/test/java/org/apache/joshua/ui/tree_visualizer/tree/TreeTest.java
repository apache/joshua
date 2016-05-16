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
package org.apache.joshua.ui.tree_visualizer.tree;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TreeTest {
  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void ctor_EmptyString_IllegalArgument() {
    Tree tree = new Tree("");
    Assert.assertEquals(tree.size(), 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void ctor_TooFewCloseParens_IllegalArgument() {
    Tree tree = new Tree("(A{0-1} foo");
    Assert.assertEquals(tree.size(), 0);
  }

  @Test
  public void simpleTree_correctSize() {
    Tree tree = new Tree("(A{0-1} foo)");
    Assert.assertEquals(tree.size(), 2);
  }

  @Test
  public void simpleTree_correctRoot() {
    Tree tree = new Tree("(A{0-1} foo)");
    Tree.Node root = tree.root();
    Assert.assertEquals(root.label(), "A");
    Assert.assertEquals(root.sourceStartIndex(), 0);
    Assert.assertEquals(root.sourceEndIndex(), 1);
    Assert.assertEquals(root.children().size(), 1);
  }

  @Test
  public void simpleTree_correctLeaf() {
    Tree tree = new Tree("(A{0-1} foo)");
    Tree.Node leaf = tree.root().children().get(0);
    Assert.assertEquals(leaf.label(), "foo");
    Assert.assertEquals(leaf.sourceStartIndex(), -1);
    Assert.assertEquals(leaf.sourceEndIndex(), -1);
    Assert.assertEquals(leaf.children().size(), 0);
  }

  @Test
  public void simpleTree_toString() {
    Tree tree = new Tree("(A{0-1} foo)");
    Assert.assertEquals(tree.toString(), "(A{0-1} foo)");
  }

  @Test
  public void trickyTree_children() {
    Tree tree = new Tree("(A{0-2} foo (B{1-2} bar))");
    List<Tree.Node> children = tree.root().children();
    Assert.assertEquals(children.size(), 2);
    Tree.Node foo = children.get(0);
    Assert.assertEquals(foo.label(), "foo");
    Assert.assertTrue(foo.isLeaf());
    Assert.assertEquals(foo.sourceStartIndex(), -1);
    Assert.assertEquals(foo.sourceEndIndex(), -1);
    Tree.Node b = children.get(1);
    Assert.assertEquals(b.label(), "B");
    Assert.assertEquals(b.children().size(), 1);
    Assert.assertFalse(b.isLeaf());
    Assert.assertEquals(b.sourceStartIndex(), 1);
    Assert.assertEquals(b.sourceEndIndex(), 2);
  }

  @Test
  public void SourceStartComparator() {
    Tree tree = new Tree("(A{0-2} foo (B{1-2} bar))");
    Tree.Node a = tree.root();
    Tree.Node b = a.children().get(1);
    Tree.NodeSourceStartComparator cmp = new Tree.NodeSourceStartComparator();
    Assert.assertTrue(cmp.compare(a, b) < 0);
  }

  @Test
  public void SourceStartComparator_LeafSmallerThanAllInternals() {
    Tree tree = new Tree("(A{0-2} foo (B{1-2} bar))");
    Tree.Node a = tree.root();
    Tree.Node foo = a.children().get(0);
    Tree.Node b = a.children().get(1);
    Tree.Node bar = b.children().get(0);
    Tree.NodeSourceStartComparator cmp = new Tree.NodeSourceStartComparator();
    Assert.assertTrue(cmp.compare(foo, a) < 0);
    Assert.assertTrue(cmp.compare(foo, b) < 0);
    Assert.assertTrue(cmp.compare(bar, a) < 0);
    Assert.assertTrue(cmp.compare(bar, b) < 0);
  }
}
