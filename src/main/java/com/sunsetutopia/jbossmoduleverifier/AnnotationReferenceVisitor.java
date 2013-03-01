package com.sunsetutopia.jbossmoduleverifier;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class AnnotationReferenceVisitor extends AnnotationVisitor {
	private final Set<String> references;

	public AnnotationReferenceVisitor(Set<String> references, AnnotationVisitor v) {
		super(Opcodes.ASM4, v);
		this.references = references;
	}

	public void visitEnum(String name, String desc, String value) {
		Utils.addTypeDescriptor(references, desc);
		super.visitEnum(name, desc, value);
	}

	public AnnotationVisitor visitAnnotation(String name, String desc) {
		Utils.addTypeDescriptor(references, desc);
		return new AnnotationReferenceVisitor(references, super.visitAnnotation(name, desc));
	}

	public AnnotationVisitor visitArray(String name) {
		return new AnnotationReferenceVisitor(references, super.visitArray(name));
	}
}