package org.voyanttools.trombone.tool.progress;

import java.io.IOException;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.util.AbstractTool;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("progress")
public class ProgressMonitor extends AbstractTool  implements Progressable {

	protected Progress progress;
	
	protected String tool = "progress.ProgressMonitor";
	
	public ProgressMonitor(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		String id = parameters.getParameterValue("id");
		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Parameter \"id\" must be defined for progress.");
		} else if (Progress.isStored(storage, id)==false) {
			throw new IllegalArgumentException("The specified progress identifier could not be found.");
		}
		progress = Progress.retrieve(storage, id);
		if (progress.isActive()) {
			progress.checkStart(parameters.getParameterIntValue("maxMillisSinceStart", 1000*60*5)); // default 5 minutes
			progress.checkUpdate(parameters.getParameterIntValue("maxMillisSinceStart", 1000*60)); // default 1 minute
		}
	}

	@Override
	public Progress getProgress() {
		return progress;
	}
}
