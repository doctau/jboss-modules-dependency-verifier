package com.sunsetutopia.jbossmoduleverifier;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class FieldReferenceVisitor extends FieldVisitor {
	private final Set<String> references;

	public FieldReferenceVisitor(Set<String> references, FieldVisitor v) {
		super(Opcodes.ASM4, v);
		this.references = references;
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		Utils.addTypeDescriptor(references, desc);
		return new AnnotationReferenceVisitor(references, super.visitAnnotation(desc, visible));
	}
}