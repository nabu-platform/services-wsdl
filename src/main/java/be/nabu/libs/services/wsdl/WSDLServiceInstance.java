package be.nabu.libs.services.wsdl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.http.client.BasicAuthentication;
import be.nabu.libs.http.client.NTLMPrincipalImpl;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.wsdl.api.WSExtension;
import be.nabu.libs.types.BaseTypeInstance;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.binding.xml.XMLMarshaller;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.wsdl.api.BindingOperationMessage;
import be.nabu.libs.wsdl.api.BindingOperationMessageLayout;
import be.nabu.libs.wsdl.api.MessagePart;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.MultiPart;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class WSDLServiceInstance implements ServiceInstance {

	private WSDLService definition;
	
	public WSDLServiceInstance(WSDLService wsdlService) {
		this.definition = wsdlService;
	}

	@Override
	public WSDLService getDefinition() {
		return definition;
	}
	
	LimitedReadableContainer<ByteBuffer> buildInput(ComplexContent input, Charset charset) throws IOException {
		ComplexType buildRequestEnvelope = buildRequestEnvelope(true);
		ComplexContent envelope = buildRequestEnvelope.newInstance();
		envelope.set("Body", ((ComplexType) buildRequestEnvelope.get("Body").getType()).newInstance());
		
		// check if we have extensions in place
		if (definition.getExtensions() != null) {
			for (WSExtension extension : definition.getExtensions()) {
				extension.addInstance(definition, envelope, input);
			}
		}
		
		if (input != null) {
			if (definition.isBackwardsCompatible()) {
				((ComplexContent) envelope.get("Body")).set(getDefinition().getOperation().getOperation().getInput().getParts().get(0).getElement().getName(), input);
			}
			else {
				BindingOperationMessageLayout inputPartLayout = getDefinition().getOperation().getInputPartLayout();
				if (inputPartLayout != null && inputPartLayout.getBody() != null && inputPartLayout.getBody().getParts() != null) {
					for (MessagePart part : inputPartLayout.getBody().getParts()) {
						envelope.set("Body/" + part.getElement().getName(), input.get("body/" + part.getElement().getName()));
					}
				}
				if (inputPartLayout != null && inputPartLayout.getHeader() != null && inputPartLayout.getHeader().getParts() != null && !inputPartLayout.getHeader().getParts().isEmpty()) {
					for (MessagePart part : inputPartLayout.getHeader().getParts()) {
						envelope.set("Header/" + part.getElement().getName(), input.get("header/" + part.getElement().getName()));
					}
				}
			}
		}
		// use marshaller directly to access more features
		XMLMarshaller marshaller = new XMLMarshaller(new BaseTypeInstance(buildRequestEnvelope));
		ByteBuffer buffer = IOUtils.newByteBuffer();
		marshaller.setAllowXSI(definition.isAllowXsi());
		marshaller.setAllowDefaultNamespace(definition.isAllowDefaultNamespace());
		marshaller.setAllowQualifiedOverride(true);
		if (definition.isAllowDefaultNamespace()) {
			// we set the default namespace to that of the actual input, not all parsers are too good with namespaces, having the main content
			// with as few prefixes as possible is always a good thing
			if (input != null) {
				marshaller.setDefaultNamespace(input.getType().getNamespace());
			}
			else {
				marshaller.setDefaultNamespace(getDefinition().getOperation().getDefinition().getTargetNamespace());
			}
		}
		boolean soapNamespaceFixed = false;
		if (getDefinition().getNamespaces() != null) {
			for (PredefinedNamespace namespace : getDefinition().getNamespaces()) {
				marshaller.setPrefix(namespace.getPrefix(), namespace.getNamespace());
				soapNamespaceFixed |= buildRequestEnvelope.getNamespace().equals(namespace.getNamespace());
			}
		}
		if (!soapNamespaceFixed) {
			// fix the soap prefix, otherwise it will be autogenerated as "tns1"
			// again: some parsers aren't too bright
			marshaller.setPrefix(buildRequestEnvelope.getNamespace(), "soap");
		}
		if (getDefinition().getExtensions() != null) {
			for (WSExtension extension : getDefinition().getExtensions()) {
				Map<String, String> preferredPrefixes = extension.getPreferredPrefixes();
				if (preferredPrefixes != null) {
					for (Map.Entry<String, String> prefix : preferredPrefixes.entrySet()) {
						marshaller.setPrefix(prefix.getKey(), prefix.getValue());
					}
				}
			}
		}
		marshaller.marshal(IOUtils.toOutputStream(buffer), charset, envelope);
		return buffer;
	}
	
	ComplexContent parseOutput(ReadableContainer<ByteBuffer> input, Charset charset) throws IOException, ParseException {
		XMLBinding responseBinding = new XMLBinding(buildRequestEnvelope(false), charset);
		responseBinding.setIgnoreUndefined(true);
		return responseBinding.unmarshal(IOUtils.toInputStream(input), new Window[0]);
	}

	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		String endpoint = (String) input.get("endpoint");
		String transactionId = (String) input.get("transactionId");
		if (endpoint == null) {
			endpoint = getDefinition().getEndpoint();
		}
		// if no endpoint is given, use the one from the wsdl
		if (endpoint == null && !getDefinition().getOperation().getDefinition().getServices().isEmpty() && !getDefinition().getOperation().getDefinition().getServices().get(0).getPorts().isEmpty()) {
			endpoint = getDefinition().getOperation().getDefinition().getServices().get(0).getPorts().get(0).getEndpoint();
		}
		if (endpoint == null) {
			throw new ServiceException("SOAP-1", "No endpoint passed in and none were found in the wsdl");
		}
		try {
			LimitedReadableContainer<ByteBuffer> buffer = buildInput((ComplexContent) input, getDefinition().getCharset());
			
			final String username = input == null || input.get("authentication/username") == null ? definition.getUsername() : (String) input.get("authentication/username");
			final String password = input == null || input.get("authentication/password") == null ? definition.getPassword() : (String) input.get("authentication/password");

			BasicPrincipal principal = null;
			if (username != null) {
				int index = username.indexOf('/');
				if (index < 0) {
					index = username.indexOf('\\');
				}
				if (index < 0) {
					principal = new BasicPrincipal() {
						private static final long serialVersionUID = 1L;
						@Override
						public String getName() {
							return username;
						}
						@Override
						public String getPassword() {
							return password;
						}
					};
				}
				// create an NTLM principal
				else if (username != null) {
					principal = new NTLMPrincipalImpl(username.substring(0, index), username.substring(index + 1), password);
				}
			}
			
			URI uri = new URI(URIUtils.encodeURI(endpoint));
			HTTPClient client = getDefinition().getHttpClientProvider().newHTTPClient(transactionId);
			PlainMimeContentPart content = new PlainMimeContentPart(null, buffer,
				new MimeHeader("Content-Length", new Long(buffer.remainingData()).toString()),
				new MimeHeader("Content-Type", (getDefinition().getOperation().getDefinition().getSoapVersion() == 1.2 ? "application/soap+xml" : "text/xml") + "; charset=" + getDefinition().getCharset().displayName().toLowerCase()),
				new MimeHeader("Host", uri.getAuthority())
			);
			// this should be ok because the buffer is a DynamicByteBuffer which is resettable
			content.setReopenable(true);
			
			if (getDefinition().getOperation().getSoapAction() != null) {
				content.setHeader(new MimeHeader("SOAPAction", "\"" + getDefinition().getOperation().getSoapAction() + "\""));
			}
			
			if (definition.getPreemptiveAuthorizationType() != null && principal != null) {
				switch(definition.getPreemptiveAuthorizationType()) {
					case BASIC:
						content.setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, new BasicAuthentication().authenticate(principal, "basic")));
					break;
					case BEARER:
						content.setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, "Bearer " + principal.getName()));
					break;
				}
			}
			
			HTTPResponse httpResponse = client.execute(
				new DefaultHTTPRequest("POST", definition.isUseFullPathTarget() ? uri.toString() : uri.getPath(), content), 
				principal, 
				endpoint.startsWith("https"), 
				true
			);
			if ((httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) || (getDefinition().getAllowedHttpCodes() != null && getDefinition().getAllowedHttpCodes().contains(httpResponse.getCode()))) {
				ContentPart contentPart;
				if (httpResponse.getContent() instanceof ContentPart) {
					contentPart = (ContentPart) httpResponse.getContent();
				}
				else if (httpResponse.getContent() instanceof MultiPart) {
					contentPart = (ContentPart) ((MultiPart) httpResponse.getContent()).getChild("part0");
				}
				else {
					throw new IllegalStateException("Could not find content part for response");
				}
				ComplexContent response = parseOutput(contentPart.getReadable(), getDefinition().getCharset());
				ComplexContent output = getDefinition().getServiceInterface().getOutputDefinition().newInstance();
				if (definition.isBackwardsCompatible()) {
					if (getDefinition().getOperation().getOperation().getOutput() != null && !getDefinition().getOperation().getOperation().getOutput().getParts().isEmpty()) {
						output.set("response", ((ComplexContent) response.get("Body")).get(getDefinition().getOperation().getOperation().getOutput().getParts().get(0).getElement().getName()));
					}
					if (getDefinition().getOperation().getOperation().getFaults() != null && !getDefinition().getOperation().getOperation().getFaults().isEmpty() && !getDefinition().getOperation().getOperation().getFaults().get(0).getParts().isEmpty()) {
						output.set("fault", ((ComplexContent) response.get("Body")).get(getDefinition().getOperation().getOperation().getFaults().get(0).getParts().get(0).getElement().getName()));
					}
				}
				else if (getDefinition().getOperation().getOutputPartLayout() != null) {
					BindingOperationMessage body = getDefinition().getOperation().getOutputPartLayout().getBody();
					if (body != null && body.getParts() != null && !body.getParts().isEmpty()) {
						for (MessagePart part : body.getParts()) {
							output.set("body/" + part.getElement().getName(), response.get("Body/" + part.getElement().getName()));
						}
					}
					BindingOperationMessage header = getDefinition().getOperation().getOutputPartLayout().getHeader();
					if (header != null && header.getParts() != null && !header.getParts().isEmpty()) {
						for (MessagePart part : header.getParts()) {
							output.set("header/" + part.getElement().getName(), response.get("Header/" + part.getElement().getName()));
						}
					}
					if (getDefinition().getOperation().getFaults() != null) {
						for (BindingOperationMessage message : getDefinition().getOperation().getFaults()) {
							if (message.getParts() != null) {
								for (MessagePart part : message.getParts()) {
									output.set("fault/" + part.getElement().getName(), response.get("Body/" + part.getElement().getName()));
								}		
							}
						}
					}
				}
				return output;
			}
			else {
				byte[] bytes = null;
				if (httpResponse.getContent() instanceof ContentPart) {
					ReadableContainer<ByteBuffer> readable = ((ContentPart) httpResponse.getContent()).getReadable();
					if (readable != null) {
						try {
							bytes = IOUtils.toBytes(readable);
						}
						finally {
							readable.close();
						}
					}
				}
				throw new ServiceException("SOAP-2", "HTTP Exception [" + httpResponse.getCode() + "] " + httpResponse.getMessage() + (bytes == null ? "" : "\n" + new String(bytes)), httpResponse.getCode(), httpResponse.getMessage());
			}
		}
		catch (IOException e) {
			throw new ServiceException(e);
		}
		catch (FormatException e) {
			throw new ServiceException(e);
		}
		catch (ParseException e) {
			throw new ServiceException(e);
		}
		catch (URISyntaxException e) {
			throw new ServiceException(e);
		}
	}

	private ComplexType buildRequestEnvelope(boolean isInput) {
		Structure envelope = new Structure();
		envelope.setName("Envelope");
		envelope.setProperty(new ValueImpl<Boolean>(new AttributeQualifiedDefaultProperty(), false));
		envelope.setProperty(new ValueImpl<Boolean>(new ElementQualifiedDefaultProperty(), true));
		if (getDefinition().getOperation().getDefinition().getSoapVersion() == 1.2) {
			envelope.setNamespace("http://www.w3.org/2003/05/soap-envelope");
		}
		// default is 1.1
		else {
			envelope.setNamespace("http://schemas.xmlsoap.org/soap/envelope/");
		}
		Structure header = new Structure();
		header.setName("Header");
		envelope.add(new ComplexElementImpl(header, envelope, new ValueImpl<Integer>(new MinOccursProperty(), 0)));
		
		
		Structure body = new Structure();
		body.setName("Body");
		body.setNamespace(envelope.getNamespace());
		if (isInput) {
			// check if we have extensions in place
			if (definition.getExtensions() != null) {
				for (WSExtension extension : definition.getExtensions()) {
					extension.addDefinition(definition, envelope);
				}
			}
			
			// check if we have a header piece
			if (definition.getOperation().getInputPartLayout() != null && definition.getOperation().getInputPartLayout().getHeader() != null && definition.getOperation().getInputPartLayout().getHeader().getParts() != null
					&& !definition.getOperation().getInputPartLayout().getHeader().getParts().isEmpty()) {
				// should only be one
				for (MessagePart part : definition.getOperation().getInputPartLayout().getHeader().getParts()) {
					header.add(TypeBaseUtils.clone(part.getElement(), header));
				}
			}
			
			if (getDefinition().getOperation().getInputPartLayout() != null && getDefinition().getOperation().getInputPartLayout().getBody() != null) {
				List<MessagePart> parts = getDefinition().getOperation().getInputPartLayout().getBody().getParts();
				if (parts != null && !parts.isEmpty()) {
					for (MessagePart part : parts) {
						body.add(TypeBaseUtils.clone(part.getElement(), header));
					}
				}
			}
		}
		else {
			// check if we have a header piece
			if (definition.getOperation().getOutputPartLayout() != null && definition.getOperation().getOutputPartLayout().getHeader() != null && definition.getOperation().getOutputPartLayout().getHeader().getParts() != null
					&& !definition.getOperation().getOutputPartLayout().getHeader().getParts().isEmpty()) {
				// should only be one
				for (MessagePart part : definition.getOperation().getOutputPartLayout().getHeader().getParts()) {
					header.add(TypeBaseUtils.clone(part.getElement(), header));
				}
			}
			
			if (getDefinition().getOperation().getOutputPartLayout() != null && getDefinition().getOperation().getOutputPartLayout().getBody() != null) {
				List<MessagePart> parts = getDefinition().getOperation().getOutputPartLayout().getBody().getParts();
				if (parts != null && !parts.isEmpty()) {
					for (MessagePart part : parts) {
						body.add(TypeBaseUtils.clone(part.getElement(), header));
					}
				}
				List<BindingOperationMessage> faults = getDefinition().getOperation().getFaults();
				if (faults != null && !faults.isEmpty()) {
					for (BindingOperationMessage fault : faults) {
						List<MessagePart> faultParts = fault.getParts();
						if (faultParts != null && !faultParts.isEmpty()) {
							for (MessagePart faultPart : faultParts) {
								body.add(TypeBaseUtils.clone(faultPart.getElement(), header));								
							}
						}
					}
				}
			}
		}
		envelope.add(new ComplexElementImpl(body, envelope));
		return envelope;
	}
	
}
