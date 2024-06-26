val V = _root_.scalafix.sbt.BuildInfo

addCompilerPlugin(scalafixSemanticdb)
scalacOptions ++= Seq("-Yrangepos", "-Ywarn-unused")
scalaVersion := V.scala212
Compile / compile / scalacOptions ++= Seq(
  // generate errors on procedure syntax
  "-Wconf:cat=deprecation:e",
  // suppress the warning that is coming from IgnoreWarning
  "-Wconf:src=scala/IgnoreWarning.scala:s",
  "-Xfuture",
  "-deprecation"
)
TaskKey[Unit]("checkLastCompilationCached") := {
  val str = streams.value
  val thisProject = thisProjectRef.value

  // readText expects a fully resolved scope (no This)
  val compileIncScope = Scope(
    Select(thisProject),
    Select(Compile),
    Select(compileIncremental.key),
    Zero
  )
  val reader = str.readText(streamScopedKey(compileIncScope), None)
  val lines = IO.readLines(reader)

  val cachedCompilationLogEntry = "No changes"
  if (lines.forall(line => !line.contains(cachedCompilationLogEntry))) {
    lines.foreach(line => str.log.error(line))
    throw new RuntimeException("last compilation was not fully cached")
  }
}

// https://github.com/sbt/sbt/commit/dbb47b3ce822ff7ec25881dadd71a3b29e202273
// must be outside a macro to workaround "Illegal dynamic reference: Def"
def streamScopedKey(scope: Scope) = Def.ScopedKey(scope, Keys.streams.key)
