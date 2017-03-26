package services.contentextraction

import java.nio.file.Path

/**
  * Created by simfischer on 3/25/17.
  */
class PlainTextContentExtractor extends ContentExtractor {

  var COMPATIBLE_CONTENT_TYPES: Array[String] = Array("text/plain")

  def getCompatibleMimeTypes: Array[String] = COMPATIBLE_CONTENT_TYPES

  override def extractContent(srcPath: Path): String = {
    val source = scala.io.Source.fromFile(srcPath.toFile)
    try source.mkString finally source.close()
  }

}
