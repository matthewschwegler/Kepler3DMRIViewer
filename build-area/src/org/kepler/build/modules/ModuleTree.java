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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class that represents a tree of modules
 *
 * @author welker
 */
public class ModuleTree implements Iterable<Module>
{
    private Node root;
    private Set<Module> moduleSet = new HashSet<Module>();
    private List<Module> resolved = new ArrayList<Module>();
    static private ModuleTree instance;

    /**
     * constructor used by the singleton instance
     */
    private ModuleTree()
    {
        root = new Node();
        populate(root);
        String currentSuiteName = ModuleUtil.getCurrentSuiteName();
        if( !currentSuiteName.equals("unknown") )
        {
            Node firstChild = root.getFirstChild();
            if( firstChild != null )
            {
                String firstModuleName = firstChild.getModule().getName();
                Module currentSuite = Module.make(currentSuiteName);
                if(!currentSuite.getName().equals(firstModuleName) )
                {
                    root.addToFront(Module.make(currentSuiteName));
                }
            }
            else
            {
                root.addChild(Module.make(currentSuiteName));
            }

        }
    }

    /**
     * create a new ModuleTree from a modulesTxt object
     */
    public ModuleTree(ModulesTxt modulesTxt)
    {
        root = new Node(modulesTxt);
        populate(root);
    }

    /**
     * return a singleton instance of this class
     */
    public static ModuleTree instance()
    {
        if (instance == null)
        {
            init();
        }
        return instance;
    }

    /**
     * initialize this class
     */
    public static void init()
    {
        ModulesTxt.init();
        //XXX why call read() when init() above has already done so?
        ModulesTxt.instance().read();
        instance = new ModuleTree();
    }

    /**
     * get a list of modules
     *
     * @return
     */
    public List<Module> getModuleList()
    {
        resolve();
        return resolved;
    }

    /**
     * return the modulesTxt object used to build this tree
     */
    public ModulesTxt getModulesTxt()
    {
        return root.getModule().getModulesTxt();
    }

    private List<Module> missingModules = new ArrayList<Module>();

    /**
     * get any missing modules
     *
     * @return
     */
    public List<Module> getMissingModules()
    {
        return missingModules;
    }

    /**
     * return true if all modules in the tree are present
     *
     * @return
     */
    public boolean allModulesPresent()
    {
        missingModules.clear();
        for (Module m : this)
        {
            if (!m.getDir().isDirectory())
            {
                missingModules.add(m);
            }
        }
        return missingModules.size() == 0;
    }

    /**
     * return the module that has highest priority
     */
    public Module getHighestPriorityModule()
    {
        return root.getFirstChild().getModule();
    }

    /**
     * return a list of all modules that have a lower priority than the given
     * module
     */
    public List<Module> getLowerPriorityModules(Module module)
    {
        resolve();
        int moduleIndex = resolved.indexOf(module);
        List<Module> lowerPriorityModules = new ArrayList<Module>();
        for (int i = moduleIndex + 1; i < resolved.size(); i++)
        {
            lowerPriorityModules.add(resolved.get(i));
        }
        return lowerPriorityModules;
    }

    /**
     * return an iterator of a reverse traversal of the tree
     */
    public Iterable<Module> reverse()
    {
        return new Iterable<Module>()
        {
            public Iterator<Module> iterator()
            {
                return reverseHelper();
            }
        };
    }

    /**
     * return an iterator over the tree
     */
    public Iterator<Module> iterator()
    {
        resolve();
        return resolved.iterator();
    }

    /**
     * resolve all modules in the tree
     */
    public void resolve()
    {
        if (alreadyResolved())
        {
            return;
        }
        for (Module module : preorderTraversal())
        {
            resolved.add(module);
        }
    }

    /**
     * return an iterator of a preorder traversal of the tree
     */
    public Iterable<Module> preorderTraversal()
    {
        return new Iterable<Module>()
        {
            public Iterator<Module> iterator()
            {
                return preorderTraversalHelper();
            }

        };
    }

    /**
     * return true if the tree contains a module with the given name
     */
    public boolean contains(String moduleName)
    {
        for (Module module : this)
        {
            if (module.getName().equals(moduleName))
            {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Module module)
    {
        return contains(module.getName());
    }

    /**
     * get the first module in the tree
     */
    public Module getFirst()
    {
        resolve();
        return resolved.get(0);
    }

    /**
     * return true if the first module starts with moduleName
     */
    public boolean firstStartsWith(String moduleName)
    {
        resolve();
        if (resolved.size() < 1)
        {
            return false;
        }
        return resolved.get(0).getName().startsWith(moduleName);
    }

    /**
     * return the module with the given name, return null if the module
     * does not exist.
     */
    public Module getModule(String name)
    {
        for (Module module : this)
        {
            if (module.getName().equals(name))
            {
                return module;
            }
        }
        return null;
    }

    /**
     * return the module with the given stem name, return null if the module
     * does not exist.
     */
    public Module getModuleByStemName(String stemName)
    {
        for (Module module : this)
        {
            if (module.getStemName().equals(stemName))
            {
                return module;
            }
        }
        return null;
    }

    /**
     * return a string representation of this module tree
     */
    public String toString()
    {
        String result = "==ModulesTree==\n";
        for (Module module : this)
        {
            result += module.writeString();
        }
        return result;
    }

    /** Get an MD5 string unique to this suite. */
    public String getModuleConfigurationMD5() throws Exception
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");            
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new Exception("Error accessing MD5: " + e.getMessage(), e);
        }
        
        for(Module module : getModuleList()) {
            digest.update(module.getName().getBytes());
        }
         
        byte[] md5sum = digest.digest();
         
        //XXX necessary?
        BigInteger bigInt = new BigInteger(1, md5sum);
        String retval = bigInt.toString(16);
                
        // make sure it's 32 chars
        while(retval.length() < 32)
        {
            retval = "0" + retval;
        }
        
        return retval;
    }

    /**
     * check to see if a module is compatible with the current OS
     */
    private boolean isCompatibleWithCurrentOS(Module module)
    {
        return OSRegistryTxt.isCompatibleWithCurrentOS(module);
    }

    /**
     * return an iterator of a preorder traversal of the tree
     */
    private Iterator<Module> preorderTraversalHelper()
    {
        return new Iterator<Module>()
        {
            Node current = root;

            public boolean hasNext()
            {
                return !current.isLeaf() || current.hasRightSibling() || !current.isRoot() && current.getParent().hasRightSibling();
            }

            public Module next()
            {
                if (!current.isLeaf())
                {
                    current = current.getFirstChild();
                }
                else if (current.hasRightSibling())
                {
                    current = current.getRightSibling();
                }
                else
                {
                    current = current.getParent().getRightSibling();
                }
                return current.getModule();
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * check to see if the tree has already been resolved.
     */
    private boolean alreadyResolved()
    {
        return resolved.size() > 0;
    }

    /**
     * return an iterator of a reverse traversal of the tree
     */
    private Iterator<Module> reverseHelper()
    {
        List<Module> reversed = new ArrayList<Module>();
        for (Module module : this)
        {
            reversed.add(module);
        }
        Collections.reverse(reversed);
        return reversed.iterator();
    }

    /**
     * populate the tree
     */
    private void populate(Node node)
    {
        if (!node.getModule().isSuite())
        {
            return;
        }
        Suite suite = node.getModule().asSuite();
        if (!suite.getModulesTxt().exists())
        {
            return;
        }
        suite.getModulesTxt().read();
        for (Module module : suite)
        {
            if (moduleSet.contains(module))
            {
                continue;
            }
            if (!isCompatibleWithCurrentOS(module))
            {
                continue;
            }
            moduleSet.add(module);
            Node child = node.addChild(module);
            if (module.isSuite())
            {
                populate(child);
            }
        }
    }
}
