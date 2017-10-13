# DirSuite

DirSuite is an add-on to ScalaTest which makes it possible to run and test 
corpus of tests which are defined in as files on directory tree.

Test interface is simple. Test cases are define as special `exec-file`.
Exec-files describes how many times your test function is executed 
per test, and what are the arguments for each execution step.

Your testFunction must take arguments as an array of strings.

Return values, possibly thrown exceptions etc. can be defined 
and validated (asserted) by using normal
[ScalaTest's assertions](http://www.scalatest.org/user_guide/using_assertions). 

DirSuite also supports automatic validation of potential output.
Currently supported formats are txt and xml-output, but there are 
extension points, where it is possible to add more 
different kind of Validators.

It is possible to run multiple execution steps per one test case.

For example, test case for `jgit commit` operation could look 
something like that:

  `init`, `add file.txt`, `commit -m "msg"`

if test function is jgit's main method.


## Runnable Example Project

There are three demo DirSuite-setups under `examples`.

These are runnable examples, which demonstrate different aspects of DirSuite:

 * [DemoApp](../examples/src/main/scala/DemoApp.scala) which is used in tests as an example app. 
   It can count arguments, write arguments to files, and it will fail if told so.
 * [Test corpus](../examples/tests/) actual test case steps (exec-files), inputs and reference outputs are stored here.
 * [DirSuiteDemo](../examples/src/test/scala/DirSuiteDemo.scala)
   This is DirSuite test, which tests "normal" operation of demo app.
 * [MapArgsDemo](../examples/src/test/scala/MapArgsDemo.scala)
   This is DirSuite test, which manipulates test case arguments from DirSuite. This could be used for example
   to provide fixed configuration arguments to the test app.
 * [FailureDemo](../examples/src/test/scala/FailureDemo.scala) These are tests which all will fail.
   Currently all of these are ignored, but if you run them, they will provide example output of failure cases.

### How to run examples?

Install SBT, if you don't have it, and then:

    cd examples
    sbt test


## Example of simple DirSuite test suite

Here is an example of DirSuite test case, which first runs tests which validates
successful operation, and after that check that application fails in expected way 
(with invalid input).


Example of exec-file for success case, this would be located under 
`tests/success/basic01.exec` in actual system:


    # format: exec
    exec:do;something;
    exec:do;more;


Example of exec-file for failure case, this would be located under
`tests/failure/runtime01.exec` in actual system:

    # format: exec
    exec:fail;this;is;stupid;


Convention of `tests/success`, `tests/failure` is not mandated by DirSuite, 
but it would be good idea to group different kind of tests to separate folders.

With above exec-file  setup, we could write following DirSuite test.

    class MyDirSuite extends DirSuiteLike { 

      val testdir = Paths.get("tests").toAbsolutePath.normalize
      
      /*
       * find all test cases which match glob: "tests/" + "success/basic*.exec"
       * add assert that they return success status code.
       */
      runDirSuiteTestCases(testdir, Glob("success/basic*.exec")) { args: Array[String] =>
        assertResult(DemoApp.SUCCESS) {
          DemoApp.doMain(args)
        }
      }

      /*
       * find all test cases wich match glob: "tests/" + "failure/runtimeEx*.exec"
       * and assert that they throw runtime exception
       */
      runDirSuiteTestCases(testdir, Glob("failure/runtimeEx*.exec")) { args: Array[String] =>
        assertThrows[RuntimeException] {
          DemoApp.run(args)
        }
      }
   }


So this would run all `basic*.exec` tests and assert successful run for each basic.*exec steps and test cases 
(if multiple `basic*.exec` files are found).

After that all `runtimeEx*.exec` test will run, and test framework will assert that each execution step will 
throw `RuntimeException`.


### Multistep failure tests

Dirsuite supports also multistep failure mode testing. 

This is a test where first steps must behave one way (succeed), 
and then the last step must behave differently (fail). 

For example there is following Exec-file content:

    # format: exec
    exec:1;do;something;fun;
    exec:2;do;more;fun;
    exec:3;fail;this;is;stupid;

In this configuration, one test case will execute all these steps, 
and first two steps (1-2) must succeed, but then the last, 
step 3, must fail.


Multistep DirSuite configuration for above test case:


    class MyDirSuite extends DirSuiteLike { 

      val testdir = Paths.get("tests").toAbsolutePath.normalize
      
      /*
       * find all test cases wich match glob: "tests/" + "failure/multistep*.exec"
       * and assert that they throw runtime exception
       */
      runDualAssertionDirSuiteTestCases(testdir, Glob("failure/multistep*.exec")) { args: Array[String] =>
        assertResult(DemoApp.SUCCESS) {
          DemoApp.doMain(args)
        } { args: Array[String] =>
          assertThrows[RuntimeException] {
            DemoApp.run(args)
          }
        }
      }
    }


In that case test framework will find all `multistep*.exec` test cases, and then run those tests.

For each test it will assert that first steps (steps 1-2) will be successful and last execution 
step (step 3) will  throw `RuntimeException`.



## Exec file format

Exec file format is following:

    # format: exec
    # first line content must be above, and this line is comment
    # next line is exec step without any arguments
    exec:
    # next step is exec step with one argument "a b c"
    exec:a b c;
    # next step is exec with 3 args "a", "b", "c"
    exec:a;b;c;
    # next step is exec with 3 empty args "", "", ""
    exec:;;;
    # next step is exec with arg: "\"qoute\""
    exec:"qoute";


If exec separator `";"` is not suitable in your situation, 
then it could be easily changed by overriding `getExecArgumentSeparator` method. 
Separator is string, so it could consist of multiple characters, and default `tokenizer` 
implementation will use it with `String::split`.

Example of changing default sepator to `"|"`:

    class MyDirSuite extends DirSuiteLike { 
      // change exec separator to "|"
      override protected 
      def getExecArgumentSeparator: String = "|"
      
      runDirSuiteTestCases(testdir, Glob("barsep/sep[0-9]*.exec")) { args: Array[String] =>
        assertResult(DemoApp.SUCCESS) {
          app.doTxt(args)
        }
      }
    }

If exec arguments must contain new line characters or something more complex,
then exec line tokenizer or the whole exec arg parser can be overridden:

 - `tokenizer` tokenize line after "exec:" to the end of line
 - `parseExec` parses whole exec file

See [API docs](http://javadoc.io/doc/fi.sn127/dirsuite_2.12) or source code for exact arguments and return values for these two.

### Regex or Glob to find exec-files

DirSuite Regex and Glob behaves as Java Regex and Glob, with twist.
 
  * [Java Regex](https://docs.oracle.com/javase/tutorial/essential/regex/index.html)
  * [Java Glob](https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob)

The Twist: DirSuite uses `better-files.glob and regex` to find files.
If pattern is NOT absolute path, and it does not start with Regex/Glob special  character, 
then basepath and pattern are combined together. That means that you don't need wildcard 
matching at the beginning of your pattern. E.g.

    // testdir == "/path/to/some/dir/tests"
    val testdir = Paths.get("tests").toAbsolutePath.normalize

    // this will match
    // "/path/to/some/dir/tests/success/basic*.exec"
    findFiles(testdir, "success/basic*.exec")


## Validating output 

DirSuite can validate automatically output for test case, if reference 
and output files follow certain naming scheme.


### Reference files

Reference files are found automatically based on following logic:

Basename of exec-file is used as test case basename. This basename is
appended with `.ref.*` glob, and this pattern is used 
in testcase's test directory.


    Testcase:   tests/subfolder/test01.exec
    References: tests/subfolder/test01.ref.*

If this logic is not sufficient, you can override `findReferences` method.


### Output files

Expected Output files are found automatically, if files 
conform this naming scheme:

For each reference file, output name is constructed by : 

Basename of exec-file is used as basename. This basename is
prefixed with `out.` and appended with basename. Then uniq part of 
reference file's name is appended to output name. 

Uniq part of reference name is filename with `basename.ref.` 
prefix removed.

Example:

    Testcase:   tests/subfolder/test01.exec
    References: tests/subfolder/test01.ref.*
    
    For one reference:
   	   tests/subfolder/test01.ref.file-1.txt
    Output:
   	   tests/subfolder/out.test01.file-1.txt


If this logic is not sufficient, you can override `mapOutput` method.


### Selecting Validator

Default validator is selected based on reference file extension. 

`TestValidator.txtValidator` will be used  for text files, 
and `TestValidator.xmlValidator` for xml-files.

This validator selection logic and default validators can be replaced by overriding 
`selectValidator` method.


### Final words

Comments, Questions? Open a ticket or stare long and hard [SN127](https://github.com/sn127) icon and send an email. :-)

Happy testing!
