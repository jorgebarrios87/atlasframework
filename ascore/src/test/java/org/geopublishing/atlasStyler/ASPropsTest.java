package org.geopublishing.atlasStyler;

import org.junit.Test;

public class ASPropsTest {

	@Test
	public void testInit() {
		// Just check that there is no NPE about missing resources
		ASProps.init(ASProps.PROPERTIES_FILENAME, ASProps.PROPERTIES_FOLDER);
	}

}