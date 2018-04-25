package org.voyanttools.trombone.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.stanford.nlp.util.StringUtils;

public class Location implements Cloneable, Serializable {

	public enum Type {
		CITY, REGION, COUNTRY, UNKNOWN;
	}
	private String id;
	private String source;
	private String lang;
	private Type type;
	private String[] names;
	private String[] regions;
	private String[] countries;
	private String[] continents;
	private int population;
	private double lat;
	private double lng;
	public Location() {
		this(null, null, null, null, null, null, null, null, 0, 0, 0);
	}
	public Location(String id, String source, Type type, String lang, String[] names, String[] regions, String[] countries, String[] continents, int population, double lat, double lng) {
		reset(id, source, type, lang, names, regions, countries, continents, population, lat, lng);
	}
	
	public void reset(String id, String source, Type type, String lang, String[] names, String[] regions, String[] countries,
			String[] continents, int population, double lat, double lng) {
		setId(id);
		setSource(source);
		setType(type);
		setLang(lang);
		setNames(names);
		setRegions(regions);
		setCountries(countries);
		setContinents(continents);
		setPopulation(population);
		setLat(lat);
		setLng(lng);
	}
	
	public String getId() {
		return id;
	}
	
	public int getPopulation() {
		return population;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLng() {
		return lng;
	}

	public void setId(String id) {
		this.id = id==null ? UUID.randomUUID().toString() : id;
	}
	public void setSource(String source) {
		this.source = source==null || source.isEmpty() ? "unknown" : source;
	}
	public void setLang(String lang) {
		this.lang = lang==null ? "" : lang;
	}
	public void setType(Type type) {
		this.type = type==null ? Type.UNKNOWN : type;
	}
	public void setNames(String[] names) {
		this.names = names==null ? new String[0] : names;
	}
	public void setRegions(String[] regions) {
		this.regions = regions==null ? new String[0] : regions;
	}
	public void setCountries(String[] countries) {
		this.countries = countries==null ? new String[0] : countries;
	}
	public void setContinents(String[] continents) {
		this.continents = continents==null ? new String[0] : continents;
	}
	public void setPopulation(int population) {
		this.population = population;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	
	/**
	 * This creates a list of place name alternatives intended to disambiguate,
	 * it generates a list of names, sorted by descending length, that includes
	 * entries for combinations of the following:
	 * 
	 *  * place name, country (for cities and regions)
	 *  * place name, country (for regions)
	 *  * place name
	 * @return
	 */
	public List<String> getPlaces() {
		Set<String> places = new HashSet<String>();
		for (String name : names) {
			places.add(name);
			if (type==Type.CITY) {
				for (String region : regions) {
					places.add(name+", "+region);
				}
			}
			if (type==Type.CITY || type==Type.REGION) {
				for (String country : countries) {
					places.add(name+", "+country);
				}
			}
		}
		return places.stream()
			.sorted((p1,p2) -> Integer.compare(p2.length(), p1.length()))
			.collect(Collectors.toList());
	}
	
	/**
	 * This returns a canonical place name.
	 * 
	 * @return
	 */
	public String getBestName() {
		StringBuilder sb = new StringBuilder(names[0]);
//		if (countries!=null && countries.length>0 && (type==Type.CITY || type==Type.REGION)) {
//			sb.append(", ").append(countries[0]);
//		}
		return sb.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(source+":"+id +" ("+type.name().toLowerCase()+": ").append(population).append(")");
		appendMulti(sb, names, ": ");
		appendMulti(sb, regions, ", ");
		appendMulti(sb, countries, ", ");
		appendMulti(sb, continents, ", ");
		sb.append(" ").append(lat).append("/").append(lng);
		return sb.toString();
	}
	private void appendMulti(StringBuilder sb, String[] multi, String prefix) {
		if (multi!=null && multi.length>0) {
			sb.append(prefix).append(multi[0]);
			if (multi.length>1) {
				sb.append(" (").append(StringUtils.join(Arrays.copyOfRange(multi, 1, multi.length), ", ")).append(")");
			}
		}
	}
	
	public Location clone() {
		try {
			return (Location) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

}
