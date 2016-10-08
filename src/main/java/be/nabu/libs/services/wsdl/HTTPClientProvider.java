package be.nabu.libs.services.wsdl;

import be.nabu.libs.http.api.client.HTTPClient;

public interface HTTPClientProvider {
	public HTTPClient newHTTPClient(String transactionId);
}
