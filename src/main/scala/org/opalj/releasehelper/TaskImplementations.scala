package org.opalj.releasehelper

import sbt.Keys._
import sbt._
import sbtrelease.{ExtraReleaseCommands, Version, versionFormatError}
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStateTransformations.{commitReleaseVersion, setNextVersion, setReleaseVersion}
import org.opalj.releasehelper.ReleaseHelperPlugin.autoImport._

import scala.collection.mutable

trait TaskImplementations {

  def noopStep: StateTransitionProcess = identity[State] _
  def notImplementedYet: StateTransitionProcess = (state: State) => scala.sys.error("This release step is not implemented yet")

//  import sbtrelease.Utilities._ // implicit syntax to work with state API
//  import StateTransitionProcess.processFromStep

  def pauseStep: StateTransitionProcess = (state: State) => {
    SimpleReader.readLine("Proceed with the process? [y/n]: ") match {
      case Some("y") => state
      case Some("n") => println("aborting..."); state.fail
      case Some(omega) => sys.error(s"'$omega' is not a valid choice. exiting...");
      case None => sys.error("Pause is not supported by this process call or terminal!")
    }
  }
  def releaseStepCleanall: StateTransitionProcess = releaseStepCommand("cleanAll")
  def stepGitChecks: StateTransitionProcess = releaseStepCommand(ExtraReleaseCommands.initialVcsChecksCommand, "")
  def askForVersions: StateTransitionProcess = inquireVersions
  def parseVersions: StateTransitionProcess = (state: State) => {
    val extracted = Project.extract(state)
    val currentV = extracted.get(version)
    val useDefs = state.get(useDefaults).getOrElse(false)
    val cmdlineReleaseVersion = state.get(commandLineReleaseVersion).flatten
    val cmdlineNextVersion = state.get(commandLineNextVersion).flatten
    val releaseFunc = extracted.runTask(releaseVersion, state)._2
    val nextFunc = extracted.runTask(releaseNextVersion, state)._2
    val suggestedReleaseV = releaseFunc(currentV)
    val releaseV = if (cmdlineReleaseVersion.isDefined) {
      cmdlineReleaseVersion.get
    } else if(useDefs) {
      suggestedReleaseV
    } else {
      scala.sys.error("The release version was no set. Maybe specify 'release-version' or 'with-defaults' on the command line?")
    }
    val suggestedNextV = nextFunc(releaseV)
    val nextV = if (cmdlineNextVersion.isDefined) {
      cmdlineNextVersion.get
    } else if(useDefs) {
      suggestedNextV
    } else {
      scala.sys.error("The next version was no set. Maybe specify 'release-version' or 'with-defaults' on the command line?")
    }
    state.put(versions, (releaseV, nextV))
  }

  def stepGitChecksForMerge(sourceBranch: String, targetBranch: String): StateTransitionProcess = notImplementedYet
  def stepGitMergeInto(sourceBranch: String, targetBranch: String): StateTransitionProcess = notImplementedYet

  def stepSetAssertionsTo(assertionsOn: Boolean): StateTransitionProcess = ReleaseStep(
    action = st => {
      def addOrNot(assertionsOn: Boolean): Seq[String] = if(assertionsOn) Seq() else Seq("-Xdisable-assertions")
      reapply(Seq(
        scalacOptions in ThisBuild := ( (scalacOptions in ThisBuild).value.filterNot(_.equals("-Xdisable-assertions")) ++ addOrNot(assertionsOn) )
      ), st)
    }
  )

  def runAllTests: StateTransitionProcess = ReleaseStep(
    action = { st: State =>
      if (!st.get(skipTests).getOrElse(false)) {
        val extracted = Project.extract(st)
        val ref = extracted.get(thisProjectRef)
        extracted.runAggregated(test in Test in ref, st)
        extracted.runAggregated(test in IntegrationTest in ref, st)
      } else st
    }
  ) //TODO: unify with plugin task

  def stepCommitReleaseVersion: StateTransitionProcess = commitReleaseVersion
  def stepSetReleaseVersion: StateTransitionProcess = setReleaseVersion

  def setReleaseVersionInMD: StateTransitionProcess = notImplementedYet

  def publishSignedStep: StateTransitionProcess = releaseStepTask(releasePublishArtifactsAction)

  def releaseSonatypeStep: StateTransitionProcess = notImplementedYet
  def releaseWebsiteStep: StateTransitionProcess = releaseStepTask(rhWebpageUpload)
  def uploadScaladocStep: StateTransitionProcess = notImplementedYet
  def uploadDisassemblerToBitbucketStep: StateTransitionProcess = notImplementedYet
  def updateMyOpalProjectStep: StateTransitionProcess = notImplementedYet

  def updateBugPickerStep: StateTransitionProcess = notImplementedYet
  def updateAtomPluginStep: StateTransitionProcess = notImplementedYet

  def mergeBackToDevelopStep: StateTransitionProcess = notImplementedYet

  def printProjectInfo : StateTransitionProcess = releaseStepTask(rhPrintProjectInfo)

}

object ImplPure extends TaskImplementations {}
object ImplWithAnnounce extends TaskImplementationsWithAnnounce {}
object ImplJustMock extends TaskImplementationsJustMocked {}

trait TaskImplementationsWithAnnounce extends TaskImplementations {
  override def releaseStepCleanall = super.releaseStepCleanall.announces("Step: cleanAll")
  override def stepGitChecks = super.stepGitChecks.announces("Step: Git Status checks")
  override def askForVersions = super.askForVersions.announces("Step: Parses versions from the command line or uses defaults. Asks the user for versions in interacrive mode if necessary")
  override def parseVersions = super.parseVersions.announces("Step: Parses versions from the command line or uses defaults. No interaction.")
  override def stepGitChecksForMerge(sourceBranch: String, targetBranch: String) = super.stepGitChecksForMerge(sourceBranch, targetBranch).announces(s"Step: Git: Check dynamically for merge $sourceBranch into $targetBranch")
  override def stepGitMergeInto(sourceBranch: String, targetBranch: String) = super.stepGitMergeInto(sourceBranch, targetBranch).announces(s"Step: Git: Merge $sourceBranch into $targetBranch")
  override def stepSetAssertionsTo(assertionsOn: Boolean) = super.stepSetAssertionsTo(assertionsOn).announces(s"Step: change scalac options to compile assertions to ($assertionsOn)")
  override def runAllTests = super.runAllTests.announces("Step: test + it:test")
  override def stepCommitReleaseVersion = super.stepCommitReleaseVersion.announces("Step: Git: stage and commit")
  override def stepSetReleaseVersion = super.stepSetReleaseVersion.announces("Step: write the release version to disk .sbt file")
  override def setReleaseVersionInMD = super.setReleaseVersionInMD.announces("change the release version in website markdown files")
  override def publishSignedStep = super.publishSignedStep.announces("Step: publishSigned")
  override def releaseSonatypeStep = super.releaseSonatypeStep.announces("Step: publish to sonatype step")
  override def releaseWebsiteStep = super.releaseWebsiteStep.announces("Step: upload the generated website")
  override def uploadScaladocStep = super.uploadScaladocStep.announces("Step: upload scaladoc")
  override def uploadDisassemblerToBitbucketStep = super.uploadDisassemblerToBitbucketStep.announces("Step: upload to bitbucket")
  override def updateMyOpalProjectStep = super.updateMyOpalProjectStep.announces("Step: update MyOpalProject dependency version")
  override def updateBugPickerStep = super.updateBugPickerStep.announces("Step: update BuckPicker dependency version")
  override def updateAtomPluginStep = super.updateAtomPluginStep.announces("Step: update Atom plugin step")
  override def mergeBackToDevelopStep = super.mergeBackToDevelopStep.announces("Step: Git: merge back into develop")
  override def printProjectInfo = super.printProjectInfo.announces("Prints debug information about the current project")
}

trait TaskImplementationsJustMocked extends TaskImplementations {
  override def releaseStepCleanall = super.releaseStepCleanall.mock("Step: cleanAll")
  override def stepGitChecks = super.stepGitChecks.mock("Step: Git Status checks")
  override def askForVersions = super.askForVersions.mock("Step: Asks the user for versions if interactive")
  override def parseVersions = super.parseVersions.announces("Step: Parses versions from the command line or uses defaults. No interaction.")
  override def stepGitChecksForMerge(sourceBranch: String, targetBranch: String) = super.stepGitChecksForMerge(sourceBranch, targetBranch).mock(s"Step: Git: Check dynamically for merge $sourceBranch into $targetBranch")
  override def stepGitMergeInto(sourceBranch: String, targetBranch: String) = super.stepGitMergeInto(sourceBranch, targetBranch).mock(s"Step: Git: Merge $sourceBranch into $targetBranch")
  override def stepSetAssertionsTo(assertionsOn: Boolean) = super.stepSetAssertionsTo(assertionsOn).mock(s"Step: change scalac options to compile assertions to ($assertionsOn)")
  override def runAllTests = super.runAllTests.mock("Step: test + it:test")
  override def stepCommitReleaseVersion = super.stepCommitReleaseVersion.mock("Step: Git: stage and commit")
  override def stepSetReleaseVersion = super.stepSetReleaseVersion.mock("Step: write the release version to disk .sbt file")
  override def setReleaseVersionInMD = super.setReleaseVersionInMD.mock("change the release version in website markdown files")
  override def publishSignedStep = super.publishSignedStep.mock("Step: publishSigned")
  override def releaseSonatypeStep = super.releaseSonatypeStep.mock("Step: publish to sonatype step")
  override def releaseWebsiteStep = super.releaseWebsiteStep.mock("Step: upload the generated website")
  override def uploadScaladocStep = super.uploadScaladocStep.mock("Step: upload scaladoc")
  override def uploadDisassemblerToBitbucketStep = super.uploadDisassemblerToBitbucketStep.mock("Step: upload to bitbucket")
  override def updateMyOpalProjectStep = super.updateMyOpalProjectStep.mock("Step: update MyOpalProject dependency version")
  override def updateBugPickerStep = super.updateBugPickerStep.mock("Step: update BuckPicker dependency version")
  override def updateAtomPluginStep = super.updateAtomPluginStep.mock("Step: update Atom plugin step")
  override def mergeBackToDevelopStep = super.mergeBackToDevelopStep.mock("Step: Git: merge back into develop")
  override def printProjectInfo = super.printProjectInfo.mock("Prints debug information about the current project")
}

