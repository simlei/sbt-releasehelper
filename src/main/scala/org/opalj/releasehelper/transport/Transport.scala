package org.opalj.releasehelper.transport

import java.io.File

import scala.util.{Success, Try}

trait Transport {
  def withEstablished(f: TransportConnection => Unit): Try[Unit] // ARM style, f should be composed from single TransportConnection#transportOperation(...)
}
object Transport {
}

trait TransportConnection {
  def remoteBasePath = "/"
  def uploadSingleFile(localFile: File, parentPath: String = remoteBasePath): Try[Unit] // contract: parent path must exist, automatic overwrite, force target directory to exist
  def uploadDirectory(localDir: File, parentPath: String = remoteBasePath, remoteName: String): Try[Unit] // contract: parent must exist; no directory merge - complete override of accidental collisions
}
trait BasicRemoteConnection extends TransportConnection {
  def pathOf(parentPath: String, childName: String) = parentPath match {
    case "/" => "/" + childName
    case _ => parentPath + "/" + childName
  }
  def mkdir(path: String): Try[Unit]
  def rmdir(path: String): Try[Unit]

  def uploadDirectory(localDir: File, parentPath: String, remoteName: String): Try[Unit] = {
    def foldOp(currentPath: String)(lastOp: Try[Unit], subFile: File): Try[Unit] = {
      lastOp.flatMap(_ =>
        if (subFile.isDirectory) {
          uploadDirectory(subFile, currentPath, subFile.getName)
        } else {
          uploadSingleFile(subFile, currentPath)
        }
      )
    }

    val targetPath = pathOf(parentPath, remoteName)
    rmdir(targetPath) // ignore if this fails
    mkdir(targetPath)
    Try(localDir.listFiles).flatMap(subFiles =>
      subFiles.foldLeft[Try[Unit]](Success())(foldOp(targetPath))
    )

  }

}
