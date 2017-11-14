package org.opalj.releasehelper.transport

import java.io.File
import org.apache.commons.net.ftp.FTPClient
import scala.util.Try

case class ApacheFTPTransport(server: String, port: Int, user: String, pass: String, remoteBasePath: String = "/") extends Transport {
  override def withEstablished(f: TransportConnection => Unit): Try[Unit] = {

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
    cleanupResult.map(_ => opTry).get
  }
}

case class ApacheFTPConnection(hotFTPClient: FTPClient) extends BasicRemoteConnection {
  val cl = hotFTPClient

  override def mkdir(path: String) = Try {
    assert(cl.makeDirectory(path))
  }

  override def rmdir(path: String) = Try{
    assert(cl.removeDirectory(path))
  }

  override def uploadSingleFile(localFile: File, parentPath: String) = Try {
    import org.apache.commons.net.ftp.FTP
    import java.io.FileInputStream
    val inputStream = new FileInputStream(localFile)
    val result = Try{
      cl.setFileType(FTP.BINARY_FILE_TYPE)
      println("Uploading file: " + localFile.getName + " to " + parentPath)
      assert(cl.storeFile(pathOf(parentPath, localFile.getName), inputStream))
    }
    inputStream.close()
    result.get
  }
}