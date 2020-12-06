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

## misc history
### Install issues
- install using site/target/..zip, not by pointing at site directory
- build fail 1: no Arrays class, when no JAVA-HOME or it points to java 11+
	- workaround: point to legacy (non-module) java-8 as JAVA_HOME and with PATH
- build working fine, but install fails looking for spellchecker and org.eclipse.epp...
- reinstalled eclipse, removed ~/.eclipse, added certiv update site: http://www.certiv.net/updates
	- note local mirror should manage certiv.  Unclear what happens if update site goes down.

### hyperlink detection, Oct 2020
- bug is in URLHyperlinkDetector
- FluentSimpleSourceViewerConfiguration.java
- FluentSimpleSourceViewer.java
- fluentmark build not working
    - backed up to 1.8 JAVA_HOME
    - require spellcheck 3.3.2
    - plugin is in repo mirror, but not in feature/artifact metadata
    - also no local installable units added - not resolving local p2 repo?
    - better after changing manifest to permit 3.3.1 spellchecker
- fluentmark compile not working: missing Shell?
    - net/certiv/fluentmark/editor/text/rules/IndentedCodeRule.java:
    - The type ..Shell is indirectly referenced from required .class files
    - swt -> wrong environment parameters?
    - working after reverting to photon repo

