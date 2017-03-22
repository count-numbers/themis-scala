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

  private val pdfOCRContentExtractor = new PdfOCRContentExtractor(cliRunner)

  def forMimetype(mimeType: String): Option[ContentExtractor] = {
    if (mimeType == "application/pdf") Some(pdfOCRContentExtractor) else None
  }

}