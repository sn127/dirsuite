[![Build Status](https://travis-ci.org/sn127/dirsuite.svg?branch=master)](https://travis-ci.org/sn127/dirsuite)
[![Coverage Status](https://coveralls.io/repos/github/sn127/dirsuite/badge.svg?branch=master)](https://coveralls.io/github/sn127/dirsuite?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fi.sn127/dirsuite_2.12/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/fi.sn127/dirsuite_2.12)
[![Javadocs](http://javadoc.io/badge/fi.sn127/dirsuite_2.12.svg?color=blue)](http://javadoc.io/doc/fi.sn127/dirsuite_2.12)

# DirSuite test framework and add-on for ScalaTest


DirSuite add-on to ScalaTest. DirSuite can run (huge) collection of tests which are defined on filesystem. 
Arguments, inputs and expected reference output files are defined by test directory.


## Documentation

 * [docs/dirsuite.md](./docs/dirsuite.md) has general information about dirsuite and how to use it
 * [docs/howto.md](./docs/howto.md) has links and explanation to example project and it's test cases.
 * Example test setup with DirSuite:
   * [tests](./examples/tests/) Example test corpus
   * [DirSuiteDemo](./examples/src/test/scala/DirSuiteDemo.scala) Demo setup against example test corpus.
 * [Tackler Project](https://github.com/sn127/tackler) has extensive set of tests based on DirSuite
   * [Tackler's dirsuite tests](https://github.com/sn127/tackler/tree/stable/tests)
   * [Tackler's dirsuite clean up setup](https://github.com/sn127/tackler/blob/stable/project/TacklerTests.scala)

## Releases

Release artifacts are published on the maven 
[Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22fi.sn127%22).

Library configuration for sbt:

    libraryDependencies += "fi.sn127" %% "dirsuite" % "0.7.0"

For release information and version history details, see [CHANGELOG](./CHANGELOG.md). For Upgrade instructions,
see [UPGRADE](./UPGRADE.md) .


## Contributing to DirSuite

Contributions to the project are most welcome. Please see 
[CONTRIBUTING](./CONTRIBUTING.md) how you could help. 

Your pull requests can be merged only if you can certify 
the [Developer Certificate of Origin (DCO), Version 1.1](./DCO). 
To certify DCO (e.g. sign-off your commit), you must add 
a `Signed-off-by` line to **every**  git commit message 
(e.g. `git commit -s`):

    Signed-off-by: github-account <your.real@email.address>

If you set your `user.name` and `user.email` in git configs,
then git will include that line for you with `git commit -s`. 
These settings can be done per repository basis, 
so they don't have be global settings in your system. 
 
Please make sure that you sign-off all your PR's commits. 


## Credits

See [THANKS](./THANKS.md) for full list of credits. Obviously 
without [ScalaTest](http://www.scalatest.org/) this project 
would not exists.


## License

    Copyright 2016-2017 SN127.fi Contributors
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    