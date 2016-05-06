build-system/README.txt
$Id: README.txt 30828 2012-10-05 22:33:22Z jianwu $

The latest version of quick instructions on how to build Kepler using
the Kepler Build System can be found at
https://dev.kepler-project.org/developers/teams/build/systems/build-system/extension-build-system

More extensive documentation is available at:
https://dev.kepler-project.org/developers/teams/build/documentation/the-new-build-system
                                          
A possibly out-of-date version of the quick instructions is reproduced below:

Building Kepler
===============

Assumptions
-----------
   1. You are running Java 1.6. To test this assumption type: "java -version"
   2. You are using Ant 1.7.1. To test this assumption type: "ant -version"
   3. You have installed an SVN client. To test this assumption type: "svn --version"

Getting Started
---------------

Decide whether you would like to work off the development trunk
of Kepler, a Kepler-2.x branch, or the Kepler-1.0 branch.

* To work off the development trunk run these commands:

mkdir kepler
cd kepler
svn co https://code.kepler-project.org/code/kepler/trunk/modules/build-area
cd  build-area
ant change-to -Dsuite=kepler
ant run

* To work off a Kepler-2.x branch (working off the latest is recommended):

mkdir kepler
cd kepler
svn co https://code.kepler-project.org/code/kepler/branches/releases/release-branches/build-area-2.x build-area
cd build-area
ant change-to -Dsuite=kepler-2.x
ant run

* To work off the Kepler-1.0 branch (not recommended):

mkdir kepler
cd kepler
svn co https://code.kepler-project.org/code/kepler/trunk/modules/build-area
cd build-area
ant change-to -Dsuite=kepler-1.0
ant run



How to Contribute Your Own Code
-------------------------------

You need to make your own suite along with whatever source modules you
would like to contribute.

Example:

I want to add source code to extent the functionality of Kepler,
including overriding existing classes in Kepler or Ptolemy. I need a
source module to store this code. I also need to make a suite that
tells the build system how this module relates to Kepler and Ptolemy
and other modules. Let us call the module we will make foo-module and
the suite we will make foo-suite. Hopefully, you will choose better
names for your code.

Type the following commands:

ant make-suite -Dname=foo-suite

This makes a suite called foo-suite in the modules area as a peer to
kepler-1.0-jar-tag and loader. Edit the following file:

../foo-suite/module-info/modules.txt so that it has the following content:

foo-module
loader
kepler-1.0-jar-tag

Next, make the foo-module where you can put your source:

ant make-module -Dname=foo-module

Put any source code you would like to in the src folder of the foo-module.

Sharing Your Code with Other Developers
---------------------------------------

Now that you have made a suite and a module with source, you would
probably like to share it with others. You can upload it to our
repository. That is easy enough:

ant upload -Dmodule=foo-suite
ant upload -Dmodule=foo-module

Now, other developer can get your suite after they download the build
by issuing the following command:

ant change-to -Dsuite=foo-suite

Finally, please note that you can store any modules in your own
Subversion repository instead of our if you like. Just append the
Subversion URL right after the module name in modules.txt like the
following fictional example:

foo-module     svn://pantara.genomecenter.ucdavis.edu/extensions/foo-module/trunk
loader
kepler-1.0-jar-tag

While loader and kepler-1.0-jar-tag will retrieved from appropriate
places in the Kepler repository at
https://code.kepler-project.org/code/kepler the foo-module will be
stored at the url specified.

Sharing Your Work with Scientists
---------------------------------

So, you have developed a useful scientific workflow and an associated
source code in foo-module and would now like to share this work with
scientists. How can you go about doing that? One solution is to use
the package command to create a zip which in turn will contain an
executable jar that a scientist can double click on to execute your
project just as you do whenever you type in ant run. Just type:

 

ant package -Dsuite=foo-suite

This will create a foo-suite.zip file in the kepler.build
directory. When this file is unzipped, it will contain a foo-suite.jar
that can be double-clicked to run Kepler along with your extension.

 
Using an IDE to Develop Source Code
-----------------------------------

The Extension Build System supports three major IDEs: Intellij IDEA,
Eclipse, and Netbeans. To generate project files for these IDEs and a
particular configuration of modules as specified in modules.txt, try
one of the following commands as appropriate:

ant idea
ant eclipse
ant netbeans


