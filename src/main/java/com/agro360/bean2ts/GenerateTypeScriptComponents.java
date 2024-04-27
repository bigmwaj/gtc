package com.agro360.bean2ts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.agro360.bo.bean.common.AbstractLigneBean;
import com.agro360.bo.utils.TypeScriptInfos;
import com.agro360.model.TsComponentModel;
import com.agro360.model.TsComponentModel.TYPE;
import com.agro360.model.TsFieldModel;
import com.agro360.model.TsModuleModel;
import com.agro360.util.CommonUtils;

@Mojo(name = "gtc")
public class GenerateTypeScriptComponents extends AbstractMojo {

	private static final String SOURCE_ROOT_PACKAGE = "com.agro360";
	
	private static final String TS_PROJECT_APP_DIR = "C:\\WorkSpace\\0-projects\\0-business\\agro360v2\\agro360-web-client\\src\\app\\";

	private final String[] CANDIDATE_COMPONENT_POSTFIX = {
			"Bean", 
			"EnumVd", 
			"Message", 
			"FieldMetadata"
	};

	public void execute() throws MojoExecutionException {
		try {
			getLog().info(String.format("Deleting angular backed dir %s ...", TS_PROJECT_APP_DIR));
			deleteDir(new File(TS_PROJECT_APP_DIR + "/backed"));
			getLog().info(String.format("Deleting angular backed dir %s completed. [Succes!]", TS_PROJECT_APP_DIR));
			
			getLog().info(String.format("Scanning candidate components from [%s]...", SOURCE_ROOT_PACKAGE));
			var components = scanneClasses();
			getLog().info(String.format("Scanning candidate components from [%s] completed! Total scanned %d", SOURCE_ROOT_PACKAGE, components.size()));
			
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
	
	private void deleteDir(File file) {
		if( file.exists() ) {
			if( file.isDirectory() ) {
				String[] children = file.list();
				if( children.length == 0 ) {
					file.delete();
				}else {
					for (String fileName : children) {
						deleteDir(new File(file.getAbsoluteFile() + "/" + fileName));
					}
				}
			}else {
				file.delete();
			}
		}
	}
	
	// TODO Modifier un fichier que si le contenu a changé
	private boolean contentChanged(File file, String content) {		
		return true;
	}
	
	protected void saveTsFile(Map<String, TsComponentModel> importSource, TsModuleModel module) {
		var backedDir = new File(TS_PROJECT_APP_DIR + "/backed/");
		if( !backedDir.exists()) {
			backedDir.mkdirs();
		}
		var file = new File(TS_PROJECT_APP_DIR + "/backed/" + module.getNamespace() + ".ts");
		try(var out = new BufferedWriter(new FileWriter(file, false))) {
			var code = module.getCode(importSource);
			if( contentChanged(file, code) ) {
				file.createNewFile();
				out.write(code);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Set<TsComponentModel> scanneClasses(){
		Predicate<Class<?>> filter;
		filter = (s) -> Arrays.stream(CANDIDATE_COMPONENT_POSTFIX)
				.anyMatch(s.getName()::endsWith);
		
		Predicate<Class<?>> isNotInterface = e -> !e.isInterface();
		
		return Arrays.asList(
				new Reflections(SOURCE_ROOT_PACKAGE, new SubTypesScanner(false)).getSubTypesOf(Enum.class),
				new Reflections(SOURCE_ROOT_PACKAGE, new SubTypesScanner(false)).getSubTypesOf(Object.class)
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
		var ns = klass.getName().replace(SOURCE_ROOT_PACKAGE, "")
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
			if( klass.isAnnotationPresent(TypeScriptInfos.class) && klass.getAnnotation(TypeScriptInfos.class).igroreSuperClassParam()) {
				return null;
			}
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
		return !AbstractLigneBean.class.getName().equals(klass.getName()) 
				&& klass.getTypeParameters().length > 0;
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
			return CommonUtils.getTsType(CANDIDATE_COMPONENT_POSTFIX, fieldType.substring(1 + fieldType.lastIndexOf(".")));
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
		if( field.isAnnotationPresent(TypeScriptInfos.class) ) {
			return field.getAnnotation(TypeScriptInfos.class).type();
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
