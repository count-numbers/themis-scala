package services.contentextraction;

import java.nio.file.Path;

/**
 * Extracts contents of a {@link models.Document}'s {@link models.Attachment} file as plain text.
 *  
 * @author Simon
 */
public interface ContentExtractor {

	/** Returns the content as plain text. */
	public String extractContent(Path srcPath);
}
