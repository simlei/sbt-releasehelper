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

import java.io.File

import sbt.{State}
import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep}

/**
  * wraps state transformations that are the essential building blocks of a release process.
  * Most of the credit goes to eed3si9n (Eugene Yokota) of sbt-release on which this builds
  *
  * @author Simon Leischnig
  */
trait StateTransitionProcess {
  def stateTransformation(state: State): State
  def staticCheck(state: State): State
  def toReleasePluginStep: ReleaseStep = ReleaseStep(
    stateTransformation,
    staticCheck
  )
}
object StateTransitionProcess {
  // decorating "DSL", implicits for plugin sbt-release interop

  implicit def processFromStep(step: ReleaseStep): StateTransitionProcess = ProcessStepL(step.action, step.check)
  implicit def stepFromProcess(process: StateTransitionProcess): ReleaseStep = process.toReleasePluginStep
  implicit def func2Process(f: State => State): StateTransitionProcess = ReleaseStep(f)
  implicit def Process2Func(rp: StateTransitionProcess): State=>State = rp.stateTransformation

  implicit def decoratingStep(step: ReleaseStep): StateTransitionDecorator = new StateTransitionDecorator(step)
  implicit def decoratingProcess(process: StateTransitionProcess): StateTransitionDecorator = new StateTransitionDecorator(process)


  class StateTransitionDecorator(base: StateTransitionProcess) {
    def printBefore(msg: String)(f: State => State): State => State = {
      f.compose{ state: State =>
        println(msg)
        state
      }
    }

    def announces(msg: String): StateTransitionProcess = {
      val tf = printBefore(s"Executing step: $msg")(base.stateTransformation)
      val check = printBefore(s"Static checking for planned step: $msg")(base.staticCheck)
      ProcessStepL(tf, check)
    }

    def mock(msg: String) = {
      val tf = printBefore(s"[just mocked, no effects] Executing Step: $msg")(identity)
      val check = printBefore(s"[just mocked, no effects] Static checks for: $msg")(identity)
      ProcessStepL(tf, check)
    }
  }
}

// "State => State" lambda describing a process step on the local project
case class ProcessStepL(f: State => State, staticCheckF: State => State = identity) extends StateTransitionProcess {
  override def staticCheck(state: State): State = staticCheckF(state)
  override def stateTransformation(state: State): State = f(state)
}
case class ProcessStepExternal(cmd: String, project: File) extends StateTransitionProcess {
  def checkIsSbtProject(project: File) = {
    project.listFiles().filter(_.getName.endsWith(".sbt")).size > 0 //TODO: better check if it's a sbt project
  }
  override def staticCheck(state: State): State = {
    if(! project.exists() || ! project.isDirectory|| !checkIsSbtProject(project)) {
      scala.sys.error(s"Folder $project for external command execution must be a valid sbt project directory.")
    }
    state
  }
  override def stateTransformation(state: State): State = {
    import sys.process._
    val sbtcmd = "sbt \"" + cmd + "\""
    println("Executing SBT command: " + sbtcmd)
    Utils.errorPredicate("sbt process for another project failed on the command > $cmd")(Process(sbtcmd, project).! == 0) // TODO: can we do better?
    state
  }
}


trait StatePredicate {
  def name: String
  def apply(s: State): Boolean
}
object StatePredicate {
  implicit def fromFunction(f: State => Boolean) = StatePredicateL(f)

  case class StatePredicateL(f: State => Boolean) extends StatePredicate {
    private[this] val id = scala.util.Random.nextInt()
    override def apply(s: State) = f(s)
    override def name: String = s"generic state predicate lambda $id"
  }
}