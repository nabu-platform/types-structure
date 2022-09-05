package be.nabu.libs.types.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedTypeResolver;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Group;
import be.nabu.libs.types.api.ModifiableComplexType;
import be.nabu.libs.types.api.RestrictableComplexType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.BaseType;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.DuplicateProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.types.properties.NamespaceProperty;
import be.nabu.libs.types.properties.RestrictProperty;
import be.nabu.libs.types.properties.ValidateProperty;
import be.nabu.libs.validator.MultipleValidator;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.libs.validator.api.Validator;

/**
 * A, B extends A
 * 
 * A a = new B();	// upcast
 * B b = (B) a;		// downcast
 * 
 * @author alex
 *
 */
public class Structure extends BaseType<StructureInstance> implements ComplexType, ModifiableComplexType, RestrictableComplexType {
	
	/**
	 * Used to resolve any types it extends
	 */
	private DefinedTypeResolver referenceResolver;
	
	private boolean attributeQualified = false, elementQualified = false;
	
	private List<Group> groups = new ArrayList<Group>();
	
	private Map<Property<?>, Value<?>> values = new HashMap<Property<?>, Value<?>>();
		
	/**
	 * Used to be a linkedhashmap but updates to element names can break be annoying
	 */
	private List<Element<?>> children = new ArrayList<Element<?>>();
	
	/**
	 * Contains restrictions placed upon the referenced structure
	 */
	private Map<String, Element<?>> restrictions = new HashMap<String, Element<?>>();
	
	public Structure() {
		// auto create
	}
	
	public Structure(Value<?>...values) {
		setProperty(values);
	}
	/**
	 * Whether or not this is an aspect
	 */
	private boolean isAspect = false;
	
	public DefinedTypeResolver getReferenceResolver() {
		return referenceResolver;
	}

	public void setReferenceResolver(DefinedTypeResolver referenceResolver) {
		this.referenceResolver = referenceResolver;
	}

	
	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>...values) {
		Set<Property<?>> properties = super.getSupportedProperties(values);
		properties.add(SuperTypeProperty.getInstance());
		properties.add(ValidateProperty.getInstance());
		properties.add(RestrictProperty.getInstance());
		properties.add(DuplicateProperty.getInstance());
		return properties;
	}
		
	@Override
	public Element<?> get(String path) {
		// if we ask for an ANY element, check if there is an element of type java.lang.Object
		// for definition time this works, but at runtime this does not work atm because we can't set any items in an Object.class obviously
		// we would need to switch it out with a dynamic instance that creates elements as they are set? this brings into question data types etc, so currenlty disabling this until we get a better view on the requirements
//		if (path.equals(NameProperty.ANY)) {
//			for (Element<?> child : this) {
//				if (child.getType() instanceof BeanType && ((BeanType<?>) child.getType()).getBeanClass().equals(Object.class)) {
//					return child;
//				}
//			}
//			if (getSuperType() instanceof ComplexType) {
//				return ((ComplexType) getSuperType()).get(path);
//			}
//		}
		return TypeUtils.getChild(this, path);
	}

	@Override
	public List<ValidationMessage> add(Element<?> element) {
		List<ValidationMessage> list = new ArrayList<ValidationMessage>();
		for (Element<?> child : children) {
			if (child.getName().equals(element.getName())) {
				list.add(new ValidationMessage(Severity.ERROR, "The element with name " + element.getName() + " already exists, it can not be added twice"));
			}
		}
		if (list.isEmpty()) {
			boolean isRestricted = false;
			if (getSuperType() instanceof ComplexType) {
				Element<?> parentElement = ((ComplexType) getSuperType()).get(element.getName());
				if (parentElement != null) {
					String value = ValueUtils.getValue(RestrictProperty.getInstance(), getProperties());
					// if we restrict it from the parent, we can redefine it as we please
					List<String> restricted = value == null || value.trim().isEmpty() ? new ArrayList<String>() : Arrays.asList(value.trim().split("[\\s]*,[\\s]*"));
					if (restricted.indexOf(element.getName()) < 0) {
						restrictions.put(element.getName(), element);
						isRestricted = true;
					}
				}
			}
			// always add it? otherwise it does not show up in the iterable!
//			if (!isRestricted) {
				children.add(element);
//			}
		}
		return list;
	}
	
	public void setSuperType(String id) {
		setSuperType(getReferenceResolver().resolve(id));
	}
	
	public void setSuperType(Type superType) {
		setProperty(new ValueImpl<Type>(SuperTypeProperty.getInstance(), superType));
	}
	
	@Override
	public Type getSuperType() {
		return ValueUtils.getValue(SuperTypeProperty.getInstance(), getProperties());
	}

	@Override
	public StructureInstance newInstance() {
		// need to instantiate a structure instance per level of extension
		// this allows for upcasting later on
		if (getSuperType() instanceof ComplexType)
			return new StructureInstanceDowncastReference(((ComplexType) getSuperType()).newInstance(), this);
		else {
			return new StructureInstance(this);
		}
	}

	public boolean isAspect() {
		return isAspect;
	}

	public void setAspect(boolean isAspect) {
		this.isAspect = isAspect;
	}
	
	public boolean isEmpty() {
		return children.size() == 0;
	}

	@Override
	public Iterator<Element<?>> iterator() {
		// it is modified directly by some users, so it must return a correct iterator (looking at you elementtreeitem:move)
		return children.iterator();
	}

	@Override
	public String toString() {
		return (getSuperType() == null ? "" : getSuperType().toString() + " > ") +
			(getName() == null ? "structure[anonymous]" : "structure[anonymous:" + getName() + ":" + hashCode() + "]");
	}

	@Override
	public String getName(Value<?>...values) {
		return ValueUtils.getValue(NameProperty.getInstance(), getProperties());
	}

	@Override
	public String getNamespace(Value<?>... values) {
		return ValueUtils.getValue(NamespaceProperty.getInstance(), getProperties());
	}

	@Override
	public void setName(String name) {
		setProperty(new ValueImpl<String>(new NameProperty(), name));
	}

	@Override
	public void setNamespace(String namespace) {
		setProperty(new ValueImpl<String>(new NamespaceProperty(), namespace));
	}

	@Override
	public Boolean isAttributeQualified(Value<?>... values) {
		if (ValueUtils.contains(new AttributeQualifiedDefaultProperty(), values))
			return ValueUtils.getValue(AttributeQualifiedDefaultProperty.getInstance(), values);
		else
			return attributeQualified;
	}

	@Override
	public Boolean isElementQualified(Value<?>... values) {
		if (ValueUtils.contains(new ElementQualifiedDefaultProperty(), values))
			return ValueUtils.getValue(ElementQualifiedDefaultProperty.getInstance(), values);
		else
			return elementQualified;
	}

	@Override
	public void remove(Element<?> element) {
		children.remove(element);
	}
	
	public void removeAll() {
		children.clear();
	}

	@Override
	public List<ValidationMessage> add(Group group) {
		groups.add(group);
		return new ArrayList<ValidationMessage>();
	}

	@Override
	public void remove(Group group) {
		groups.remove(group);
	}

	@Override
	public Group[] getGroups() {
		return groups.toArray(new Group [groups.size()]);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Validator<StructureInstance> createValidator(Value<?>...values) {
		List<Validator> validators = new ArrayList<Validator>();
		validators.add(super.createValidator(values));
		validators.add(TypeUtils.createComplexValidator(this));
		for (Group group : groups)
			validators.add(group.createValidator(values));
		return new MultipleValidator<StructureInstance>(validators.toArray(new Validator[validators.size()]));
	}

	@Override
	public Element<?> getRestriction(String childName) {
		return restrictions.get(childName);
	}

	@Override
	public Value<?>[] getProperties() {
		return values.values().toArray(new Value[values.size()]);
	}

	@Override
	public void setProperty(Value<?>... values) {
		for (Value<?> value : values)
			this.values.put(value.getProperty(), value);
	}
	
	public static ComplexContent cast(ComplexContent content, ComplexType to) {
		ComplexType original = content.getType();
		if (to.equals(original))
			return content;
		ComplexContent cast = StructureInstanceDowncastReference.downcast(content, to);
		if (cast == null)
			cast = StructureInstanceUpcastReference.upcast(content, to);
		return cast;
	}

}

