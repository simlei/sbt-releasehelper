package org.opalj.releasehelper

import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, releaseStepTask}
import sbtrelease.ReleaseStateTransformations.setNextVersion


object Processes {
  import org.opalj.releasehelper.ImplJustMock._

  val fullReleaseProcessSeq: Seq[StateTransitionProcess] = Seq[StateTransitionProcess](
    releaseStepCleanall,
    stepGitChecks,            // releaseStepCommand(initialVcsChecksCommand), // TODO: just warn if fails?
    askForVersions,

    //TODO: - skip develop integration if current branch is already develop
    //      - just display warning if no diff to develop
    stepGitChecksForMerge("WIP", "develop"), //TODO: configure WIP branch
    stepGitMergeInto("WIP", "develop"),
    runAllTests, //TODO: check success or failure: https://stackoverflow.com/questions/16057378/executing-a-task-dependency-for-test-failure-in-sbt

    //TODO: - skip master integration if current branch is already master
    //      - just display warning if no diff
    stepSetAssertionsTo(true), // to master?
    stepGitChecksForMerge("develop", "master"),
    stepGitMergeInto("develop", "master"),
    stepCommitReleaseVersion,

    stepSetReleaseVersion,
    setReleaseVersionInMD, //TODO implement. markers + regex with .md files?

    stepSetAssertionsTo(false), // assertions on
    releaseStepCleanall,
    runAllTests,

    publishSignedStep,

    releaseSonatypeStep,
    releaseWebsiteStep,
    uploadScaladocStep,
    uploadDisassemblerToBitbucketStep,
    updateMyOpalProjectStep,
    // check if sbt-perf should be updated
    updateBugPickerStep,
    updateAtomPluginStep,

    mergeBackToDevelopStep,
    setNextVersion,
    // turn on assertions (`build.sbt`) - no need if no tests follow?
    publishSignedStep
  ) // release a new snapshot build to ensure that the snapshot is always younger

  val testProcessSeq: Seq[StateTransitionProcess] = Seq(
    releaseStepCleanall,
    askForVersions, //inquireVersions
    stepSetAssertionsTo(true), // assertions on
    printProjectInfo,
    stepSetAssertionsTo(false), // assertions off
    printProjectInfo
  )

  val testProcessSeqBugpicker: Seq[StateTransitionProcess] = Seq(
    parseVersions, // noninteracive if command line version is provided
    printProjectInfo
  )

}
