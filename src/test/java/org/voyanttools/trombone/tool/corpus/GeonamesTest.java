package org.voyanttools.trombone.tool.corpus;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class GeonamesTest {

	@Test
	public void test() throws IOException {
		Storage storage = TestHelper.getDefaultTestStorage();
		String text1 = "London, Ontario and Montreal and London, England and Montreal and London and Montreal.";
		FlexibleParameters parameters = new FlexibleParameters(new String[]{"string="+text1,"includeCities=true"});
		Geonames geonamesTool = new Geonames(storage, parameters);
		geonamesTool.run();
		assertEquals(3, geonamesTool.citiesCountList.size());
		assertEquals(3, (int) geonamesTool.citiesCountList.get(0).getValue());
		assertEquals(1, (int) geonamesTool.citiesCountList.get(2).getValue());
		assertEquals(4, geonamesTool.connectionsCount.size());
		assertEquals("6058560-6077243", geonamesTool.connectionsCount.get(0).getKey());
		assertEquals(2, geonamesTool.connectionsCount.get(0).getValue().get());
		assertEquals("2643743-6077243", geonamesTool.connectionsCount.get(3).getKey());
		assertEquals(1, geonamesTool.connectionsCount.get(3).getValue().get());
		assertEquals(5, geonamesTool.connectionOccurrences.size());
		
	}

}
