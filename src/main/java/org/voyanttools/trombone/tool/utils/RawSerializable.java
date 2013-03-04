package org.voyanttools.trombone.tool.utils;

import java.io.IOException;
import java.io.Writer;

public interface RawSerializable {
	public void serialize(Writer writer) throws IOException;
}
