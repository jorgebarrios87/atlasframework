package org.geopublishing.geopublisher;

import static org.junit.Assert.assertEquals;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.geopublishing.atlasViewer.exceptions.AtlasException;
import org.geopublishing.atlasViewer.internal.AMLUtil;
import org.geopublishing.geopublisher.GPTestingUtil.Atlas;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;



public class AMLExporterTest extends AMLExporter{

	public AMLExporterTest() throws ParserConfigurationException, AtlasException, FactoryException, TransformException, SAXException, IOException, TransformerException {
		super(GPTestingUtil.getAtlasConfigE(Atlas.small));
		
		// Create a DOM builder and parse the fragment
		final DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		Document document = null;
		document = factory.newDocumentBuilder().newDocument();

		Node fontsE = exportFonts(document);
		int length = fontsE.getChildNodes().getLength();
		assertEquals(1,length);
		
		Node namedItem = fontsE.getChildNodes().item(0).getAttributes().getNamedItem(AMLUtil.ATT_FONT_FILENAME);

		assertEquals("futura_book.ttf",namedItem.getTextContent());
	}

	@Test
	public void testFonts() throws AtlasException, FactoryException, TransformException, SAXException, IOException, ParserConfigurationException, TransformerException {
		AMLExporterTest amlExporter = new AMLExporterTest();
	}
}