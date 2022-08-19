package org.rg.sbc

import com.typesafe.config.ConfigFactory

object Settings:
  private val cfg = ConfigFactory.load()
  val pathForBackupFile: String = cfg.getString("sbc.path")




