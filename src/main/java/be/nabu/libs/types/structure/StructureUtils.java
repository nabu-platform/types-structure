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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.RestrictProperty;
import be.nabu.libs.types.properties.ScopeProperty;

public class StructureUtils {
	
	public static ComplexType scope(ComplexType type) {
		return scope(type, new HashMap<ComplexType, Structure>(), new ArrayList<Structure>());
	}

	// we need to keep track of already restricted complex types to avoid circular references scoping eternally
	private static ComplexType scope(ComplexType type, Map<ComplexType, Structure> restricted, List<Structure> used) {
		Structure newComplexType = null;
		// if we already restricted it, we are dealing with a circular reference, we add it to the used to mark it as such
		if (restricted.containsKey(type)) {
			newComplexType = restricted.get(type);
			used.add(newComplexType);
			// no need to restrict if further, whoever added it to the map will make sure it is OK
			return newComplexType;
		}
		// otherwise we immediately register a new type, so we can make sure our recursive resolving reuses this type instead of going all circular
		else {
			newComplexType = new Structure();
			newComplexType.setProperty(type.getProperties());
			newComplexType.setSuperType(type);
			restricted.put(type, newComplexType);
		}
		// this keeps track of the elements that have to be restricted and optionally replaced with
		Map<String, Element<?>> restrictions = new HashMap<String, Element<?>>();
		for (Element<?> child : TypeUtils.getAllChildren(type)) {
			switch(ValueUtils.getValue(ScopeProperty.getInstance(), child.getProperties())) {
				// we need to restrict private fields
				case PRIVATE:
					// replace with nothing
					restrictions.put(child.getName(), null);
				break;
				case PUBLIC:
					// recurse
					if (child.getType() instanceof ComplexType) {
						ComplexType scoped = scope((ComplexType) child.getType(), restricted, used);
						if (!scoped.equals(child.getType())) {
							restrictions.put(child.getName(), new ComplexElementImpl(child.getName(), scoped, newComplexType, child.getProperties()));
						}
					}
				break;
			}
		}
		// if we have no restrictions and we didn't use the complex type in a recursion, we should be in the clear
		if (restrictions.isEmpty() && !used.contains(newComplexType)) {
			restricted.remove(type);
			return type;
		}
		else {
			// add the restrictions
			if (!restrictions.isEmpty()) {
				StringBuilder builder = new StringBuilder();
				for (String key : restrictions.keySet()) {
					if (!builder.toString().isEmpty()) {
						builder.append(",");
					}
					builder.append(key);
				}
				newComplexType.setProperty(new ValueImpl<String>(RestrictProperty.getInstance(), builder.toString()));
				// once restricted, add any remaining new items
				for (String key : restrictions.keySet()) {
					if (restrictions.get(key) != null) {
						newComplexType.add(restrictions.get(key));
					}
				}
			}
		}
		return newComplexType;
	}
}
