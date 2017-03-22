package services.thumbnail

import services.CLIRunner
import java.io.IOException
import java.nio.file.Path

/** Uses the ImageMagick CLI ("convert") to extract a PNG from a PDF.
  */
class ImageMagickThumbnailExtractor(val cliRunner: CLIRunner) extends ThumbnailExtractor {

  def getCompatibleMimeTypes: Array[String] = Array("application/pdf")

  @throws[IOException]
  def extractFromFile(srcPath: Path, thumbPath: Path) {
    cliRunner
      .runAndWait(Seq(
        "convert",                      // executable
        srcPath + "[0]",                // page 1 of input PDF
        "-resize",                      // output size, aspect ratio preserved
        "200x200", "png:" + thumbPath)) // destination with format as prefix
  }
}
