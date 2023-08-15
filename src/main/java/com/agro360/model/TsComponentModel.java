package com.agro360.model;

import java.util.Set;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class TsComponentModel {

	public enum TYPE {
		INTERFACE,

		TYPE
	}

	private TYPE type;

	private String name;

	@EqualsAndHashCode.Include()
	private String javaName;

	private String namespace;

	private String extend;
	
	private String extendJavaName;

	private Set<TsFieldModel> fields;
	
	private boolean isGeneric;
	
	private String genericType;

	public String getComponentCodeWithoutImport() {		
		if( TYPE.TYPE.equals(type)) {
			var typeFormat = "export type %s = %s;";
			var values = fields.stream().map(TsFieldModel::getName).collect(Collectors.joining(" | "));
			return String.format(typeFormat, name, values);
		}else {
			var finalName = name;
			if( isGeneric && genericType != null ) {
				finalName += "<" + genericType + ">";
			}
			if( extend != null ) {
				finalName += " extends " + extend;
			}
			var sourceCode = fields.stream().map(TsFieldModel::getFieldCode).collect(Collectors.joining("\n"));
			var interfaceFormat = "\nexport interface %s {\n%s\n};";
			return String.format(interfaceFormat, finalName, sourceCode);
		}
	}

	@Override
	public String toString() {
		return "TsComponentModel [name=" + name + ", extendJavaName=" + extendJavaName + "]";
	}


}
