package be.nabu.libs.services.wsdl.api;

import java.util.List;
import java.util.Map;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.wsdl.WSDLService;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ModifiableComplexType;

public interface WSExtension {
	public void addDefinition(WSDLService service, ModifiableComplexType envelope, Value<?>...values);
	public void addInstance(WSDLService service, ComplexContent envelope, ComplexContent serviceInput, Value<?>...values);
	public Map<String, String> getPreferredPrefixes();
	public List<Property<?>> getSupportedProperties();
}
