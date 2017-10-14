# Changes

DirSuite project uses [semantic versioning](http://semver.org/).


## Releases

Latest release (for scala 2.12) is:

 * dirsuite: 0.7.0
 * [Upgrade Guide](./UPGRADE.md)

#### 0.7.0

 - fs:
    - fs functionality has been moved to [better-files](https://github.com/pathikrit/better-files).

 - DirSuite (former testing):
    - `fi.sn127.utils.fs` -based file utils are removed and replaced with [better-files](https://github.com/pathikrit/better-files) 
    - API-Changes
        - API-Change:testing:chg: `fi.sn127.utils.fs.{Regex, Glob}` are moved under `fi.sn127.utils.testing` package

 - build:
   - SBT 1.0
   - Scala 2.12.3
 - testing:
   - ScalaTest: 3.0.4
   - better-files: 3.1.0 (new)

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
