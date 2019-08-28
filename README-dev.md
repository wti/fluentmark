# Interim developer notes

The original project appears to be abandoned, though the update site still works.

This fork is my attempt to fix some bugs.  
The target platform was not defined or complete, so I removed some functionality
(UML/dot output, conversion by commonmark, et. al.).

Steps to build:

1. I'm using mvn 3.0+ and Java 1.8.
2. Run `mvn -f mirror-pom.xml` to mirror the update site to `repository`
3. Run `mvn package` should complete successfully.

To configure Eclipse for development:
1. I'm using 2019 with PDE support
2. Open the tp.target file, and set it as the target platform (first change the path /Users/wes/... - change to your location)
3. Import the plugin/ folder for the editor.

You should see no compiler errors or warnings.

Note that although tycho (the command-line build) and eclipse/m2e should
be able to configure the project, I found it not to work, so I configured
the .classpath and .project file directly.


This fork is in alpha state and has not been tested.

