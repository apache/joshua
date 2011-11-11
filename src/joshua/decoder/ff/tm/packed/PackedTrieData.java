package joshua.decoder.ff.tm.packed;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class PackedTrieData {
	
	private MappedByteBuffer data;
	
	public PackedTrieData(String file_name, long size) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(file_name);
		this.data = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, size);
	}
}