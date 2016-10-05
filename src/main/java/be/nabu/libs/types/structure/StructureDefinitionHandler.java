package be.nabu.libs.types.structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.converter.api.Converter;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeResolver;
import be.nabu.libs.types.api.ModifiableType;
import be.nabu.libs.types.api.SimpleTypeWrapper;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;

public class StructureDefinitionHandler {
	
//	private DefinedTypeResolver resolver;
//	private SimpleTypeWrapper wrapper;
//	private Converter converter;
//	
//	public StructureDefinitionHandler(DefinedTypeResolver resolver) {
//		this.resolver = resolver;
//	}
//	
//	private boolean hasAttribute(Element element, String name) {
//		String value = element.getAttribute(name);
//		return value != null && !value.isEmpty();
//	}
//	
//	public Converter getConverter() {
//		if (converter == null) {
//			converter = ConverterFactory.getInstance().getConverter();
//		}
//		return converter;
//	}
//
//	public void setConverter(Converter converter) {
//		this.converter = converter;
//	}
//
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public Structure parse(Element element) {
//		Structure structure;
//
//		try {
//			// a simple type
//			if (element.getNodeName().equals("simple")) {
//				if (hasAttribute(element, "name")) {
//					DefinedSimpleStructure simpleStructure = new DefinedSimpleStructure(getWrapper().wrap(Class.forName(element.getAttribute("value"))));
//					simpleStructure.setName(element.getAttribute("name"));
//					simpleStructure.setNamespace(element.getAttribute("namespace"));
//					structure = simpleStructure;
//				}
//				else
//					structure = new SimpleStructure(getWrapper().wrap(Class.forName(element.getAttribute("value"))));
//			}
//			else  {
//				if (hasAttribute(element, "name")) {
//					DefinedStructure definedStructure = new DefinedStructure();
//					definedStructure.setName(element.getAttribute("name"));
//					definedStructure.setNamespace(element.getAttribute("namespace"));
//					structure = definedStructure;
//				}
//				else
//					structure = new Structure();
//			}
//		}
//		catch (ClassNotFoundException e) {
//			throw new RuntimeException(e);
//		}
//		parse(element, structure);
//		return structure;
//	}
//	
//	public void parse(Element element, Structure structure) {
//		
//	}
//	
//	public static void load(be.nabu.eai.repository.nodes.Node node, String version, DefinedStructure root) throws IOException, RepositoryException {
//		InputStream input = node.getLocator().getInputStream(DESCRIPTOR, version);
//		root.setNode(node);
//		try {
//			Document document = XMLUtils.toDocument(input);
//			XMLUtils.validate(document, getSchema());
//			load(node, document.getDocumentElement(), root, 0);
//		}
//		catch (SAXException e) {
//			throw new IOException(e);
//		}
//		catch (ParserConfigurationException e) {
//			throw new IOException(e);
//		}
//		catch (ClassNotFoundException e) {
//			throw new RepositoryException(e);
//		}
//		catch (InstantiationException e) {
//			throw new RepositoryException(e);
//		}
//		catch (IllegalAccessException e) {
//			throw new RepositoryException(e);
//		}
//		finally {
//			input.close();
//		}
//	}
//	
//	/**
//	 * You can use this public method to load a structure that is part of another definition
//	 * @param node
//	 * @param element
//	 * @param parent
//	 * @throws ClassNotFoundException
//	 * @throws InstantiationException
//	 * @throws IllegalAccessException
//	 * @throws DOMException
//	 * @throws RepositoryException
//	 */
//	public static void load(be.nabu.eai.repository.nodes.Node node, Element element, Structure parent) throws ClassNotFoundException, InstantiationException, IllegalAccessException, DOMException, RepositoryException {
//		load(node, element, parent, 0);
//	}
//	/**
//	 * 
//	 * @param node The node that holds the definition of the structure, this does not necessarily have to be an actual structure node
//	 * 		Structures are used throughout as parts of another node (e.g. services, interfaces,...)
//	 * 		The reason you pass in the node is so it can be used to access the repository and allow for node-based versioning etc
//	 * @param element
//	 * @param parent
//	 * @param location Indicates from which child element to start, usually 0
//	 * @throws ClassNotFoundException
//	 * @throws InstantiationException
//	 * @throws IllegalAccessException
//	 * @throws DOMException 
//	 * @throws RepositoryException 
//	 */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private void load(be.nabu.eai.repository.nodes.Node node, Element element, Structure parent, int location) throws ClassNotFoundException, InstantiationException, IllegalAccessException, DOMException, RepositoryException {
//		setItemAttributes(element, parent);
//		for (int i = location; i < element.getChildNodes().getLength(); i++) {
//			if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
//				Element child = (Element) element.getChildNodes().item(i);
//				if (child.getLocalName().equals("structure") || child.getLocalName().equals("aspect")) {
//					// if no type is defined, it is a default base structure
//					// otherwise it may be part of another node
//					Structure item = child.hasAttribute("type") ? (Structure) Class.forName(child.getAttribute("type")).newInstance() : new Structure();
//					item.setAspect(child.getLocalName().equals("aspect"));
//					item.setNode(node);
//					// need to set the parent because the load() sequence requires a reference to the parent to resolve structure references!
//					// this allows us to intercept the resolving and for example resolve partial local structures instead of redirecting to the repository
//					item.setParent(parent);
//					load(node, child, item, 0);
//					parent.add(item);
//				}
//				else if (child.getLocalName().equals("field") || child.getLocalName().equals("attribute")) {
//					Class wrapperClass = child.hasAttribute("type") ? Class.forName(child.getAttribute("type")) : be.nabu.libs.types.xsd.types.String.class;
//					// need an item reference to add to the parent
//					Item item = (Item) wrapperClass.newInstance();
//					// cast for remainder, you can only define fields in the definition
//					// the magical simple structure wrapping is later on and this is NOT a field
//					Field field = (Field) item;
//					setItemAttributes(child, item);
//					field.setAttribute(child.getLocalName().equals("attribute"));
//					for (int j = 0; j < child.getChildNodes().getLength(); j++) {
//						if (child.getChildNodes().item(j).getNodeType() == Node.ELEMENT_NODE) {
//							Element fieldChild = (Element) child.getChildNodes().item(j);
//							// you can define enumerations for a field
//							if (fieldChild.getLocalName().equals("enumeration")) {
//								if (!(item instanceof Unmarshallable))
//									throw new DefinitionException("The field '" + item.getPath() + "' contains enumerations but they can not be unmarshalled");
//								field.addEnumeration(((Unmarshallable) item).unmarshal(fieldChild.getTextContent()));
//							}
//							// ok, the field has attributes, it needs to be "upgraded" to a SimpleStructure
//							// and recursively parsed
//							else if (fieldChild.getLocalName().equals("attribute")) {
//								// generate a new simple structure
//								SimpleStructure simpleStructure = SimpleStructure.newInstance(field);
//								simpleStructure.setNode(node);
//								// assign it to the item, so the structure get's added to the parent, not the field
//								item = simpleStructure;
//								// start loading from the offset, enumerations are of no value
//								load(node, child, simpleStructure, j);
//							}
//						}
//					}
//					parent.add(item);
//				}
//				// restrictions on the referenced structure
//				else if (child.getLocalName().equals("restriction")) {
//					String name = child.getAttribute("name");
//					String reference = child.getAttribute("reference");
//					parent.setRestriction(name, reference);
//				}
//			}
//		}
//	}
//	
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	private void setItemAttributes(Element element, ModifiableType type) {
//		NamedNodeMap attr = element.getAttributes();
//		for (int i = 0; i < attr.getLength(); i++) {
//			String key = attr.item(i).getLocalName();
//			if (key.equals("type")) {
//				continue;
//			}
//			for (Property<?> supported : type.getSupportedProperties()) {
//				if (supported.getName().equals(key)) {
//					String value = attr.item(i).getNodeValue();
//					type.setProperty(new ValueImpl(supported, getConverter().convert(value, supported.getValueClass())));					
//				}
//			}
//		}
//	}
//	
//	public static void save(Locator locator, DefinedStructure structure) throws DefinitionException, DOMException, MarshalException {
//		Document document = XMLUtils.newDocument();
//		Element root = document.createElementNS(NAMESPACE, structure.isAspect() ? "aspect" : "structure");
//		build(root, structure);
//	}
//	
//	private static void build(Element element, Structure parent) throws DefinitionException, DOMException, MarshalException {
//		setElementAttributes(element, parent);
//		for (Item child : parent.getChildren()) {
//			if (child instanceof SimpleStructure) {
//				SimpleStructure<?> simpleStructure = (SimpleStructure<?>) child;
//				Element childElement = element.getOwnerDocument().createElementNS(NAMESPACE, "field");
//				// copy all the attributes to the field header
//				setElementAttributes(childElement, simpleStructure.getValueField());
//				// copy the enumerations
//				setElementEnumerations(childElement, simpleStructure.getValueField());
//				// set as attributes
//				// each item is definately a field as no structures are allowed as attributes
//				for (Item item : simpleStructure.getChildren()) {
//					Element attribute = element.getOwnerDocument().createElementNS(NAMESPACE, "attribute");
//					setElementAttributes(attribute, item);
//					setElementEnumerations(attribute, (Field<?>) item);
//				}
//			}
//			else if (child instanceof Structure) {
//				// create new structure child
//				Element childElement = element.getOwnerDocument().createElementNS(NAMESPACE, ((Structure) child).isAspect() ? "aspect" : "structure");
//				build(childElement, (Structure) child);
//			}
//			else {
//				// must be a field
//				Field<?> field = (Field<?>) child;
//				Element childElement = element.getOwnerDocument().createElementNS(NAMESPACE, field.isAttribute() ? "attribute" : "field");
//				setElementAttributes(childElement, field);
//				setElementEnumerations(childElement, field);
//			}
//		}
//	}
//	
//	private static void setElementAttributes(Element element, Item item) {
//		Map<String, String> properties = item.getProperties();
//		for (String key : properties.keySet())
//			element.setAttribute(key, properties.get(key));
//	}
//	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private static void setElementEnumerations(Element element, Field field) throws DefinitionException, DOMException, MarshalException {
//		if (field.getEnumerations().size() > 0) {
//			if (!(field instanceof Marshallable))
//				throw new DefinitionException("Could not marshal the enumerations for field " + field.getPath());
//			for (Object enumeration : field.getEnumerations()) {
//				Element child = element.getOwnerDocument().createElementNS(NAMESPACE, "enumeration");
//				child.setTextContent(((Marshallable) field).marshal(enumeration));
//				element.appendChild(child);
//			}
//		}
//	}
//
//	public SimpleTypeWrapper getWrapper() {
//		if (wrapper == null)
//			wrapper = SimpleTypeWrapperFactory.getInstance().getWrapper();
//		return wrapper;
//	}
//
//	public void setWrapper(SimpleTypeWrapper wrapper) {
//		this.wrapper = wrapper;
//	}
//	
//	public void unsetWrapper(SimpleTypeWrapper wrapper) {
//		this.wrapper = null;
//	}
//	
	
}
