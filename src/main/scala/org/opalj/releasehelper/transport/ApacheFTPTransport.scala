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

import org.apache.commons.net.ftp.FTPClient
import org.opalj.releasehelper.Utils

import scala.util.Try

/**
  * Models FTP file transfer with the Apache commons net library.
  *
  * @author Simon Leischnig
  */
case class ApacheFTPTransport(server: String, port: Int, user: String, pass: String) extends Transport {
  override def withEstablished[T](f: TransportConnection => T): Try[T] = {

    val ftpClient = new FTPClient

    val opTry = Try { // connect and login to the server
      ftpClient.connect(server, port)
      val loginSuccessful = ftpClient.login(user, pass)
      ftpClient.enterLocalPassiveMode()

      if(! loginSuccessful) scala.sys.error(s"Login for FTP connection was incorrect ($user@$server:$port)")
      f(ApacheFTPConnection(ftpClient))
    }
    val cleanupResult = Try {
      ftpClient.logout
      ftpClient.disconnect()
    }
    cleanupResult.recover[Unit]{ case (t: Throwable) => println("Error: could not close FTP connection: " + t.getMessage)}
    opTry
  }
}

case class ApacheFTPConnection(hotFTPClient: FTPClient) extends BasicRemoteConnection {
  val cl = hotFTPClient

  override def mkdir(path: String) = Try {
    Utils.errorPredicate(s"FTP: mkdir($path) not successful")(cl.makeDirectory(path))
  }

  override def rmdir(path: String) = Try{
    Utils.errorPredicate(s"FTP: rm -r ($path) not successful")(cl.removeDirectory(path))
  }

  override def uploadSingleFile(localFile: File, remotePath: String) = Try {
    import org.apache.commons.net.ftp.FTP
    import java.io.FileInputStream
    val inputStream = new FileInputStream(localFile)
    val result = Try{
      cl.setFileType(FTP.BINARY_FILE_TYPE)
      println("Uploading file: " + localFile.getName + " to " + remotePath)
      Utils.errorPredicate(s"FTP: upload $localFile -> $remotePath not successful")(cl.storeFile(remotePath, inputStream))
    }
    inputStream.close()
    result.get
  }
}