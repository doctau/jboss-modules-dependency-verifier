package com.sunsetutopia.jbossmoduleverifier;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodReferenceVisitor extends MethodVisitor {
	private final Set<String> references;

	public MethodReferenceVisitor(Set<String> references, MethodVisitor v) {
		super(Opcodes.ASM4, v);
		this.references = references;
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		Utils.addTypeDescriptor(references, desc);
		return new AnnotationReferenceVisitor(references, super.visitAnnotation(desc, visible));
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		Utils.addTypeDescriptor(references, desc);
		return new AnnotationReferenceVisitor(references, super.visitAnnotation(desc, visible));
	}

	public void visitTypeInsn(int opcode, String type) {
		Utils.addSlashedType(references, type);
		super.visitTypeInsn(opcode, type);
	}

	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		Utils.addSlashedType(references, owner);
		Utils.addTypeDescriptor(references, desc);
		super.visitMethodInsn(opcode, owner, name, desc);
	}

	public void visitMultiANewArrayInsn(String desc, int dims) {
		Utils.addTypeDescriptor(references, desc);
		super.visitMultiANewArrayInsn(desc, dims);
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		if (type != null) // null for finally blocks
			Utils.addSlashedType(references, type);
		super.visitTryCatchBlock(start, end, handler, type);
	}
}