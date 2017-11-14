package org.opalj.releasehelper

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.opalj.releasehelper.transport.{ApacheFTPTransport, Transport}

object Utils {

    def defaultCfg = "/home/simon/ftptarget/releasehelper.conf"

    def obtainOpalWebsiteTransport(opalWebsiteConfigFile: File): Transport = {
      val cfg = ConfigFactory.parseFile(opalWebsiteConfigFile)
      val host = cfg.getString("ftp.opalhome.host")
      val port = cfg.getInt("ftp.opalhome.port")
      val user = cfg.getString("ftp.opalhome.user")
      val password = cfg.getString("ftp.opalhome.password")
      val websitedir = cfg.getString("ftp.opalhome.websitedir")
      new ApacheFTPTransport(host, port, user, password, websitedir)
    }

}
