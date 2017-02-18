package be.nabu.libs.services.wsdl;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.SimpleTypeWrapper;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.wsdl.api.BindingOperation;
import be.nabu.libs.wsdl.api.Message;

public class WSDLService implements DefinedService {

	private String id;
	private BindingOperation operation;
	private Structure input, output;
	private SimpleTypeWrapper wrapper = SimpleTypeWrapperFactory.getInstance().getWrapper();
	private HTTPClientProvider httpClientProvider;
	private Charset charset;
	private List<PredefinedNamespace> namespaces;
	private String username, password;
	private List<Integer> allowedHttpCodes;
	private boolean allowXsi = true, allowDefaultNamespace = true;
	private boolean useFullPathTarget;
	private String endpoint;
	
	public WSDLService(String id, BindingOperation operation, HTTPClientProvider httpClientProvider, Charset charset) {
		this.id = id;
		this.operation = operation;
		this.httpClientProvider = httpClientProvider;
		this.charset = charset;
	}
	
	Structure getInput() {
		if (input == null) {
			input = new Structure();
			input.setName("input");
			input.add(new SimpleElementImpl<String>("endpoint", wrapper.wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
			input.add(new SimpleElementImpl<String>("transactionId", wrapper.wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
			Structure authentication = new Structure();
			authentication.add(new SimpleElementImpl<String>("username", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
			authentication.add(new SimpleElementImpl<String>("password", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
			input.add(new ComplexElementImpl("authentication", authentication, input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
			Message message = operation.getOperation().getInput();
			if (message != null && !message.getParts().isEmpty()) {
				// don't take the actual element name, this makes it harder to do generic mapping
				input.add(new ComplexElementImpl("request", (ComplexType) message.getParts().get(0).getElement().getType(), input));
			}
		}
		return input;
	}
	
	Structure getOutput() {
		if (output == null) {
			output = new Structure();
			output.setName("output");
			// add the actual response
			Message message = operation.getOperation().getOutput();
			if (message != null && !message.getParts().isEmpty()) {
				output.add(new ComplexElementImpl("response", (ComplexType) message.getParts().get(0).getElement().getType(), output));
			}
			// add any faults
			List<Message> faults = operation.getOperation().getFaults();
			if (faults != null && !faults.isEmpty()) {
				if (faults.size() > 1) {
					throw new RuntimeException("No support yet for multiple faults");
				}
				output.add(new ComplexElementImpl("fault", (ComplexType) faults.get(0).getParts().get(0).getElement().getType(), output));
			}
		}
		return output;
	}
	
	@Override
	public ServiceInterface getServiceInterface() {
		return new ServiceInterface() {
			@Override
			public ComplexType getInputDefinition() {
				return getInput();
			}
			@Override
			public ComplexType getOutputDefinition() {
				return getOutput();
			}
			@Override
			public ServiceInterface getParent() {
				return null;
			}
		};
	}
	
	BindingOperation getOperation() {
		return operation;
	}
	
	@Override
	public WSDLServiceInstance newInstance() {
		return new WSDLServiceInstance(this);
	}

	@Override
	public Set<String> getReferences() {
		return new HashSet<String>();
	}

	@Override
	public String getId() {
		return id;
	}

	public HTTPClientProvider getHttpClientProvider() {
		return httpClientProvider;
	}

	public Charset getCharset() {
		return charset == null ? Charset.defaultCharset() : charset;
	}

	public List<PredefinedNamespace> getNamespaces() {
		return namespaces;
	}

	public void setNamespaces(List<PredefinedNamespace> namespaces) {
		this.namespaces = namespaces;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<Integer> getAllowedHttpCodes() {
		return allowedHttpCodes;
	}

	public void setAllowedHttpCodes(List<Integer> allowedHttpCodes) {
		this.allowedHttpCodes = allowedHttpCodes;
	}

	public boolean isAllowXsi() {
		return allowXsi;
	}
	public void setAllowXsi(boolean allowXsi) {
		this.allowXsi = allowXsi;
	}

	public boolean isAllowDefaultNamespace() {
		return allowDefaultNamespace;
	}
	public void setAllowDefaultNamespace(boolean allowDefaultNamespace) {
		this.allowDefaultNamespace = allowDefaultNamespace;
	}

	public boolean isUseFullPathTarget() {
		return useFullPathTarget;
	}
	public void setUseFullPathTarget(boolean useFullPathTarget) {
		this.useFullPathTarget = useFullPathTarget;
	}

	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

}
