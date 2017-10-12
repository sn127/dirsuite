# How to use DirSuite

Under examples are three demo DirSuites, which demonstrates 
different aspects of DirSuite. These are runnable projects,
just run `sbt test` on `examples`-directory.

 * [test cases](../examples/tests/) Test cases which are used by examples. 

 * [DirSuiteDemo](../examples/src/test/scala/DirSuiteDemo.scala)
   Example of typical usage of DirSuite

 * [MapArgsDemo](../examples/src/test/scala/MapArgsDemo.scala)
   Example of how to map and change test's arguments by DirSuite test
   (e.g. to provide extra arguments for conf file in actual test code). 

 * [FailureDemo](../examples/src/test/scala/FailureDemo.scala)
   Example how to ignore tests and examples of actual error messages when tests fail.
   These ignored tests will all fail,  if you enable them by changing 
   `ignore` prefix to `run` prefix.


