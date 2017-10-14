# Upgrade guide

This is upgrade guide for `dirsuite` and former `utils-fs` and `utils-testing` packages.


## Upgrade from utils-fs and utils-testing to dirsuite

### utils-fs

Utils-fs component has been removed and its functionality has been migrated to
[better-files](https://github.com/pathikrit/better-files).


### utils-testing

Utils-testing has been renamed to dirsuite.


#### Dependencies

New dependency is:

    libraryDependencies += "fi.sn127" %% "dirsuite" % "0.7.0"

This replaces `utils-fs` and `utils-testing` dependencies, which are not needed anymore.


#### API-changes

Classes `fi.sn127.utils.fs.{Regex, Glob}` are moved to `fi.sn127.utils.testing` package. 

```diff
-import fi.sn127.utils.fs.Glob
-import fi.sn127.utils.fs.Regex
+import fi.sn127.utils.testing.Glob
+import fi.sn127.utils.testing.Regex
```

This API-change affects DirSuite test code.
