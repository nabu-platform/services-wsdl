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
