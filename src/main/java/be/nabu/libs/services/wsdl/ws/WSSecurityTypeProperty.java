package be.nabu.libs.services.wsdl.ws;

import be.nabu.libs.services.wsdl.api.WSSecurityType;
import be.nabu.libs.types.properties.SimpleProperty;

public class WSSecurityTypeProperty extends SimpleProperty<WSSecurityType> {

	private static WSSecurityTypeProperty instance = new WSSecurityTypeProperty();
	
	public static WSSecurityTypeProperty getInstance() {
		return instance;
	}
	
	public WSSecurityTypeProperty() {
		super(WSSecurityType.class);
	}

	@Override
	public String getName() {
		return "wsSecurityType";
	}
	
}
