package be.nabu.libs.services.wsdl.ws;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.wsdl.WSDLService;
import be.nabu.libs.services.wsdl.api.WSExtension;
import be.nabu.libs.services.wsdl.api.WSSecurityType;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.NamespaceProperty;
import be.nabu.libs.types.properties.TimezoneProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.impl.Base64Encoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.security.DigestAlgorithm;
import be.nabu.utils.security.SecurityUtils;

public class WSSecurity implements WSExtension {
	
	public static final String WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
	public static final String WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
	
	private WSSecurityType wsSecurityType;
	// when a duration is configured, the timestamp is added
	private Long timestampDuration;
	
	private TimeZone timezone;
	
	@Override
	public void addDefinition(WSDLService service, ModifiableComplexType envelope, Value<?>...values) {
		((ModifiableComplexType) envelope.get("Header").getType()).add(new ComplexElementImpl("Security", newDefinition(service.getSoapNamespace(), timezone), envelope));
	}
	
	@Override
	public void addInstance(WSDLService definition, ComplexContent envelope, ComplexContent input, Value<?>...values) {
		WSSecurityType value = ValueUtils.getValue(WSSecurityTypeProperty.getInstance(), values);
		if (value == null) {
			value = wsSecurityType == null ? WSSecurityType.PasswordText : wsSecurityType;
		}
		
		final String username = input == null || input.get("authentication/username") == null ? definition.getUsername() : (String) input.get("authentication/username");
		final String password = input == null || input.get("authentication/password") == null ? definition.getPassword() : (String) input.get("authentication/password");
		
		
		if (username != null) {
			// extract this to properties later on
			boolean useNonce = true;
			boolean useCreated = true;
			
			Date created = new Date();
			if (useCreated) {
				envelope.set("Header/Security/UsernameToken/Created", created);
			}
			
			if (timestampDuration != null) {
				envelope.set("Header/Security/Timestamp/Created", created);
				envelope.set("Header/Security/Timestamp/Expires", new Date(created.getTime() + timestampDuration));
				envelope.set("Header/Security/Timestamp/@Id", "Timestamp-" + UUID.randomUUID().toString().replace("-", ""));
			}
			
			
			String nonce = null;
			if (useNonce) {
				nonce = UUID.randomUUID().toString().replace("-", "");
				byte[] nonceBytes = nonce.getBytes(Charset.forName("UTF-8"));
				try {
					String result = new String(IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(nonceBytes, true), new Base64Encoder())), Charset.forName("UTF-8"));
					envelope.set("Header/Security/UsernameToken/Nonce/$value", result);
					envelope.set("Header/Security/UsernameToken/Nonce/@EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			envelope.set("Header/Security/@mustUnderstand", 1);
			envelope.set("Header/Security/UsernameToken/@Id", "UsernameToken-" + UUID.randomUUID().toString().replace("-", ""));
			envelope.set("Header/Security/UsernameToken/Username", username);
			
			if (password != null) {
				switch (value) {
					case PasswordText:
						envelope.set("Header/Security/UsernameToken/Password/$value", password);
					break;
					case PasswordDigest:
						try {
							// Password_Digest = Base64 ( SHA-1 ( nonce + created + password ) )
							String toDigest = password;
							if (useCreated) {
								Element<?> element = envelope.getType().get("Header/Security/UsernameToken/Created");
								toDigest = ((Marshallable<Date>) element.getType()).marshal(created, element.getProperties()) + toDigest;
							}
							if (useNonce) {
								toDigest = nonce + toDigest;
							}
							byte[] digest = SecurityUtils.digest(new ByteArrayInputStream(toDigest.getBytes(Charset.forName("UTF-8"))), DigestAlgorithm.SHA1);
							String result = new String(IOUtils.toBytes(TranscoderUtils.transcodeBytes(IOUtils.wrap(digest, true), new Base64Encoder())), Charset.forName("UTF-8"));
							envelope.set("Header/Security/UsernameToken/Password/$value", result);
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					break;
				}
			}
			envelope.set("Header/Security/UsernameToken/Password/@Type", value.getType());
		}
	}
	
	@Override
	public Map<String, String> getPreferredPrefixes() {
		Map<String, String> prefixes = new HashMap<String, String>();
		prefixes.put(WSSE, "wsse");
		prefixes.put(WSU, "wsu");
		return prefixes;
	}
	
	@Override
	public List<Property<?>> getSupportedProperties() {
		List<Property<?>> list = new ArrayList<Property<?>>();
		list.add(WSSecurityTypeProperty.getInstance());
		return list;
	}
	
	public static Structure newDefinition(String soapNamespace, TimeZone timezone) {
		DefinedSimpleType<String> string = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
		Structure security = new Structure();
		security.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), true));
		security.setName("Security");
		security.setNamespace(WSSE);
		
		security.add(new SimpleElementImpl<Integer>("@mustUnderstand", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Integer.class), security, 
				new ValueImpl<String>(NamespaceProperty.getInstance(), soapNamespace)));
		
		Structure token = new Structure();
		security.add(new ComplexElementImpl("UsernameToken", token, security));
		token.setNamespace(WSSE);
		
		token.add(new SimpleElementImpl<String>("@Id", string, token,
				new ValueImpl<String>(NamespaceProperty.getInstance(), WSU)));
		
		token.add(new SimpleElementImpl<String>("Username", string, token,
				new ValueImpl<String>(NamespaceProperty.getInstance(), WSSE)));
		
//		token.add(new SimpleElementImpl<String>("Username", string, token,
//				new ValueImpl<String>(NamespaceProperty.getInstance(), WSSE)));

		Structure password = new Structure();
		password.add(new SimpleElementImpl<String>(ComplexType.SIMPLE_TYPE_VALUE, string, password));
		password.add(new SimpleElementImpl<String>("@Type", string, password));
		token.add(new ComplexElementImpl("Password", password, token));

		Structure nonce = new Structure();
		nonce.add(new SimpleElementImpl<String>(ComplexType.SIMPLE_TYPE_VALUE, string, nonce));
		nonce.add(new SimpleElementImpl<String>("@EncodingType", string, nonce));
		token.add(new ComplexElementImpl("Nonce", nonce, token));
		
		token.add(new SimpleElementImpl<Date>("Created", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class), token,
				new ValueImpl<String>(NamespaceProperty.getInstance(), WSU),
				new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone)));
		
		Structure timestamp = new Structure();
		timestamp.setNamespace(WSU);
		timestamp.add(new SimpleElementImpl<String>("@Id", string, token,
				new ValueImpl<String>(NamespaceProperty.getInstance(), WSU)));
		timestamp.add(new SimpleElementImpl<Date>("Created", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class), token,
				new ValueImpl<String>(NamespaceProperty.getInstance(), WSU),
				new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone)));
		timestamp.add(new SimpleElementImpl<Date>("Expires", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class), token,
				new ValueImpl<String>(NamespaceProperty.getInstance(), WSU),
				new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone)));
		security.add(new ComplexElementImpl("Timestamp", timestamp, security));
		
		return security;
	}

	public WSSecurityType getWsSecurityType() {
		return wsSecurityType;
	}

	public void setWsSecurityType(WSSecurityType wsSecurityType) {
		this.wsSecurityType = wsSecurityType;
	}

	public Long getTimestampDuration() {
		return timestampDuration;
	}

	public void setTimestampDuration(Long timestampDuration) {
		this.timestampDuration = timestampDuration;
	}

	public TimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}
}
