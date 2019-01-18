package util

import java.nio.file.{Path, Paths}

import play.api.Configuration

/** Resolves files in the filesystem based on configuration.
  *
  * Created by simfischer on 3/27/17.
  */
object FileSystemUtils {

  private def storageDir(implicit config: Configuration): Option[Path] = {
    config.getString("themis.storage.dir").map(Paths.get(_))
  }

  def thumbnailPath(attachmentId: Int)(implicit config: Configuration): Option[Path] = {
    storageDir.map(_.resolve(s"${attachmentId}.thumb"))
  }

  def previewPath(attachmentId: Int)(implicit config: Configuration): Option[Path] = {
    storageDir.map(_.resolve(s"${attachmentId}.preview"))
  }

  def attachmentPath(attachmentId: Int)(implicit config: Configuration): Option[Path] = {
    storageDir.map(_.resolve(s"${attachmentId}.attachment"))
  }
}
