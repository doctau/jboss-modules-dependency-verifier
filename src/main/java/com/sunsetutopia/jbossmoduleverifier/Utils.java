package com.sunsetutopia.jbossmoduleverifier;

import java.util.Set;

import org.objectweb.asm.Type;


public class Utils {
	public static void addSlashedType(Set<String> references, String c) {
		if (references.contains("/"))
			throw new IllegalArgumentException(c + " contains slash");
		references.add(c.replace('/', '.'));
	}

	public static void addTypeDescriptor(Set<String> references, String desc) {
		Type type = Type.getType(desc);
		addType(references, type);
	}

	private static void addType(Set<String> references, Type type) {
		switch (type.getSort()) {
		case Type.OBJECT:
			addSlashedType(references, type.getClassName());
			break;
			
		case Type.ARRAY:
			addType(references, type.getElementType());
			break;
			
		case Type.METHOD:
			addType(references, type.getReturnType());
			for (Type t: type.getArgumentTypes())
				addType(references, t);
			break;
			
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.DOUBLE:
		case Type.FLOAT:
		case Type.INT:
		case Type.LONG:
		case Type.SHORT:
		case Type.VOID:
			break;
		default:
			throw new RuntimeException("unknown type sort " + type.getSort() + " for " + type.getDescriptor());
		}
		
	}
}
