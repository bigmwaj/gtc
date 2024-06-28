package com.agro360.bean2ts;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.agro360.model.TsComponentModel;
import com.agro360.model.TsComponentModel.TYPE;
import com.agro360.model.TsFieldModel;
import com.agro360.model.TsModuleModel;
import com.agro360.util.CommonUtils;

@Mojo(name = "gtc", requiresProject = true, 
	defaultPhase = LifecyclePhase.PROCESS_CLASSES, 
	requiresDependencyResolution = ResolutionScope.COMPILE, 
	threadSafe = true)
public class GenerateTypeScriptComponents extends AbstractMojo {	 
	
	@Parameter(required = true)
	private String packageSource;
	
	@Parameter(required = true)
	private String targetDir;
	
	@Parameter()
	private Map<String, String> javaTypeMap = new HashMap<>();
	
	@Parameter()
	private Map<String, String> fieldTypeMap = new HashMap<>();
	
	private final String[] CANDIDATE_COMPONENT_POSTFIX = {
			"Bean", 
			"EnumVd", 
			"Message", 
			"FieldMetadata"
	};
	
	@Override
	public void execute() throws MojoExecutionException {
		try {
			
			CommonUtils.initJavaTypeMap(javaTypeMap);
			
			getLog().info("Liste des types est " + javaTypeMap);
			
			//getLog().info(String.format("Deleting angular backed dir %s ...", targetDir));
			//deleteDir(new File(targetDir + "/backed"));
			//getLog().info(String.format("Deleting angular backed dir %s completed. [Succes!]", targetDir));
			
			getLog().info(String.format("Scanning candidate components from [%s]...", packageSource));
			var components = scanneClasses();
			getLog().info(String.format("Scanning candidate components from [%s] completed! Total scanned %d", packageSource, components.size()));
			
			getLog().info(String.format("Generating typescript components..."));
			var importSource = components.stream()
					.collect(Collectors.toMap(TsComponentModel::getJavaName, e -> e));
			getLog().info(String.format("Generating typescript components completed!"));
			
			getLog().info(String.format("Saving typescript components in angular project..."));
			components.stream().collect(Collectors.toMap(TsComponentModel::getNamespace, Collections::singleton, CommonUtils::merge))
					.entrySet()
					.stream()
					.map(e -> TsModuleModel.builder().namespace(e.getKey()).components(e.getValue()).build())
					.forEach(e -> saveTsFile(importSource, e));
			getLog().info(String.format("Saving typescript components in angular project completed!"));
			
		} catch (Exception e) {
			getLog().error(e);
		}
	}
	
	private boolean contentChanged(File file, String content) {	
		try(var st1 = new FileInputStream(file); var st2 = new ByteArrayInputStream(content.getBytes())){
			return !IOUtils.contentEquals(st1, st2);
		}catch (Exception e) {
			getLog().error(e);
		}

		return false ;
	}
	
	protected void saveTsFile(Map<String, TsComponentModel> importSource, TsModuleModel module) {
		var backedDir = new File(targetDir + "/backed/");
		if( !backedDir.exists()) {
			backedDir.mkdirs();
		}
		var file = new File(targetDir + "/backed/" + module.getNamespace() + ".ts");
		var code = module.getSourceCode(importSource);
		if( !file.exists() || contentChanged(file, code) ) {
			getLog().info("Génération du fichier " + file.getAbsolutePath());
			try(var out = new BufferedWriter(new FileWriter(file, false))) {
				file.createNewFile();
				out.write(code);				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private Set<TsComponentModel> scanneClasses(){
		Predicate<Class<?>> filter;
		filter = (s) -> Arrays.stream(CANDIDATE_COMPONENT_POSTFIX)
				.anyMatch(s.getName()::endsWith);
		
		Predicate<Class<?>> isNotInterface = e -> !e.isInterface();
		
		return Arrays.asList(
				new Reflections(packageSource, new SubTypesScanner(false)).getSubTypesOf(Enum.class),
				new Reflections(packageSource, new SubTypesScanner(false)).getSubTypesOf(Object.class)
			)
			.stream()
			.flatMap(Set::stream)
			.filter(filter)
			.filter(isNotInterface)
			.map(this::mapToTsComponentModel)
			.collect(Collectors.toSet());
	}
	
	private TYPE getTsType(Class<?> klass) {
		return klass.isEnum() ? TYPE.TYPE : TYPE.INTERFACE;
	}
	
	private String getTsNamespace(Class<?> klass) {
		var ns = klass.getName().replace(packageSource, "")
				.replace(klass.getSimpleName(), "")
				.replace(".bo", "");
		
		if( ns.startsWith(".")) {
			ns = ns.substring(1);
		}
		
		if( ns.endsWith(".")) {
			ns = ns.substring(0, ns.length()-1);
		}
		return ns;
	}
	
	private boolean isNotStaticField(Field field) {
		return !Modifier.isStatic(field.getModifiers());
	}
	
	private Set<TsFieldModel> getTsFields(Class<?> klass){
		if( klass.isEnum() ) {
			return Arrays.stream(klass.getEnumConstants())
				.map(e -> TsFieldModel.builder().name(e.toString()).build())
				.collect(Collectors.toSet());
		}
		return Arrays.stream(klass.getDeclaredFields())
				.filter(this::isNotStaticField)
				.map(this::mapToTsFieldModel)
				.collect(Collectors.toSet());
	}
	
	private String getExtend(Class<?> klass) {
		try {
			if( klass.isInterface() ) {
				return null;
			}
			var ex = klass.getSuperclass().getSimpleName();
			if (!"Object".equals(ex)) {
				return ex;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(String.format("L'extension de la class %s contient des erreurs", klass), e);
		}
	}
	
	private String getExtendJavaName(Class<?> klass) {
		if( klass.isInterface() ) {
			return null;
		}
		var ex = klass.getSuperclass().getName();
		if (!Object.class.getName().equals(ex)) {
			return ex;
		}
		
		return null;
	}
	
	private String getExtendParameterJavaName(Class<?> klass) {
		try {
			var ex = klass.getSuperclass();
			var params = ex.getTypeParameters();
			if( params.length != 0 ) {
				var superClass = klass.getGenericSuperclass().getTypeName();
				superClass = superClass.substring(1 + superClass.indexOf("<"), superClass.lastIndexOf(">"));
				return superClass;
			}
			return null;
		} catch (Exception e) {
			getLog().error(e);
			throw new RuntimeException("Impossible d'avoir le nom de la class java du paramètre de la classe " + klass, e);
		}
	}
	
	private String getExtendParameterName(Class<?> klass) {		
		try {
			var ex = getExtendParameterJavaName(klass);
			if( ex != null && ex.contains(".") ) {
				return ex.substring(ex.lastIndexOf(".") + 1);
			}
			return null;
		} catch (Exception e) {
			getLog().error(e);
			throw new RuntimeException("Impossible d'avoir le nom du paramètre de la classe " + klass, e);
		}
	}
	
	private boolean isGeneric(Class<?> klass) {
		return klass.getTypeParameters().length > 0;
	}
	
	private String getGenericType(Class<?> klass) {
		if( isGeneric(klass)) {
			return klass.getTypeParameters()[0].getName();
		}
		return null;
	}
	
	private TsComponentModel mapToTsComponentModel(Class<?> klass) {
		return TsComponentModel.builder()
				.name(klass.getSimpleName())
				.javaName(klass.getName())
				.namespace(getTsNamespace(klass))
				.type(getTsType(klass))
				.fields(getTsFields(klass))
				.extend(getExtend(klass))
				.extendJavaName(getExtendJavaName(klass))
				.isGeneric(isGeneric(klass))
				.genericType(getGenericType(klass))
				.extendParameterJavaClassName(getExtendParameterJavaName(klass))
				.extendParameterName(getExtendParameterName(klass))
				.build();
	}
	
	private boolean isGeneric(Field field) {
		return field.toGenericString().indexOf("<") >= 0;
	}

	private String getGenericType(Field field) {
		if( !field.getType().equals(Map.class) && isGeneric(field)) {
			var fieldType = field.toGenericString();
			fieldType = fieldType.substring(1 + fieldType.indexOf("<"), fieldType.lastIndexOf(">"));
			fieldType = fieldType.substring(1 + fieldType.lastIndexOf("."));
			
			if( javaTypeMap.containsKey(fieldType) ) {
				return javaTypeMap.get(fieldType);
			}
			return CommonUtils.getTsType(CANDIDATE_COMPONENT_POSTFIX, fieldType);
		}
		return null;
	}

	private String getGenericTypeImportJavaType(Field field) {
		if( isGeneric(field)) {	
			var fieldType = field.toGenericString();
			fieldType = fieldType.substring(1 + fieldType.indexOf("<"), fieldType.lastIndexOf(">"));
			if( Arrays.stream(CANDIDATE_COMPONENT_POSTFIX).anyMatch(fieldType::endsWith)) {
				return fieldType;
			}else {
				var param = Arrays.stream(field.getDeclaringClass().getTypeParameters())
				.map(Type::getTypeName).anyMatch(fieldType::equals);
				
				if( param ) {
					return fieldType;
				}
			}
		}
		return null;
	}

	private String getType(Field field) {
		var fieldFullName = field.getDeclaringClass().getName() + "." + field.getName();
		if( fieldTypeMap.containsKey(fieldFullName) ) {
			return fieldTypeMap.get(fieldFullName);
		}
		
		var fieldType = field.getType().getSimpleName();
		if( javaTypeMap.containsKey(fieldType) ) {
			return javaTypeMap.get(fieldType);
		}
		
		return CommonUtils.getTsType(CANDIDATE_COMPONENT_POSTFIX, field.getType().getSimpleName());
	}
	
	private boolean isEnumValue(Field field) {
		return field.getDeclaringClass().isEnum();
	}
	
	private boolean isImportable(Field field) {
		return Arrays.stream(CANDIDATE_COMPONENT_POSTFIX).anyMatch(field.getType().getName()::endsWith);
	}
	
	private String getImportJavaType(Field field) {
		if( isImportable(field)) {
			return field.getType().getName();
		}else {
			return null;
		}
	}
	
	private TsFieldModel mapToTsFieldModel(Field field) {
		return TsFieldModel.builder()
				.name(field.getName())
				.type(getType(field))
				.isGeneric(isGeneric(field))
				.genericType(getGenericType(field))
				.genericTypeImportJavaType(getGenericTypeImportJavaType(field))
				.isEnumValue(isEnumValue(field))
				.importJavaType(getImportJavaType(field))
				.build();
	}
	
}
