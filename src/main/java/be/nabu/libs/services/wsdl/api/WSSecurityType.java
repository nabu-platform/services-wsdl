package be.nabu.libs.services.wsdl.api;

public enum WSSecurityType {
	PasswordText("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText"), 
	PasswordDigest("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest");
	
	private String type;

	private WSSecurityType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	
}
