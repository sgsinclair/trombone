package ca.crim.nlp.pacte.client;

public class PacteDocument {

	private String psContent = null;
	private String psTitle = null;
	private String psID = null;
	private String psSource = null;
	private String psLanguages = null;
	private Long pnlDocSize = null;
	private String psDateAdded = null;
	private String psPath = null;

	public PacteDocument(String tsID, String tsTitle, String tsContent, String tsSource, String tsLanguages,
			Long tnlDocSize, String tsDateAdded, String tsPath) {
		psContent = tsContent;
		psTitle = tsTitle;
		psID = tsID;
		psSource = tsSource;
		psLanguages = tsLanguages;
		pnlDocSize = tnlDocSize;
		psDateAdded = tsDateAdded;
		psPath = tsPath;
	}

	public Long getDocSize() {
		return pnlDocSize;
	}

	public String getDateAdded() {
		return psDateAdded;
	}

	public String getPath() {
		return psPath;
	}

	public String getContent() {
		return psContent;
	}

	public String getTitle() {
		return psTitle;
	}

	public String getID() {
		return psID;
	}

	public String getSource() {
		return psSource;
	}

	public String getLanguages() {
		return psLanguages;
	}
}
