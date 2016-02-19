package org.voyanttools.trombone.model;

public enum CorpusAccess {
	
	NONE, ADMIN, FULL, CONSUMPTIVE, NONCONSUMPTIVE;

	public static CorpusAccess getForgivingly(String value) {
		if (value!=null) {
			value = value.toUpperCase().trim();
			for (CorpusAccess v : values()) {
				if (value.equals(v.name())) {
					return v;
				}
			}
		}
		return FULL;
	}

}
