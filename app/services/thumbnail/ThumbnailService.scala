package services.thumbnail

import javax.inject.{Inject, Singleton}

import play.api.Logger
import services.CLIRunner

/**
  * Created by simfischer on 3/20/17.
  */
@Singleton
class ThumbnailService @Inject() (val cliRunner: CLIRunner) {

  private val extractors = Seq(
    new PlainTextThumbnailExtractor,
    //new PDFThumbnailExtractor,
    new ImageMagickThumbnailExtractor(cliRunner),
    new ImageThumbnailExtractor
  )

  Logger.info(s"Set up ${extractors.length} thumbnail extractors")

  def forMimetype(mimeType: String): Option[ThumbnailExtractor] = {
    extractors.find (_.getCompatibleMimeTypes.contains(mimeType))
  }
}
