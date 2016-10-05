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
