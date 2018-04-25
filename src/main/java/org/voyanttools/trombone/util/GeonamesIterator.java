package org.voyanttools.trombone.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.voyanttools.trombone.model.Location;

public class GeonamesIterator implements Iterator<Location>, AutoCloseable  {
	
	private BufferedReader reader;
	private String lang;

	public GeonamesIterator(String lang) {
		this.lang = lang;
		reader = null; // initialize
		InputStream is = getClass().getResourceAsStream("/org/voyanttools/trombone/geonames/"+lang+".txt");
		if (is!=null) {
			reader = new BufferedReader(new InputStreamReader(is));
		}
	}

	@Override
	public void close() {
		if (reader!=null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			reader = null;
		}
	}

	@Override
	public boolean hasNext() {
		try {
			if (reader!=null && reader.ready()) {return true;}
		} catch (Exception e) {
			// fail silently but ensure closed belo
		}
		close();
		return false;
	}

	@Override
	public Location next() {
		return next(new Location());
	}
	
	public Location next(Location reusableLocation) {
		String line = null;
		try {
			line = reader.readLine();
		} catch (IOException e) {
		}
		if (line!=null && line.trim().isEmpty()==false) {
			String[] parts = line.trim().split("\t");
			if (parts.length==8) {
				if (reusableLocation==null) {reusableLocation= new Location();}
				reusableLocation.reset(parts[0],"geonames",Location.Type.CITY,lang,parts[5].split("\\+"),parts[6].split("\\+"),parts[7].split("\\+"),null,Integer.parseInt(parts[4]),Double.valueOf(parts[2]),Double.valueOf(parts[3]));
				return reusableLocation;
			} else {
				System.err.println("Unrecognized geoname line:\n\t"+line);
			}
		}
		// any exceptions, empty lines or incomplete data
		return hasNext() ? next(reusableLocation) : null;
	}
}
