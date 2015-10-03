package org.voyanttools.trombone.model;

public enum EntityType{time, location, organization, person, money, percent, date, duration, set, misc, unknnown;
	public static EntityType getForgivingly(String type) {
		String typeString = type.toLowerCase();
		for (EntityType t : EntityType.values()) {
			if (t.name().equals(typeString)) return t;
		}
		return unknnown;
	}
}