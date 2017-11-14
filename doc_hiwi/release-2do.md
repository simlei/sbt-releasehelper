# Creating an OPAL Release


## Necessary Steps
 1. integrate everything into the develop branch
 assertions on!
 1. run tests and integration tests
 1. migrate everything to the master branch
 1. update version number in `build.sbt`
 1. update version information in `src/index.md`
 1. turn off assertions in `local.sbt`
 1. run tests and integration tests (to ensure that everything works if we have no assertions)
 1. publish to maven (`sbt publishedSigned`)
 1. go to Sonatype to release the build
 1. upload the new webpage to www.opal-project.de
 1. upload the generated ScalaDoc
 1. upload the latest version of the OPAL-Disassembler to BitBucket
 1. update *MyOPALProject* using the latest released version of OPAL
 1. // force the recreation of the OPAL Docker Container

## Optional Steps
 1. check if sbt-perf should be updated
 1. update BugPicker
 1. update OPAL-Disassembler ATOM Plug-in

# Preparing the next release
 1. merge changes from master back to develop (in particular version information)
 1. update version information (`build.sbt`) (x.y.z-SNAPSHOT)
 1. turn on assertions (`build.sbt`)
 1. release a new snapshot build to ensure that the snapshot is always younger than the last release (`sbt publishSigned`)


 SBTR) sbt-release:
  1. Check that the working directory is a git repository and the repository has no outstanding changes. Also prints the hash of the last commit to the console.
  2. If there are any snapshot dependencies, ask the user whether to continue or not (default: no).
  3. Ask the user for the `release version` and the `next development version`. Sensible defaults are provided.
  4. run `clean`
  5. Run `test:test`, if any test fails, the release process is aborted.
  6. Write `version in ThisBuild := "$releaseVersion"` to the file `version.sbt` and also apply this setting to the current [build state](http://www.scala-sbt.org/release/docs/Build-State.html).
  7. Commit the changes in `version.sbt`.
  8. Tag the previous commit with `v$version` (eg. `v1.2`, `v1.2.3`).
  9. Run `publish`.
  10. Write `version in ThisBuild := "nextVersion"` to the file `version.sbt` and also apply this setting to the current build state.
  11. Commit the changes in `version.sbt`

  Merged:
    ## Necessary Steps
  1. integrate everything into the develop branch
  assertions on!
  2. clean, run tests and integration tests
  3. migrate everything to the master branch
  4. update version number in `build.sbt`
  5. update version information in `src/index.md`
  6. turn off assertions in `local.sbt`
  7. clean, run tests and integration tests (to ensure that everything works if we have no assertions)
  8. publish to maven (`sbt publishedSigned`)
  9. go to Sonatype to release the build
  10. upload the new webpage to www.opal-project.de
  11. upload the generated ScalaDoc
  12. upload the latest version of the OPAL-Disassembler to BitBucket
  13. update *MyOPALProject* using the latest released version of OPAL
  14. // force the recreation of the OPAL Docker Container

