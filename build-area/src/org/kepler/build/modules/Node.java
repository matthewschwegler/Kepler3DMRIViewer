/*
 * Copyright (c) 2009 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */
package org.kepler.build.modules;

import java.util.ArrayList;
import java.util.List;

/**
 * class to represent a node in the module tree
 *
 * @author berkley
 */
public class Node
{
    private Module module;

    private Node parent;
    private Node left;
    private Node right;
    private List<Node> children = new ArrayList<Node>();

    /**
     * constructor
     */
    public Node()
    {
        module = new Suite("root");
        module.asSuite().setModulesTxt(ModulesTxt.instance());
    }

    /**
     * create a node for a given modulesTxt obj
     *
     * @param modulesTxt
     */
    public Node(ModulesTxt modulesTxt)
    {
        module = new Suite("root");
        module.asSuite().setModulesTxt(modulesTxt);
    }

    /**
     * create a module node
     *
     * @param module
     */
    public Node(Module module)
    {
        this.module = module;
    }

    /**
     * add a child to this
     *
     * @param module
     * @return
     */
    public Node addChild(Module module)
    {
        Node newChild = new Node(module);
        newChild.parent = this;
        Node lastChild = children.size() > 0 ? children.get(children.size() - 1) : null;
        children.add(newChild);
        if (lastChild != null)
        {
            lastChild.right = newChild;
            newChild.left = lastChild;
        }
        return newChild;
    }

    public void addToFront(Module module)
    {
        Node newChild = new Node(module);
        newChild.parent = this;
        Node firstChild = children.size() > 0 ? children.get(0) : null;
        children.add(0, newChild);
        if (firstChild != null)
        {
            newChild.right = firstChild;
            firstChild.left = newChild;
        }
    }

    /**
     * get this node's module
     *
     * @return
     */
    public Module getModule()
    {
        return module;
    }

    /**
     * return true if this is the root
     *
     * @return
     */
    public boolean isRoot()
    {
        return parent == null;
    }

    /**
     * get the parent of this node
     *
     * @return
     */
    public Node getParent()
    {
        return parent;
    }

    /**
     * return true if there is a left sibling in the tree
     *
     * @return
     */
    public boolean hasLeftSibling()
    {
        return left != null;
    }

    /**
     * get the left sib
     *
     * @return
     */
    public Node getLeftSibling()
    {
        return left;
    }

    /**
     * return true if this has a right sib
     *
     * @return
     */
    public boolean hasRightSibling()
    {
        return right != null;
    }

    /**
     * get the right sib
     *
     * @return
     */
    public Node getRightSibling()
    {
        return right;
    }

    /**
     * return true if this is a leaf
     *
     * @return
     */
    public boolean isLeaf()
    {
        return children.size() == 0;
    }

    /**
     * get the first child of this
     *
     * @return
     */
    public Node getFirstChild()
    {
        return isLeaf() ? null : children.get(0);
    }

    /**
     * get the last child of this
     *
     * @return
     */
    public Node getLastChild()
    {
        return isLeaf() ? null : children.get(children.size() - 1);
    }

    /**
     * return a string rep of this
     */
    public String toString()
    {
        return module.name;
    }
}
