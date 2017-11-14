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

import sbt.{AutoPlugin, Def, _}
import sbt.Keys._
import java.io.File

import org.opalj.releasehelper.transport.Transport
import sbtrelease.ReleasePlugin


/**
  * Plugin to help with the release process of OPAL.
  * In this object there are all keys and settings that have been modified from the base plugin "sbt-release"
  *
  * Most of the credit goes to eed3si9n (Eugene Yokota) of sbt-release on which this builds
  *
  * @author Simon Leischnig
  */
object ReleaseHelperPlugin extends AutoPlugin {

//    override def trigger = allRequirements

  object autoImport {
    // tasks of the plugin

    // TODO: LATER future members include:
    //  - gen. process / target of other shippable resources
    //  - targets & kinds of version updates (like in website MD, other project's sbt files

    val rhAllTests = taskKey[Unit]("execution of test and it:test")

    // tasks relating to the upload of the webpage
    val rhWebpageLocalDirectory = taskKey[File]("the local base directory of the webpage")
    val rhWebpageRemoteWWWPath = taskKey[String]("the name of the www directory on the remote server inside its configured base path")
    val rhWebpageUploadConfig = taskKey[File]("Upload config file incl. login data for the website")
    val rhWebpageUpload = taskKey[Unit]("Upload task for the website")
    val rhRuntimeVersion = taskKey[String]("returns the runtime version, possibly changing throughout the release process")

    // debug task
    val rhPrintProjectInfo = taskKey[Unit]("prints the project information")
  }

  import autoImport._
  import sbtrelease.ReleasePlugin.autoImport._

  /*
  Convenience settings and tasks to be used by the OPAL root project
   */
  lazy val opalBaseProjectConvenienceSettings: Seq[Def.Setting[_]] = Seq(

    rhWebpageLocalDirectory := baseDirectory.value / "target" / "scala-2.12" / "resource_managed" / "site",  //TODO: integrate in task graph
    rhWebpageRemoteWWWPath := "/www",
    rhWebpageUploadConfig := (baseDirectory in ThisProject).value / "releasehelper.conf",
    rhWebpageUpload := {
      val cfgFile = rhWebpageUploadConfig.value
      val localWWWDir = rhWebpageLocalDirectory.value
      val remoteWWWPath = rhWebpageRemoteWWWPath.value
      if(! cfgFile.exists()) {
        sys.error(s"The config file for the FTP transport of the OPAL website has not been found: $cfgFile. To be specified in taskKey 'rhWebpageUploadConfig'")
      }
      if(!localWWWDir.exists() || !localWWWDir.isDirectory || localWWWDir.listFiles.size == 0) {
        sys.error(s"The specified local WWW directory $localWWWDir is nonexistent or empty")
      }
      println(s"Website upload initiating: ($localWWWDir -> $remoteWWWPath)")
      Utils.obtainOpalWebsiteTransport(cfgFile).withEstablished{ conn =>
        conn.uploadDirectory(localWWWDir, remoteWWWPath)
      }.flatten.get //TODO: handle Transport exceptions
    }
  )

  lazy val defaultSettings: Seq[Def.Setting[_]] = baseSettings ++ Seq(
    // defaults from sbtrelease are mostly OK, they are listed here again so the interface is clear

    releasePublishArtifactsAction := publishLocal.value, //TODO: override with publishSigned task in clients, no dependency here
    releaseVersionFile := (baseDirectory in ThisProject).value / "version.sbt"
//    releaseUseGlobalVersion := true,
//    releaseCommitMessage - custom message
//    releasePublishArtifactsAction - e.g. change to publishSigned
//    releaseVersionBump := Version.Bump.default,
//    releaseIgnoreUntrackedFiles := false,
//    releaseVcsSign := true,
//    releaseCrossBuild := false
  )

  lazy val baseSettings: Seq[Def.Setting[_]] = sbtreleaseOverride ++ Seq(
    rhRuntimeVersion := ReleasePlugin.runtimeVersion.value,
    rhPrintProjectInfo := {
      val (projectName, projectVersion, projectDir, scalac) =
        ((name in ThisProject).value, ReleasePlugin.runtimeVersion.value, (baseDirectory in ThisProject).value, (scalacOptions in ThisProject).value)

      println(s"rhPrintProjectInfo: ($projectName, $projectVersion, $projectDir, $scalac)")
    }
  )

  /**
    * Rewires the tasks and settings provided by sbt-release that we reuse. Currently, we don't need to.
    * This will change with modular commands
    */
  lazy val sbtreleaseOverride: Seq[Def.Setting[_]] = sbtrelease.ReleasePlugin.projectSettings ++ Seq(
  )

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings

  // Fragen
  /*
    - git checks: currently: no untracked files, no unstaged files
    - form of version changes; Proposal:
      # OPAL-sbt: externalize version into /version.sbt
      # OPAL-sbt-external-projects: externalize dependency version into opal-depend-version.sbt
      # textual files: match invisible markers around
    -
  */

}
