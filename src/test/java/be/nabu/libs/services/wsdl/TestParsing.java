/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.services.wsdl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.binding.xml.XMLMarshaller;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.wsdl.api.WSDLDefinition;
import be.nabu.libs.wsdl.parser.WSDLParser;

public class TestParsing extends TestCase {
	
	public void testXSDAny() throws SAXException, IOException, ParseException, URISyntaxException, ParserConfigurationException {
		WSDLParser parser = new WSDLParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("example3.wsdl"), false);
		WSDLDefinition definition = parser.getDefinition();

		WSDLService service = new WSDLService("test", definition.getBindings().get(0).getOperations().get(0), new HTTPClientProvider() {
			@Override
			public HTTPClient newHTTPClient(String transactionId) {
				return TestOperation.newClient();
			}
		}, Charset.forName("UTF-8"));
		
		for (Element<?> child : service.getServiceInterface().getInputDefinition()) {
			System.out.println("child: " + child.getName());
		}
		
		// testing xsd:any
		Element<?> element = definition.getRegistry()
			.getElement("http://www.sub.test.be/XSD/SSDN/Common", "ExtensionPlaceHolder");
		
		// print the namespaces so we can check that they are ok
		element.setProperty(new ValueImpl<Boolean>(new ElementQualifiedDefaultProperty(), true));
		
		assertNotNull(element);
		
		ComplexContent content = ((ComplexType) element.getType()).newInstance();
		
		// we set both a complex element and a simple one
		content.set(NameProperty.ANY + "[test]", new Test2("wtf"));
		content.set(NameProperty.ANY + "[test2]", "wtf2");
		
		XMLMarshaller marshaller = new XMLMarshaller(element);
		marshaller.setNamespaceAware(true);
		marshaller.setAllowDefaultNamespace(false);
		StringWriter output = new StringWriter();
		marshaller.marshal(output, content);
		System.out.println(output.toString());
		
		XMLBinding binding = new XMLBinding((ComplexType) element.getType(), Charset.forName("UTF-8"));
		ByteArrayInputStream bytes = new ByteArrayInputStream(output.toString().getBytes("UTF-8"));
		ComplexContent unmarshalled = binding.unmarshal(bytes, new Window[0], element.getProperties());
		
		assertEquals("wtf", unmarshalled.get(NameProperty.ANY + "[test]/dude"));
		assertEquals("wtf2", unmarshalled.get(NameProperty.ANY + "[test2]"));
	}
	
	@XmlRootElement(name="test2", namespace="thisisatest")
	public static class Test2 {
		private String wtf;

		public Test2() {
			// auto
		}
		
		public Test2(String wtf) {
			this.wtf = wtf;
		}

		@XmlElement(name="dude", namespace="anothernamespace")
		public String getWtf() {
			return wtf;
		}

		public void setWtf(String wtf) {
			this.wtf = wtf;
		}
		
	}
}
