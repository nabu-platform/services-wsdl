package be.nabu.libs.services.wsdl;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.nabu.libs.http.api.WebAuthorizationType;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.wsdl.api.WSExtension;
import be.nabu.libs.wsdl.api.BindingOperation;

public class WSDLService implements DefinedService {

	private String id;
	private BindingOperation operation;
	private HTTPClientProvider httpClientProvider;
	private Charset charset;
	private List<PredefinedNamespace> namespaces;
	private String username, password;
	private List<Integer> allowedHttpCodes;
	private boolean allowXsi = true, allowDefaultNamespace = true;
	private boolean useFullPathTarget;
	private String endpoint;
	private boolean backwardsCompatible = false;
	private WSDLInterface iface;
	private WebAuthorizationType preemptiveAuthorizationType;
	private List<WSExtension> extensions;
	
	public WSDLService(String id, BindingOperation operation, HTTPClientProvider httpClientProvider, Charset charset) {
		this.id = id;
		this.operation = operation;
		this.httpClientProvider = httpClientProvider;
		this.charset = charset;
		this.iface = new WSDLInterface(id, operation);
	}
	
	@Override
	public ServiceInterface getServiceInterface() {
		return iface;
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

	public boolean isBackwardsCompatible() {
		return backwardsCompatible;
	}
	public void setBackwardsCompatible(boolean backwardsCompatible) {
		this.backwardsCompatible = backwardsCompatible;
	}

	public WebAuthorizationType getPreemptiveAuthorizationType() {
		return preemptiveAuthorizationType;
	}

	public void setPreemptiveAuthorizationType(WebAuthorizationType preemptiveAuthorizationType) {
		this.preemptiveAuthorizationType = preemptiveAuthorizationType;
	}
	
	public String getSoapNamespace() {
		if (getOperation().getDefinition().getSoapVersion() == 1.2) {
			return "http://www.w3.org/2003/05/soap-envelope";
		}
		// default is 1.1
		else {
			return "http://schemas.xmlsoap.org/soap/envelope/";
		}
	}

	public List<WSExtension> getExtensions() {
		return extensions;
	}

	public void setExtensions(List<WSExtension> extensions) {
		this.extensions = extensions;
	}
	
}
