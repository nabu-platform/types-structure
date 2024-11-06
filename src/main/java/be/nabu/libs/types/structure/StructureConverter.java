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

import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.TypeConverter;
import be.nabu.libs.types.api.TypeInstance;

public class StructureConverter implements TypeConverter {

	@SuppressWarnings({ "unchecked" })
	@Override
	public ComplexContent convert(Object instance, TypeInstance from, TypeInstance to) {
		// if the source type is complex but the instance is not, try converting it first
		if (from.getType() instanceof ComplexType && !(instance instanceof ComplexContent)) {
			instance = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(instance);
		}
		if (!(instance instanceof ComplexContent)) {
			return null;
		}
		
		ComplexContent complexInstance = (ComplexContent) instance;
		// @2023-11-06 could also do an id check but it is unclear if this would cascade into different issues down the line
		//  || (from instanceof DefinedType && to instanceof DefinedType && ((DefinedType) from).getId().equals(((DefinedType) to).getId()))
		if (from.getType().equals(to.getType()))
			return complexInstance;
		
		else if (from.getType() instanceof ComplexType && to.getType() instanceof ComplexType) {
			ComplexType toType = (ComplexType) to.getType();
			// try to upcast
			StructureInstanceUpcastReference upcastReference = StructureInstanceUpcastReference.upcast(complexInstance, toType);
			if (upcastReference != null)
				return upcastReference;
			// if that fails, try to downcast
			else
				return StructureInstanceDowncastReference.downcast(complexInstance, toType);
		}
		else
			return null;
	}

	@Override
	public boolean canConvert(TypeInstance from, TypeInstance to) {
		return from.getType() instanceof ComplexType && to.getType() instanceof ComplexType
			&& (!TypeUtils.getUpcastPath(from.getType(), to.getType()).isEmpty()
					|| !TypeUtils.getUpcastPath(to.getType(), from.getType()).isEmpty());
	}

}
