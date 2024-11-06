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

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.http.client.DefaultHTTPClient;
import be.nabu.libs.http.client.SPIAuthenticationHandler;
import be.nabu.libs.http.client.connections.PlainConnectionHandler;
import be.nabu.libs.http.core.CustomCookieStore;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.wsdl.api.BindingOperation;
import be.nabu.libs.wsdl.api.WSDLDefinition;
import be.nabu.libs.wsdl.parser.WSDLParser;
import be.nabu.utils.io.IOUtils;

public class TestOperation {
	public static void main(String...args) throws SAXException, IOException, ParseException, URISyntaxException, ParserConfigurationException {
		WSDLParser parser = new WSDLParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("example.wsdl"), false);
		WSDLDefinition definition = parser.getDefinition();
		System.out.println(definition.getBindings().get(0).getOperations());
		for (BindingOperation operation : definition.getBindings().get(0).getOperations()) {
			System.out.println("Operation: " + operation.getName());
			WSDLService service = new WSDLService(operation.getName(), operation, new HTTPClientProvider() {
				@Override
				public HTTPClient newHTTPClient(String transactionId) {
					return newClient();
				}
			}, Charset.forName("UTF-8"));

			ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
			
			if (operation.getName().equals("Add")) {
				input.set("body/Add/x", 5);
				input.set("body/Add/y", 6);
			}
			else if (operation.getName().equals("Multiply")) {
				input.set("body/Multiply/x", 5);
				input.set("body/Multiply/y", 6);
			}
			else if (operation.getName().equals("Divide")) {
				input.set("body/Divide/x", 5);
				input.set("body/Divide/y", 6);
			}
			else if (operation.getName().equals("Subtract")) {
				input.set("body/Subtract/x", 5);
				input.set("body/Subtract/y", 6);
			}
			byte [] bytes = IOUtils.toBytes(service.newInstance().buildInput(input, Charset.forName("UTF-8")));
			System.out.println(new String(bytes, "UTF-8"));
//			}
		}
	}

	public static HTTPClient newClient() {
		SSLContext context = null;
		HTTPClient client = new DefaultHTTPClient(
			new PlainConnectionHandler(context, 10000, 10000),
			new SPIAuthenticationHandler(),
			new CookieManager(new CustomCookieStore(), CookiePolicy.ACCEPT_ALL), 
			false
		);
		return client;
	}
}
