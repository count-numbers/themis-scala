package services.contentextraction

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import services.CLIRunner
import services.thumbnail.ThumbnailExtractor

/**
  * Created by simfischer on 3/21/17.
  */
@Singleton
class ContentExtractorService @Inject() (val cliRunner: CLIRunner) {

  private val extractors = Seq(new OCRContentExtractor(cliRunner), new PlainTextContentExtractor)

  def forMimetype(mimeType: String): Option[ContentExtractor] = {
    extractors.find (_.getCompatibleMimeTypes.contains(mimeType))
  }
}