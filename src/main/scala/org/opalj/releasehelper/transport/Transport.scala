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
package org.opalj.releasehelper.transport

import java.io.File

import scala.util.{Success, Try}


/**
  * Models general file transfer scheme like FTP, ssh, etc.
  *
  * @author Simon Leischnig
  */
trait Transport {
  def withEstablished[T](f: TransportConnection => T): Try[T] // ARM style, f should be composed from single TransportConnection#transportOperation(...)
}
object Transport {
  def pathOf(parentPath: String, childName: String) = parentPath match {
    case "/" => "/" + childName
    case _ => parentPath + "/" + childName
  }
}

trait TransportConnection {
  def uploadSingleFile(localFile: File, remotePath: String): Try[Unit] // contract: parent path must exist, automatic overwrite, force target directory to exist
  def uploadDirectory(localDir: File, remotePath: String): Try[Unit] // contract: parent must exist; no directory merge - complete override of accidental collisions
}
trait BasicRemoteConnection extends TransportConnection {
  def mkdir(path: String): Try[Unit]
  def rmdir(path: String): Try[Unit]

  def uploadDirectory(localDir: File, remotePath: String): Try[Unit] = {
    def foldOp(currentPath: String)(lastOp: Try[Unit], subFile: File): Try[Unit] = {
      lastOp.flatMap(_ =>
        if (subFile.isDirectory) {
          uploadDirectory(subFile, Transport.pathOf(currentPath, subFile.getName))
        } else {
          uploadSingleFile(subFile, Transport.pathOf(currentPath, subFile.getName))
        }
      )
    }

    if(! remotePath.equals("/")) {
      rmdir(remotePath) // ignore if this fails is ok, we just want to try our best to overwrite and not merge anything
      mkdir(remotePath)
    }
    val result = Try(localDir.listFiles).flatMap(subFiles => {
      subFiles.foldLeft[Try[Unit]](Success())(foldOp(remotePath))
    })
    result

  }

}
