jboss-modules-dependency-verifier
=================================

To run
$ mvn packageexec:java -Dexec.mainClass="org.jboss.modules.ModuleValidator" -Dexec.args=$JBOSS_HOME/modules


The output is CSV data containing:
* the module name
* the class which cannot see a class dependency
* the class dependency

For example the following means that org.apache.cxf.frontend.blueprint.SimpleBPNamespaceHandler
refers to org.apache.aries.blueprint.ParserContext in it's bytecode, but the module does not let it see that.
  org.apache.cxf.impl:main,org/apache/cxf/frontend/blueprint/SimpleBPNamespaceHandler,org.apache.aries.blueprint.ParserContext
