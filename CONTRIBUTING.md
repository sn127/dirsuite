# Contributors Guide

Contributions to the project are most welcome!

## How to build and test

 1. You do have sbt, don't you? If not, get it from here:
    
    [http://www.scala-sbt.org/](http://www.scala-sbt.org/)
    
 2. Get source code
 
    `git clone https://github.com/sn127/dirsuite.git`

    Then make an empty directory for tests:
     ```
     cd dirsuite
     mkdir tests/globtree/empty
     ```
 
 3. Build and test 
 
    `sbt clean test`
 
 4. Generate test coverage reports
    ```
    sbt clean coverage test coverageReport
    sbt coverageAggregate
    ```

 5. Explore code
    
    Directory structure follows maven's layout:
          
    * [DirSuite](./)
      - DirSuite main code: [./src/main/scala/fi/sn127/utils/testing](./src/main/scala/fi/sn127/utils/testing)
      - DirSuite tests: [./src/test/scala/fi/sn127/utils/testing](./src/test/scala/fi/sn127/utils/testing)
        - [DirSuiteDemo](./src/test/scala/fi/sn127/utils/testing/DirSuiteDemo.scala) is demo how to use DirSuite
        - [DirSuiteLikeTest](./src/test/scala/fi/sn127/utils/testing/DirSuiteLikeTest.scala) 
          and the rest of tests are DirSuite's own tests. 
          These tests are a bit convoluted,  because they are testing test-framework itself.
    * [tests](./tests) Common test target for both projects 
      - [globree](./tests/globtree) is used for path matching tests
      - [dirsuite](./tests/dirsuite) is DirSuite test collection, which is used to test DirSuite 

## Developer Certificate of Origin 

Your pull requests can be merged only if you can certify 
the [Developer Certificate of Origin (DCO), Version 1.1](../DCO). 
To certify DCO (e.g. sign-off your commit), you must add 
a `Signed-off-by` line to **every**  git commit message 
(e.g. `git commit -s`):

    Signed-off-by: github-account <your.real@email.address>

If you set your `user.name` and `user.email` in git configs,
then git will include that line for you with `git commit -s`. 
These settings can be done per repository basis, 
so they don't have be global settings in your system. 
 
Please make sure that you sign-off all your PR's commits. 
