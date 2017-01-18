# DirSuite

DirSuite is an add-on to ScalaTest, and it makes  possible to run and test 
corpus of tests which are defined in as files on directory tree.

Test interface is simple, your testFunction takes as an input 
an array of strings.  Return values, exceptions etc. you can define
normally by using ScalaTest's assertions. 

It is possible to run multiple execution steps per one test case, 
e.g. for testing git session, one test could be: 

`init`, `add file.txt`, `commit -m "msg"`

if test function is git's main method.

DirSuite also supports automatic validation of potential output.
Currently supported formats are txt and xml-output.


### Examples

Under examples are three demo DirSuites, which demonstrates 
different aspects of DirSuite:

 * [DemoApp](../examples/src/main/scala/DemoApp.scala) which is used in tests

 * [Test corpus](../examples/tests/) contains test inputs

 * [DirSuiteDemo](../examples/src/test/scala/DirSuiteDemo.scala)
   Normal usage of DirSuite

 * [MapArgsDemo](../examples/src/test/scala/MapArgsDemo.scala)
   Howto map and change test's arguments (e.g. to provide conf file)

 * [FailureDemo](../examples/src/test/scala/FailureDemo.scala)
   Ignored tests which all will fail, and provide example output of failurecases.


