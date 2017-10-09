# Changes

Utils-fs and utils-testing projects use [semantic versioning](http://semver.org/).


## Releases

Latest releases (for scala 2.12) are:

 * utils-fs: 0.8.0
 * utils-testing: 0.6.0

#### fs:0.X.0, testing:0.Y.0

 - build:
   - SBT 1.0
   - Scala 2.12.3
 - fs:
 - testing:
   - ScalaTest: 3.0.4


#### fs:0.8.0, testing:0.6.0

 - fs:
   - no changes
 - testing:
    - strict exec-file format
    - new `runDualAssertionDirSuiteTestCase`s for asserting multistep failures
    - API-Changes
      - API-Change:testing:chg: runDirSuite -> runDirSuiteTestCases
      - API-Change:testing:chg: ignoreDirSuite -> ignoreDirSuiteTestCases
      - API-Change:testing:new: runDualAssertionDirSuiteTestCases
      - API-Change:testing:new: ignoreDualAssertionDirSuiteTestCases
      - API-Change:testing:chg: registerDirSuiteTest
      - API-Change:testing:new: getTestCases
      - API-Change:testing:chg: testExecutor
      - API-Change:testing:chg: exec file format
      - API-Change:testing:new: tokenizer, new function
      - API-Change:testing:chg: execParser => parseExec
      - API-Change:testing:chg: argsMapping => mapArgs
      - API-Change:testing:chg: getOutput => mapOutput


#### fs:0.8.0, testing:0.5.1

 - fs:
   - no changes
 - testing: 
   - bug fix: test case failure (test always failed) 
     this happened when multistep exec produced multiple, 
     different output items and only one output per exec step



#### fs:0.8.0, testing:0.5.0

First public release
