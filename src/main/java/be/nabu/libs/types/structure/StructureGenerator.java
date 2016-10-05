package be.nabu.libs.types.structure;

import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.ModifiableComplexTypeGenerator;

public class StructureGenerator implements ModifiableComplexTypeGenerator {
	@Override
	public ModifiableComplexType newComplexType() {
		Structure structure = new Structure();
		structure.setName("anonymous");
		return structure;
	}
}
