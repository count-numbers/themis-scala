package services

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ProcessBuilder.Redirect
import javax.inject.Inject
import javax.inject.Singleton

import play.api.{Configuration, Logger}

import scala.sys.process.{Process, ProcessBuilder}

/**
  * Utilities to execute command line tools. Unfortunately, this is currently implemented in a blocking way.
  *
  * @author Simon
  *
  */
@Singleton
class CLIRunner @Inject() (config: Configuration) {

  val devNull = new File(config.getString("dev-null", None)
    .getOrElse(if (System.getProperty("os.name").toLowerCase.contains("windows")) "NUL:" else "/dev/null"))

  val workingDirectory = new File(config.getString("working-dir", None).getOrElse("."))


  def readFromPipeStdoutAsText(command1: Seq[String], command2: Seq[String]): String = {
    Logger.info(s"Running command ${command1} | ${command2}")
    val p1 =   Process(command = command1, cwd = workingDirectory)
    val p2 =   Process(command = command2, cwd = workingDirectory)
    val stdout: String = (p1 #| p2 !!)
    return stdout
  }

  /** Starts the given command and reads the output as text. */
  def readStdoutAsText(command: Seq[String]): String = {
    Logger.info(s"Running command ${command}")
    val stdout: String = (Process(command = command, cwd = workingDirectory)  !!)
    return stdout
  }

  def runAndWait(command: Seq[String]): Boolean = {
    (Process(command = command, cwd = workingDirectory) #>> devNull !) != 0
  }
}
