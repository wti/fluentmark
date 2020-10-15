# Fork developer notes

The developer of the original project appears unresponsive, 
though the update site still works.

This fork is my attempt to fix some bugs.  
The target platform was not defined or complete, so I removed some functionality
(UML/dot output, conversion by commonmark, et. al.).

To build from the command-line:

1. I'm using mvn 3.0+ and Java 1.8.
2. Run `mvn -f mirror-pom.xml package` to mirror the update site to `repository`
3. Then `mvn package` should complete successfully.

TODO: problems with these instructions - some local repo fixup?

To configure Eclipse IDE for development:
1. I'm using 2019 with PDE support
2. Open the tp.target file in Eclipse, and set it as the target platform (first change the path `/Users/wes/git-os/...` to your location).
3. Import the plugin/ folder for the editor.

You should see no compiler errors or warnings.

Eclipse/m2e did not auto-configure the project for me, so I configured
and checked in the .classpath and .project file directly. 

This fork is in alpha state.

