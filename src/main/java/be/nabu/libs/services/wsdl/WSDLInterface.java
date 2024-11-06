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

import java.util.List;

import be.nabu.libs.services.api.DefinedServiceInterface;
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
import be.nabu.libs.wsdl.api.BindingOperationMessage;
import be.nabu.libs.wsdl.api.BindingOperationMessageLayout;
import be.nabu.libs.wsdl.api.Message;
import be.nabu.libs.wsdl.api.MessagePart;

public class WSDLInterface implements DefinedServiceInterface {

	private String id;
	private BindingOperation operation;
	private SimpleTypeWrapper wrapper = SimpleTypeWrapperFactory.getInstance().getWrapper();
	private boolean backwardsCompatible;
	private Structure input, output;

	public WSDLInterface(String id, BindingOperation operation) {
		this.id = id;
		this.operation = operation;
	}
	
	@Override
	public ComplexType getInputDefinition() {
		if (input == null) {
			synchronized(this) {
				if (input == null) {
					Structure input = new Structure();
					input.setName("input");
					input.add(new SimpleElementImpl<String>("endpoint", wrapper.wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					input.add(new SimpleElementImpl<String>("transactionId", wrapper.wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					Structure authentication = new Structure();
					authentication.add(new SimpleElementImpl<String>("username", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					authentication.add(new SimpleElementImpl<String>("password", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					input.add(new ComplexElementImpl("authentication", authentication, input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
					BindingOperationMessageLayout inputPartLayout = operation.getInputPartLayout();
					
					// we want to stay backwards compatible, in the original usecase we assumed each operation had exactly one input part which went in the body
					// later we expanded to allow a header part and _multiple_ body parts
					if (backwardsCompatible && inputPartLayout.getHeader() == null && inputPartLayout.getBody() != null && inputPartLayout.getBody().getParts() != null && inputPartLayout.getBody().getParts().size() == 1) {
						Message message = operation.getOperation().getInput();
						if (message != null && !message.getParts().isEmpty()) {
							// don't take the actual element name, this makes it harder to do generic mapping
							input.add(new ComplexElementImpl("request", (ComplexType) message.getParts().get(0).getElement().getType(), input));
						}
					}
					else {
						// check if we have a header part, there should be only one (according to WSDL spec) but i would rather be flexible...
						if (inputPartLayout.getHeader() != null && inputPartLayout.getHeader().getParts() != null && !inputPartLayout.getHeader().getParts().isEmpty()) {
							Structure headerInput = new Structure();
							headerInput.setName("header");
							for (MessagePart part : inputPartLayout.getHeader().getParts()) {
								headerInput.add(new ComplexElementImpl(part.getElement().getName(), (ComplexType) part.getElement().getType(), headerInput));
							}
							input.add(new ComplexElementImpl("header", headerInput, input));
						}
						// check if we have a header part, there should be only one (according to WSDL spec) but i would rather be flexible...
						if (inputPartLayout.getBody() != null && inputPartLayout.getBody().getParts() != null && !inputPartLayout.getBody().getParts().isEmpty()) {
							Structure bodyInput = new Structure();
							bodyInput.setName("body");
							for (MessagePart part : inputPartLayout.getBody().getParts()) {
								bodyInput.add(new ComplexElementImpl(part.getElement().getName(), (ComplexType) part.getElement().getType(), bodyInput));
							}
							input.add(new ComplexElementImpl("body", bodyInput, input));
						}
					}
					this.input = input;
				}
			}
		}
		return input;
	}
	
	@Override
	public ComplexType getOutputDefinition() {
		if (output == null) {
			synchronized(this) {
				if (output == null) {
					Structure output = new Structure();
					output.setName("output");
					if (backwardsCompatible) {
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
					else {
						BindingOperationMessage body = operation.getOutputPartLayout().getBody();
						if (body != null) {
							Structure bodyOutput = new Structure();
							bodyOutput.setName("body");
							for (MessagePart part : body.getParts()) {
								bodyOutput.add(new ComplexElementImpl(part.getElement().getName(), (ComplexType) part.getElement().getType(), bodyOutput));
							}
							output.add(new ComplexElementImpl("body", bodyOutput, output));
						}
						BindingOperationMessage header = operation.getOutputPartLayout().getHeader();
						if (header != null) {
							Structure headerOutput = new Structure();
							headerOutput.setName("header");
							for (MessagePart part : header.getParts()) {
								headerOutput.add(new ComplexElementImpl(part.getElement().getName(), (ComplexType) part.getElement().getType(), headerOutput));
							}
							output.add(new ComplexElementImpl("header", headerOutput, output));
						}
						List<BindingOperationMessage> faults = operation.getFaults();
						if (faults != null && !faults.isEmpty()) {
							Structure faultOutput = new Structure();
							faultOutput.setName("fault");
							for (BindingOperationMessage fault : faults) {
								for (MessagePart part : fault.getParts()) {
									faultOutput.add(new ComplexElementImpl(part.getElement().getName(), (ComplexType) part.getElement().getType(), faultOutput));
								}
							}
							output.add(new ComplexElementImpl("fault", faultOutput, output));
						}
					}
					this.output = output;
				}
			}
		}
		return output;
	}
	
	@Override
	public ServiceInterface getParent() {
		return null;
	}

	@Override
	public String getId() {
		return id;
	}

	public BindingOperation getOperation() {
		return operation;
	}

}