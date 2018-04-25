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
	
	private transient boolean isNew = false; // transient, not stored
	
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
	
	public void abort(String code, String message) throws IOException {
		update(-1, Status.ABORTED, code, message);
	}
	
	public void update(float completion, Status status, String code, String message) throws IOException {
		if (completion>this.completion) {this.completion = completion;}
		this.current = Calendar.getInstance().getTimeInMillis();
		this.status = status;
		this.code = code;
		this.message = message;
		store();
	}

	public void store() throws IOException {
		current = Calendar.getInstance().getTimeInMillis();
		Writer writer = storage.getStoreWriter(id+"-progress", Location.cache, true);
		writer.append(id+"\t"+String.valueOf(start)+"\t"+String.valueOf(current)+"\t"+String.valueOf(completion)+"\t"+status.name()+"\t"+code+"\t"+message.replaceAll("\n", " ")+"\n");
		writer.close();
	}

	public static Progress retrieve(Storage storage, String id) throws IOException {
		if (isStored(storage, id)) {
			List<String> lines = storage.retrieveStrings(id+"-progress", Location.cache);
			String last = lines.get(lines.size()-1);
			String[] parts = last.split("\t");
			return new Progress(storage, parts[0], Long.valueOf(parts[1]), Long.valueOf(parts[2]), Float.valueOf(parts[3]), Status.valueOf(parts[4]), parts[5], parts[6]);
		} else {
			long now = Calendar.getInstance().getTimeInMillis();
			Progress progress = new Progress(storage, id, now, now, 0f, Status.LAUNCH, "launch", "Launching.");
			progress.store();
			progress.isNew = true;
			return progress;
		}
	}
	
	public static boolean isStored(Storage storage, String id) {
		return storage.isStored(id+"-progress", Location.cache);
	}
	
	/**
	 * Determines if this has just been created (shouldn't be true after any operations, retrievals, etc.)
	 * 
	 * @return whether or not this has just been created
	 */
	public boolean isNew() {
		return isNew;
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
			this.update(-1, Status.ABORTED, code, "Too much time elapsed since start");
			return false;
		} else {
			return true;
		}
	}

	public boolean isActive() {
		return status==Status.LAUNCH || status==Status.RUNNING;
	}
}
