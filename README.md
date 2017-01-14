[![Build Status](https://travis-ci.org/sn127/utils.svg?branch=master)](https://travis-ci.org/sn127/utils)
[![Coverage Status](https://coveralls.io/repos/github/sn127/utils/badge.svg?branch=master)](https://coveralls.io/github/sn127/utils?branch=master)

# Utils

 * utils-fs
   - java.nio.{Files,Paths} path manipulating utilities
   - Recursive findFiles with glob and regex pattern
 * utils-testing
   - Scalatest-like `DirSuite` to run collection of tests with
     predefined input and output references on filesystem.


## Documentation

 * [docs/dirsuite.md](./docs/dirsuite.md) has some general information about dirsuite
 * [docs/howto.md](./docs/howto.md) has examples against dirsuite's own tests

## Contributing to Utils

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


## Thanks

See [THANKS](./THANKS.md) for full list of credits. Most notably 
this started as a test tool for [Abandon](https://github.com/hrj/abandon) 
and it grew from there to be something generally useful. 

Obviously without [ScalaTest](http://www.scalatest.org/) this project 
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
    
