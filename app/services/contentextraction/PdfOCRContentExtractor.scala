package services.contentextraction
import java.nio.file.Path

import services.CLIRunner

/**
  * Created by simfischer on 3/21/17.
  */
class PdfOCRContentExtractor(val cLIRunner: CLIRunner) extends ContentExtractor {

  override def extractContent(srcPath: Path): String = {
    cLIRunner.readStdoutAsText(Seq("tesseract", srcPath.toString, "stdout", "-l", "deu"))
  }
}
