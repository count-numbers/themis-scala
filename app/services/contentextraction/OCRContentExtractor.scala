package services.contentextraction
import java.nio.file.Path

import services.CLIRunner

/**
  * Created by simfischer on 3/21/17.
  */
class OCRContentExtractor(val cLIRunner: CLIRunner) extends ContentExtractor {

  var COMPATIBLE_CONTENT_TYPES: Array[String] = Array("image/png", "image/jpg", "image/jpeg", "image/tiff")

  def getCompatibleMimeTypes: Array[String] = COMPATIBLE_CONTENT_TYPES

  override def extractContent(srcPath: Path): String = {
    cLIRunner.readStdoutAsText(Seq("tesseract", srcPath.toString, "stdout", "-l", "deu"))
  }
}
