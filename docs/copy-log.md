# Repositories 

Upstream: https://github.com/hrj/abandon 
Fork: https://github.com/jaa127/abandon


## New FileUtils code

Based on code: 
  internal commit: adc0facabe219a55a87b292414e951269bfd49a3

on repo (git rev-list --parents HEAD | tail -1):
  9dc2c5058c74f352d9af0e20285a55aabbe38545


## New Testrunner code

Based on code:
  internal commit: cb432a64f9327439b0a44d7b9e18e64656aed5bc

on repo (git rev-list --parents HEAD | tail -1):
  9dc2c5058c74f352d9af0e20285a55aabbe38545



### Orininal versions


#### Utils.scala

  https://github.com/jaa127/abandon/blob/08bedac1552ce63d064dd5081f82b32778e22879/base/src/main/scala/co/uproot/abandon/Utils.scala

  git co 08bedac1552ce63d064dd5081f82b32778e22879

  cp base/src/main/scala/co/uproot/abandon/Utils.scala \
     ../utils/fs/src/main/scala/fi/sn127/utils/fs/


#### GlobTest.scala

  https://github.com/jaa127/abandon/blob/a0d69f8716364eacfd6c6324cb1732e4bce478a3/base/src/test/scala/co/uproot/abandon/GlobTest.scala

  git co a0d69f8716364eacfd6c6324cb1732e4bce478a3

  cp base/src/test/scala/co/uproot/abandon/GlobTest.scala \
     ../utils/fs/src/main/scala/fi/sn127/utils/fs/


#### globtree

  https://github.com/jaa127/abandon/tree/741356b8a09a3fad89c7834101b2a80e8a1aef52/testCases/globtree

  git co 741356b8a09a3fad89c7834101b2a80e8a1aef52
  cp -a testCases/globtree/ \
        ../utils/tests/


#### CliTestRunner.scala and TestComparator.scala

  https://github.com/jaa127/abandon/tree/ffb104b71ccc40dadcec37da6b51b74bf2755a05/cli/src/test/scala/co/uproot/abandon

  git co ffb104b71ccc40dadcec37da6b51b74bf2755a05

  cp cli/src/test/scala/co/uproot/abandon/CliTestRunner.scala \
     ../utils/testing/src/main/scala/fi/sn127/utils/testing/

  cp cli/src/test/scala/co/uproot/abandon/TestComparator.scala \
     ../utils/testing/src/main/scala/fi/sn127/utils/testing/

