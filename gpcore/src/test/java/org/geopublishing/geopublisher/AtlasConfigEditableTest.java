package org.geopublishing.geopublisher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AtlasConfigEditableTest {

	@Test
	public void testCheckBasename() {
		assertFalse(AtlasConfigEditable.checkBasename("ä"));
		assertFalse(AtlasConfigEditable.checkBasename(" asd "));
		assertFalse(AtlasConfigEditable.checkBasename("my atlas"));
		assertFalse(AtlasConfigEditable.checkBasename("a?"));
		assertFalse(AtlasConfigEditable.checkBasename("testAr"));
		assertTrue(AtlasConfigEditable.checkBasename("testar"));
	}

}
