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
			System.out.println("operation: " + operation.getName());
			WSDLService service = new WSDLService(operation.getName(), operation, new HTTPClientProvider() {
				@Override
				public HTTPClient newHTTPClient(String transactionId) {
					return newClient();
				}
			}, Charset.forName("UTF-8"));
			ComplexContent content = ((ComplexType) operation.getOperation().getInput().getParts().get(0).getElement().getType()).newInstance();

			ComplexContent input = service.getInput().newInstance();
			input.set("request", content);
//			if (operation.getName().equals("Add")) {
				content.set("x", 5);
				content.set("y", "6");
				byte [] bytes = IOUtils.toBytes(service.newInstance().buildInput(content, Charset.forName("UTF-8")));
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
