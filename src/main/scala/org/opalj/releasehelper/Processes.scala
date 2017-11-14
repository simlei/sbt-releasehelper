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

import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, releaseStepTask}
import sbtrelease.ReleaseStateTransformations.setNextVersion

/**
  * A composition of the "full" release process and of smaller sample release processes and sandbox examples
  * Most of the credit goes to eed3si9n (Eugene Yokota) of sbt-release on which this builds
  *
  * @author Simon Leischnig
  */
object Processes {
  import org.opalj.releasehelper.ImplJustMock._

  val fullReleaseProcessSeq: Seq[StateTransitionProcess] = Seq[StateTransitionProcess](
    cleanallStep,
    gitStatusCheck,            // releaseStepCommand(initialVcsChecksCommand), // TODO: just warn if fails?
    versionsReadoutPossiblyInteractive,

    //TODO: - skip develop integration if current branch is already develop
    //      - just display warning if no diff to develop
    gitMergeCheck("WIP", "develop"), //TODO: configure WIP branch
    gitMergeStep("WIP", "develop"),
    allTestsStep, //TODO: check success or failure: https://stackoverflow.com/questions/16057378/executing-a-task-dependency-for-test-failure-in-sbt

    //TODO: - skip master integration if current branch is already master
    //      - just display warning if no diff
    assertionsInScalacStep(true), // to master?
    gitMergeCheck("develop", "master"),
    gitMergeStep("develop", "master"),
    gitCommitReleaseVersion,

    versionReleaseReificationStep,
    versionReleaseReflectInWebsiteMarkdownStep, //TODO implement. markers + regex with .md files?

    assertionsInScalacStep(false), // assertions on
    cleanallStep,
    allTestsStep,

    publishStep,

    sonatypeReleaseStep,
    websiteReleaseStep,
    scaladocReleaseStep,
    disassemblerBitbucketStep,
    versionReleaseReflectInMyOpalProjectStep,
    // check if sbt-perf should be updated
    versionReleaseReflectInBugpickerStep,
    versionReleaseReflectInAtomPluginStep,

    gitMergeBackToDevelopStep,
    setNextVersion,
    // turn on assertions (`build.sbt`) - no need if no tests follow?
    publishStep
  ) // release a new snapshot build to ensure that the snapshot is always younger

  val testProcessSeq: Seq[StateTransitionProcess] = Seq(
    cleanallStep,
    versionsReadoutPossiblyInteractive, //inquireVersions
    assertionsInScalacStep(true), // assertions on
    printProjectInfo,
    assertionsInScalacStep(false), // assertions off
    printProjectInfo
  )

  val testProcessSeqBugpicker: Seq[StateTransitionProcess] = Seq(
    versionsReadoutNeverInteractive, // noninteracive if command line version is provided
    printProjectInfo
  )

}
