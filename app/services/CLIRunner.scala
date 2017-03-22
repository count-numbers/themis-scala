package services

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ProcessBuilder.Redirect
import javax.inject.Inject
import javax.inject.Singleton

import play.api.Configuration

import scala.sys.process.Process

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


  /** Starts the given command and reads the output as text. */
  def readStdoutAsText(command: Seq[String]): String = {
    Process(command = command, cwd = workingDirectory) #>> devNull !!
  }

  def runAndWait(command: Seq[String]): Boolean = {
    (Process(command = command, cwd = workingDirectory) #>> devNull !) != 0
  }
}
