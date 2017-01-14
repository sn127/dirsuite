publishTo in Global := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle in Global := true

publishArtifact in Test := false

pomIncludeRepository in Global := { _ => false }

sonatypeProfileName in Global := "fi.sn127"

pomExtra in Global := {
  <url>https://github.com/sn127/utils</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <name>sn127</name>
      <email>dev@sn127.fi</email>
      <organization>SN127</organization>
      <organizationUrl>https://github.com/sn127</organizationUrl>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:https://github.com/sn127/utils.git</connection>
    <developerConnection>scm:git:https://github.com/sn127/utils.git</developerConnection>
    <url>https://github.com/sn127/utils/tree/master</url>
  </scm>
}

