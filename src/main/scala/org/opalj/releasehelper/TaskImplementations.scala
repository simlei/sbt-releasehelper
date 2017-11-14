/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.releasehelper

import sbt.Keys._
import sbt._
import sbtrelease.{ExtraReleaseCommands, Version, versionFormatError}
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStateTransformations.{commitReleaseVersion, setNextVersion, setReleaseVersion}
import org.opalj.releasehelper.ReleaseHelperPlugin.autoImport._

/**
  * The implementations of single release process steps
  *
  * @author Simon Leischnig
  */
trait TaskImplementations {

  def noopStep: StateTransitionProcess = identity[State] _
  def notImplementedYetStep: StateTransitionProcess = (state: State) => scala.sys.error("This release step is not implemented yet")

//  import sbtrelease.Utilities._ // implicit syntax to work with state API
//  import StateTransitionProcess.processFromStep

  def pauseStep: StateTransitionProcess = (state: State) => {
    SimpleReader.readLine("Proceed with the process? [y/n]: ") match {
      case Some("y") => state
      case Some("n") => sys.error("Aborting process.")
      case Some(omega) => sys.error(s"'$omega' is not a valid choice. exiting...");
      case None => sys.error("Pause is not supported by this process call or terminal!")
    }
  }
  def cleanallStep: StateTransitionProcess = releaseStepCommand("cleanAll")
  def gitStatusCheck: StateTransitionProcess = releaseStepCommand(ExtraReleaseCommands.initialVcsChecksCommand, "")
  def gitMergeCheck(sourceBranch: String, targetBranch: String): StateTransitionProcess = notImplementedYetStep
  def gitMergeStep(sourceBranch: String, targetBranch: String): StateTransitionProcess = notImplementedYetStep
  def versionsReadoutPossiblyInteractive: StateTransitionProcess = inquireVersions
  def versionsReadoutNeverInteractive: StateTransitionProcess = (state: State) => {
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


  def assertionsInScalacStep(assertionsOn: Boolean): StateTransitionProcess = ReleaseStep(
    action = st => {
      def addOrNot(assertionsOn: Boolean): Seq[String] = if(assertionsOn) Seq() else Seq("-Xdisable-assertions")
      reapply(Seq(
        scalacOptions in ThisBuild := ( (scalacOptions in ThisBuild).value.filterNot(_.equals("-Xdisable-assertions")) ++ addOrNot(assertionsOn) )
      ), st)
    }
  )

  def allTestsStep: StateTransitionProcess = ReleaseStep(
    action = { st: State =>
      if (!st.get(skipTests).getOrElse(false)) {
        val extracted = Project.extract(st)
        val ref = extracted.get(thisProjectRef)
        extracted.runAggregated(test in Test in ref, st)
        extracted.runAggregated(test in IntegrationTest in ref, st)
      } else st
    }
  ) //TODO: unify with plugin task

  def gitCommitReleaseVersion: StateTransitionProcess = commitReleaseVersion
  def versionReleaseReificationStep: StateTransitionProcess = setReleaseVersion

  def versionReleaseReflectInWebsiteMarkdownStep: StateTransitionProcess = notImplementedYetStep
  def versionReleaseReflectInMyOpalProjectStep: StateTransitionProcess = notImplementedYetStep
  def versionReleaseReflectInBugpickerStep: StateTransitionProcess = notImplementedYetStep
  def versionReleaseReflectInAtomPluginStep: StateTransitionProcess = notImplementedYetStep

  def publishStep: StateTransitionProcess = releaseStepTask(releasePublishArtifactsAction)

  def sonatypeReleaseStep: StateTransitionProcess = notImplementedYetStep
  def websiteReleaseStep: StateTransitionProcess = releaseStepTask(rhWebpageUpload)
  def scaladocReleaseStep: StateTransitionProcess = notImplementedYetStep
  def disassemblerBitbucketStep: StateTransitionProcess = notImplementedYetStep

  def gitMergeBackToDevelopStep: StateTransitionProcess = notImplementedYetStep

  def printProjectInfo : StateTransitionProcess = releaseStepTask(rhPrintProjectInfo)

}

object ImplPure extends TaskImplementations {}
object ImplWithAnnounce extends TaskImplementationsWithAnnounce {}
object ImplJustMock extends TaskImplementationsJustMocked {}

trait TaskImplementationsWithAnnounce extends TaskImplementations {
  override def cleanallStep = super.cleanallStep.announces("Step: cleanAll")
  override def allTestsStep = super.allTestsStep.announces("Step: test + it:test")
  override def publishStep = super.publishStep.announces("Step: publishSigned")
  override def gitStatusCheck = super.gitStatusCheck.announces("Step: Git Status checks")
  override def gitMergeCheck(sourceBranch: String, targetBranch: String) = super.gitMergeCheck(sourceBranch, targetBranch).announces(s"Step: Git: Check dynamically for merge $sourceBranch into $targetBranch")
  override def gitMergeStep(sourceBranch: String, targetBranch: String) = super.gitMergeStep(sourceBranch, targetBranch).announces(s"Step: Git: Merge $sourceBranch into $targetBranch")
  override def gitCommitReleaseVersion = super.gitCommitReleaseVersion.announces("Step: Git: stage and commit")
  override def gitMergeBackToDevelopStep = super.gitMergeBackToDevelopStep.announces("Step: Git: merge back into develop")
  override def versionsReadoutPossiblyInteractive = super.versionsReadoutPossiblyInteractive.announces("Step: Parses versions from the command line or uses defaults. Asks the user for versions in interacrive mode if necessary")
  override def versionsReadoutNeverInteractive = super.versionsReadoutNeverInteractive.announces("Step: Parses versions from the command line or uses defaults. No interaction.")
  override def assertionsInScalacStep(assertionsOn: Boolean) = super.assertionsInScalacStep(assertionsOn).announces(s"Step: change scalac options to compile assertions to ($assertionsOn)")
  override def versionReleaseReificationStep = super.versionReleaseReificationStep.announces("Step: write the release version to disk .sbt file")
  override def versionReleaseReflectInWebsiteMarkdownStep = super.versionReleaseReflectInWebsiteMarkdownStep.announces("change the release version in website markdown files")
  override def versionReleaseReflectInMyOpalProjectStep = super.versionReleaseReflectInMyOpalProjectStep.announces("Step: update MyOpalProject dependency version")
  override def versionReleaseReflectInBugpickerStep = super.versionReleaseReflectInBugpickerStep.announces("Step: update BuckPicker dependency version")
  override def versionReleaseReflectInAtomPluginStep = super.versionReleaseReflectInAtomPluginStep.announces("Step: update Atom plugin step")
  override def sonatypeReleaseStep = super.sonatypeReleaseStep.announces("Step: publish to sonatype step")
  override def websiteReleaseStep = super.websiteReleaseStep.announces("Step: upload the generated website")
  override def scaladocReleaseStep = super.scaladocReleaseStep.announces("Step: upload scaladoc")
  override def disassemblerBitbucketStep = super.disassemblerBitbucketStep.announces("Step: upload to bitbucket")
  override def printProjectInfo = super.printProjectInfo.announces("Prints debug information about the current project")
}

trait TaskImplementationsJustMocked extends TaskImplementations {
  override def cleanallStep = super.cleanallStep.mock("Step: cleanAll")
  override def allTestsStep = super.allTestsStep.mock("Step: test + it:test")
  override def publishStep = super.publishStep.mock("Step: publishSigned")
  override def gitStatusCheck = super.gitStatusCheck.mock("Step: Git Status checks")
  override def gitMergeCheck(sourceBranch: String, targetBranch: String) = super.gitMergeCheck(sourceBranch, targetBranch).mock(s"Step: Git: Check dynamically for merge $sourceBranch into $targetBranch")
  override def gitMergeStep(sourceBranch: String, targetBranch: String) = super.gitMergeStep(sourceBranch, targetBranch).mock(s"Step: Git: Merge $sourceBranch into $targetBranch")
  override def gitCommitReleaseVersion = super.gitCommitReleaseVersion.mock("Step: Git: stage and commit")
  override def gitMergeBackToDevelopStep = super.gitMergeBackToDevelopStep.mock("Step: Git: merge back into develop")
  override def versionsReadoutPossiblyInteractive = super.versionsReadoutPossiblyInteractive.mock("Step: Asks the user for versions if interactive")
  override def versionsReadoutNeverInteractive = super.versionsReadoutNeverInteractive.announces("Step: Parses versions from the command line or uses defaults. No interaction.")
  override def assertionsInScalacStep(assertionsOn: Boolean) = super.assertionsInScalacStep(assertionsOn).mock(s"Step: change scalac options to compile assertions to ($assertionsOn)")
  override def versionReleaseReificationStep = super.versionReleaseReificationStep.mock("Step: write the release version to disk .sbt file")
  override def versionReleaseReflectInWebsiteMarkdownStep = super.versionReleaseReflectInWebsiteMarkdownStep.mock("change the release version in website markdown files")
  override def versionReleaseReflectInMyOpalProjectStep = super.versionReleaseReflectInMyOpalProjectStep.mock("Step: update MyOpalProject dependency version")
  override def versionReleaseReflectInBugpickerStep = super.versionReleaseReflectInBugpickerStep.mock("Step: update BuckPicker dependency version")
  override def versionReleaseReflectInAtomPluginStep = super.versionReleaseReflectInAtomPluginStep.mock("Step: update Atom plugin step")
  override def sonatypeReleaseStep = super.sonatypeReleaseStep.mock("Step: publish to sonatype step")
  override def websiteReleaseStep = super.websiteReleaseStep.mock("Step: upload the generated website")
  override def scaladocReleaseStep = super.scaladocReleaseStep.mock("Step: upload scaladoc")
  override def disassemblerBitbucketStep = super.disassemblerBitbucketStep.mock("Step: upload to bitbucket")
  override def printProjectInfo = super.printProjectInfo.mock("Prints debug information about the current project")
}

