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

package be.nabu.libs.types.structure;

import java.util.LinkedHashSet;
import java.util.Set;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.SimpleElementImpl;


/**
 * A simple structure is the representation of an xml complextype which extends base simple content
 * For example if you have a string and you want to add an attribute, this results in a structure
 * as only structures can have child fields.
 * 
 * @author alex
 *
 */
public class SimpleStructure<T> extends Structure implements SimpleType<T> {

	/**
	 * This contains the definition of the value field for this simple structure
	 */
	private SimpleType<T> valueField;
	
	public SimpleStructure(SimpleType<T> valueField) {
		this.valueField = valueField;
	}
	
	@Override
	public Element<?> get(String path) {
		if (path.equals(SIMPLE_TYPE_VALUE))
			return new SimpleElementImpl<T>(valueField instanceof DefinedType ? ((DefinedType) valueField).getName() : "anonymous", valueField, null) {
				// someone might update the type, we need to capture that
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void setType(Type type) {
					valueField = (SimpleType) type;
					super.setType(type);
				}
			};
		else
			return super.get(path);
	}

	@Override
	public StructureInstance newInstance() {
		SimpleStructureInstance instance = new SimpleStructureInstance();
		instance.setType(this);
		return instance;
	}

	@Override
	public Class<T> getInstanceClass() {
		return valueField.getInstanceClass();
	}

	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>... values) {
		Set<Property<?>> supported = new LinkedHashSet<Property<?>>();
		supported.addAll(super.getSupportedProperties(values));
		supported.addAll(valueField.getSupportedProperties(values));
		return supported;
	}
	
}
