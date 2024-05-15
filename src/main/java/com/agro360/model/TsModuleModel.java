package com.agro360.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.agro360.util.CommonUtils;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class TsModuleModel {

	@EqualsAndHashCode.Include()
	private String namespace;

	private Set<TsComponentModel> components;

	private String getModuleCodeWithoutImport() {
		return components.stream()
		.map(TsComponentModel::getComponentCodeWithoutImport)
		.collect(Collectors.joining("\n"));
	}

	private String getModuleImportCode(Map<String, TsComponentModel> importSource) {
		
		var imports = Arrays.asList(
			components.stream().map(TsComponentModel::getExtendJavaName).filter(Objects::nonNull),
			components.stream().map(TsComponentModel::getFields).flatMap(Set::stream)
				.map(TsFieldModel::getImportJavaType).filter(Objects::nonNull),
			components.stream().map(TsComponentModel::getFields).flatMap(Set::stream)
				.map(TsFieldModel::getGenericTypeImportJavaType).filter(Objects::nonNull)
			)
			.stream()
			.flatMap(e -> e)
			.filter(e -> importSource.containsKey(e) && !namespace.equals(importSource.get(e).getNamespace()))
			.collect(Collectors.toSet());
		
		return importSource.entrySet().stream()
			.filter(e -> imports.contains(e.getKey()))
			.map(Entry::getValue)
			.collect(Collectors.toMap(TsComponentModel::getNamespace, e -> Collections.singleton(e.getName()), CommonUtils::merge))
			.entrySet().stream().map(this::generateImport)
			.collect(Collectors.joining("\n"));
	}
	
	public String getSourceCode(Map<String, TsComponentModel> importSource) {
		return String.format("%s\n%s", getModuleImportCode(importSource), getModuleCodeWithoutImport());
	}
	
	private String generateImport(Entry<String, Set<String>> entry) {
		return String.format("import %s from './%s';", 
				entry.getValue().stream().sorted().collect(Collectors.joining(", ", "{ ", " }")),
				entry.getKey()
		);
	}
	

}
