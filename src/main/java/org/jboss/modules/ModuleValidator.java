package org.jboss.modules;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;

import com.sunsetutopia.jbossmoduleverifier.ClassReferenceVisitor;

public class ModuleValidator {
	public static void main(String[] args) throws Exception {
		if (args.length == 1)
			new ModuleValidator().run(args[0], System.out);
		else if (args.length == 2)
			new ModuleValidator().run(args[0], new PrintStream(new FileOutputStream(args[1])));
	}
	
	private final Method GET_PACKAGES;
	private final Field JAR_FILE;
	public ModuleValidator() throws Exception {
		GET_PACKAGES = ClassLoader.class.getDeclaredMethod("getPackages");
		GET_PACKAGES.setAccessible(true);

		JAR_FILE = JarFileResourceLoader.class.getDeclaredField("jarFile");
		JAR_FILE.setAccessible(true);
	}

	private void run(final String modulePath, final PrintStream out) throws IOException {
        System.setProperty("module.path", modulePath);
        final ModuleLoader loader = new LocalModuleLoader();
        
        final Path moduleRoot = Paths.get(modulePath);
        Files.walkFileTree(moduleRoot, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            	if (file.endsWith("module.xml")) {
            		Path moduleDir = file.getParent();
            		String slot = moduleDir.getFileName().toString();
            		Iterator<Path> it = moduleRoot.relativize(moduleDir.getParent()).iterator();
            		StringBuilder moduleName = new StringBuilder();
            		while (it.hasNext()) {
            			moduleName.append(it.next().toString());
            			if (it.hasNext())
            				moduleName.append('.');
            		}
            		verifyModule(loader, moduleName.toString(), slot, out);
            	}
            	return FileVisitResult.CONTINUE;
            }
        });
	}

	private void verifyModule(ModuleLoader loader, String moduleName, String slot, final PrintStream out) throws IOException {
		try {
			final ModuleIdentifier moduleId = ModuleIdentifier.create(moduleName, slot);
			final Module module = loader.loadModule(moduleId);
			final ModuleClassLoader classLoader = module.getClassLoader();

	        ModuleSpec moduleSpec = loader.findModule(moduleId);
	        if (moduleSpec instanceof ConcreteModuleSpec) {
	        	final ConcreteModuleSpec concreteSpec = (ConcreteModuleSpec)moduleSpec;
	        	
	        	Set<String> classFiles = new HashSet<String>();
	        	for (ResourceLoaderSpec rls: concreteSpec.getResourceLoaders()) {
	        		classFiles.addAll(getResourceLoaderClasses(rls.getResourceLoader(), moduleId));
	        	}

				for (String c: classFiles) {
					InputStream is = null;
					try {
						is = classLoader.getResourceAsStream(c);
						ClassReader cr = new ClassReader(is);
						ClassReferenceVisitor crv = new ClassReferenceVisitor();
						cr.accept(crv, ClassReader.SKIP_FRAMES);
						verifyReferences(module, crv.getClassName(), crv.getReferences(), out);
					} finally {
						if (is != null)
							is.close();
					}
				}
	        } else if (moduleSpec instanceof AliasModuleSpec) {
	        	//skip
	        } else {
	        	System.err.println("unknown module spec type " + moduleSpec.getClass().getName() + " for " + moduleId);
	        }
		} catch (ModuleLoadException e) {
			System.err.println("could not load module " + moduleName + ":" + slot);
			e.printStackTrace(System.err);
		}
	}

	// returns a list of files which are for classes
	private Set<String> getResourceLoaderClasses(ResourceLoader resourceLoader, ModuleIdentifier moduleId) {
		final Set<String> classes = new HashSet<String>();
		
		if (resourceLoader instanceof JarFileResourceLoader) {
			JarFileResourceLoader jarLoader = (JarFileResourceLoader)resourceLoader;
			try {
				JarFile jarFile = (JarFile) JAR_FILE.get(jarLoader);
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith(".class")) {
						classes.add(entry.getName());
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace(System.err);
			}
			
		} else if (resourceLoader instanceof NativeLibraryResourceLoader) {
			NativeLibraryResourceLoader nativeLoader = (NativeLibraryResourceLoader)resourceLoader;

			try {
				Path path = nativeLoader.getRoot().toPath();
				if (Files.exists(path)) {
			        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			            	if (file.getFileName().toString().endsWith(".class")) {
			            		classes.add(file.toString());
			            	}
			            	return FileVisitResult.CONTINUE;
			            }
			        });
				} else {
					System.err.println(path + " does not exist");
				}
			} catch (IOException e) {
				System.err.println("failed to walk " + nativeLoader.getRoot() + " for " + moduleId);
				e.printStackTrace(System.err);
			}
			
		} else if (resourceLoader instanceof FilteredResourceLoader) {
			FilteredResourceLoader filteredLoader = (FilteredResourceLoader)resourceLoader;
			//FIXME
		} else {
        	System.err.println("unknown resource loader type " + resourceLoader.getClass().getName() + " for " + moduleId);
		}
		return classes;
	}

	private void verifyReferences(Module module, String klass, Set<String> references, PrintStream out) {
		final ModuleClassLoader classLoader = module.getClassLoader();
		final ModuleIdentifier id = module.getIdentifier();
		for (String className: references) {
			try {
				classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				out.println(id + "," + klass + "," + className);
			}
		}
	}
}
