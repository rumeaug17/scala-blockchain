package org.rg.sbc

import scala.util.Try
import com.typesafe.config.ConfigFactory

object Settings:
  private val cfg = ConfigFactory.load()
  val pathForBackupFile: String = cfg.getString("sbc.path")
  val debugMode = Try(cfg.getBoolean("sbc.debug_mode")).getOrElse(false)



