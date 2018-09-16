package services.contentextraction
import java.nio.file.Path

import services.CLIRunner

/**
  * Created by simfischer on 3/21/17.
  */
class TesseractContentExtractor(val cLIRunner: CLIRunner) extends ContentExtractor {

  var COMPATIBLE_CONTENT_TYPES: Array[String] = Array("image/png", "image/jpg", "image/jpeg", "image/tiff")

  def getCompatibleMimeTypes: Array[String] = COMPATIBLE_CONTENT_TYPES

  override def extractContent(srcPath: Path): String = {
    cLIRunner.readStdoutAsText(Seq("tesseract", srcPath.toString, "stdout", "-l", "deu"))
  }
}


/**
  * Created by simfischer on 3/21/17.
  */
class CLIContentExtractor(val cLIRunner: CLIRunner) extends ContentExtractor {

  var COMPATIBLE_CONTENT_TYPES: Array[String] = Array("application/pdf")

  def getCompatibleMimeTypes: Array[String] = COMPATIBLE_CONTENT_TYPES

  override def extractContent(srcPath: Path): String = {
    // TODO: This will miserably fail if script is not in expected position relative to working dir.
    // How do we best package this?
    cLIRunner.readStdoutAsText(Seq("app/services/contentextraction/ocr.sh", srcPath.toString))
  }
}

