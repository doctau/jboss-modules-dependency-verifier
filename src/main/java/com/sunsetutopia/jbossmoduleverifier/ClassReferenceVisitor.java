package com.sunsetutopia.jbossmoduleverifier;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ClassReferenceVisitor extends ClassVisitor {
	private final Set<String> references;
	private String className;
	
	public ClassReferenceVisitor() {
		super(Opcodes.ASM4);
		this.references = new HashSet<String>();
	}

	public Set<String> getReferences() {
		return references;
	}

	public String getClassName() {
		return className;
	}

	public void visit(int version, int access, String name,
			String signature, String superName, String[] interfaces) {
		className = name;

		Utils.addSlashedType(references, superName);
		for (String i: interfaces)
			Utils.addSlashedType(references, i);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		Utils.addTypeDescriptor(references, desc);
		return new AnnotationReferenceVisitor(references, super.visitAnnotation(desc, visible));
	}

	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		Utils.addTypeDescriptor(references, desc);
		return new FieldReferenceVisitor(references, super.visitField(access, name, signature, desc, value));
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		Utils.addTypeDescriptor(references, desc);
		if (exceptions != null)
			for (String e: exceptions)
				Utils.addSlashedType(references, e);
		return new MethodReferenceVisitor(references, super.visitMethod(access, name, desc, signature, exceptions));
	}
}