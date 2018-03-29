package org.voyanttools.trombone.tool.progress;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class Progress {
	
	public enum Status {
		LAUNCH, RUNNING, FINISHED, ABORTED
	}
	
	@XStreamOmitField
	private transient Storage storage;
	
	private String id = null;
	
	private long start = 0l;
	
	private long current = 0l;
	
	private float completion = 0f;
	
	private Status status;
	
	private String code;
	
	private String message;
	
	Progress(Storage storage, String id, long start, long current, float completion, Status status, String code, String message) {
		this.storage = storage;
		this.id = id;
		this.start = start;
		this.current = current;
		this.completion = completion;
		this.status = status;
		this.code = code;
		this.message = message;
	}
	
	public static Progress getNewProgress(Storage storage) throws IOException {
		long now = Calendar.getInstance().getTimeInMillis();
		Progress progress = new Progress(storage, UUID.randomUUID().toString(), now, now, 0f, Status.LAUNCH, "launch", "Launching.");
		progress.store();
		return progress;
	}
	
	public void store() throws IOException {
		current = Calendar.getInstance().getTimeInMillis();
		Writer writer = storage.getStoreWriter(id+"-progress", Location.cache, true);
		writer.append(id+"\t"+String.valueOf(start)+"\t"+String.valueOf(current)+"\t"+String.valueOf(completion)+"\t"+status.name()+"\t"+code+"\t"+message.replaceAll("\n", " ")+"\n");
		writer.close();
	}

	static Progress retrieve(Storage storage, String id) throws IOException {
		List<String> lines = storage.retrieveStrings(id+"-progress", Location.cache);
		String last = lines.get(lines.size()-1);
		String[] parts = last.split("\t");
		return new Progress(storage, parts[0], Long.valueOf(parts[1]), Long.valueOf(parts[2]), Float.valueOf(parts[3]), Status.valueOf(parts[4]), parts[5], parts[6]);
	}

	String getId() {
		return id;
	}
	
	public boolean checkStart(int max) throws IOException {
		return check(max, start, "updateTimeout", "Too much time elapsed since last update.");
	}
	
	public boolean checkUpdate(int max) throws IOException {
		return check(max, current, "startTimeout", "Too much time elapsed since start.");
	}
	
	private boolean check(int max, long val, String code, String message) throws IOException {
		long now = Calendar.getInstance().getTimeInMillis();
		if (Calendar.getInstance().getTimeInMillis()-val>max) {
			status = Status.ABORTED;
			this.code = code;
			this.message = "Too much time elapsed since start";
			this.current = now;
			store();
			return false;
		} else {
			return true;
		}
	}

	public boolean isActive() {
		return status==Status.LAUNCH || status==Status.RUNNING;
	}

}
