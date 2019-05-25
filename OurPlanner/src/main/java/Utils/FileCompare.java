package Utils;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;


public class FileCompare {

	private final static Logger LOGGER = Logger.getLogger(FileCompare.class);

	public static final boolean compareFiles(File file1, File file2) {

		LOGGER.info("Compare files: " + file1.getPath() + " and " + file2.getPath() );

		if(!metaDataCompre(file1, file2))
			return false;

		if(!contentCompare(file1, file2))
			return false;

		return true;
	}

	private static boolean contentCompare(File file1, File file2) {

		LOGGER.info("file content compareison");

		Path file1Path = file1.toPath();
		Path file2Path = file2.toPath();

		try {

			final long size = Files.size(file1Path);
			final int mapspan = 256 * 1024;

			try (FileChannel chana = (FileChannel)Files.newByteChannel(file1Path);
					FileChannel chanb = (FileChannel)Files.newByteChannel(file2Path)) {

				for (long position = 0; position < size; position += mapspan) {
					MappedByteBuffer mba = mapChannel(chana, position, size, mapspan);
					MappedByteBuffer mbb = mapChannel(chanb, position, size, mapspan);

					if (mba.compareTo(mbb) != 0) {
						return false;
					}
				}
			}
		} catch (IOException e) {
			LOGGER.fatal("file comparison failed!");
			LOGGER.fatal(e, e);
		}

		return true;
	}

	private static boolean metaDataCompre(File file1, File file2) {

		LOGGER.info("check files before compareison");

		if (!file1.exists() || !file2.exists()) {
			// one or both files missing
			return false;
		}

		if (file1.isDirectory() || file2.isDirectory()) {
			// don't want to compare directory contents
			return false;
		}

		if (file1.length() != file2.length()) {
			// lengths differ, cannot be equal
			return false;
		}

		try {
			if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
				// same file
				return true;
			}
		} catch (IOException e) {
			LOGGER.fatal("canonical file comparison failed!");
			LOGGER.fatal(e, e);
		}

		return true;
	}

	private static MappedByteBuffer mapChannel(FileChannel channel, long position, long size, int mapspan) throws IOException {
		final long end = Math.min(size, position + mapspan);
		final long maplen = (int)(end - position);
		return channel.map(MapMode.READ_ONLY, position, maplen);
	}
}
