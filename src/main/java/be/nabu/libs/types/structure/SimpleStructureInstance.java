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

import be.nabu.libs.types.api.ComplexType;

/**
 * This is much like a regular structure except it has an unnamed value field
 * Note that the value field can be get & set using the reserved "$value" name
 */
public class SimpleStructureInstance extends StructureInstance {
	
	/**
	 * The value it holds
	 */
	private Object value;

	@Override
	public Object get(String path) {
		if (path.equals(ComplexType.SIMPLE_TYPE_VALUE))
			return value;
		else
			return super.get(path);
	}

	@Override
	public void set(String path, Object value) {
		if (path.equals(ComplexType.SIMPLE_TYPE_VALUE))
			this.value = value;
		else
			super.set(path, value);
	}
	
	
}
