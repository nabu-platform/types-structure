package be.nabu.libs.types.structure;

import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MaxOccursProperty;

public class UntypedStructure extends Structure implements ComplexType, ModifiableComplexType, SimpleType<StructureInstance> {

	@Override
	public Element<?> get(String path) {
		Element<?> existing = super.get(path);
		if (existing == null) {
			ParsedPath parsed = ParsedPath.parse(path);
			String accessor = parsed.getName();
			if (parsed.getChildPath() != null) {
				if (parsed.getIndex() != null) {
					add(new ComplexElementImpl(accessor, new UntypedStructure(), this, new ValueImpl<Integer>(new MaxOccursProperty(), 0)));
				}
				else {
					add(new ComplexElementImpl(accessor, new UntypedStructure(), this));
				}
			}
			else {
				add(new SimpleElementImpl<String>(accessor, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), this));
			}
		}
		return super.get(path);
	}

	@Override
	public Class<StructureInstance> getInstanceClass() {
		return StructureInstance.class;
	}
	
}
