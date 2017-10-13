sonatypeProfileName := "fi.sn127"

publishMavenStyle := true

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/sn127/dirsuite"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/sn127/dirsuite"),
    "scm:git:https://github.com/sn127/dirsuite.git"
  )
)

developers := List(
  Developer(id="sn127", name="SN127", email="dev@sn127.fi", url=url("https://github.com/sn127"))
)

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
