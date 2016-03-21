package com.tsc9526.monalisa.core.generator;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.tsc9526.monalisa.core.annotation.DB;
import com.tsc9526.monalisa.core.datasource.ConfigClass;
import com.tsc9526.monalisa.core.datasource.DataSourceManager;
import com.tsc9526.monalisa.core.meta.MetaTable;
import com.tsc9526.monalisa.core.meta.MetaTable.CreateTable;
import com.tsc9526.monalisa.core.query.model.Model;
import com.tsc9526.monalisa.core.tools.JavaWriter;

/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
public class DBGeneratorProcessing extends DBGenerator{	 
	private ProcessingEnvironment processingEnv;	 
	private TypeElement typeElement;
	 
	public DBGeneratorProcessing(ProcessingEnvironment processingEnv,TypeElement typeElement) {
		super();
		 
		this.processingEnv = processingEnv;		 
		this.typeElement = typeElement;
		
		DB db=typeElement.getAnnotation(DB.class);
		if(db==null){
			throw new RuntimeException("TypeElement without @DB: "+typeElement.toString());
		}		
		String dbKey=db.key();
		if(dbKey==null || dbKey.length()<1){
			dbKey=typeElement.toString();
		}
		String projectPath=DBProject.getProject(processingEnv, typeElement).getProjectPath();
		System.setProperty("DB@"+dbKey,projectPath);				 
		
		this.dbcfg=DataSourceManager.getInstance().getDBConfig(dbKey,db,true);
				
		String name=typeElement.getQualifiedName().toString();
		String pkg=name.toLowerCase();
		int p=name.lastIndexOf(".");
		if(p>0){
			pkg=name.substring(0,p)+name.substring(p).toLowerCase();
		}		
		this.javaPackage=pkg;		
		this.resourcePackage="resources."+pkg;
		this.dbi=typeElement.getQualifiedName().toString();
		this.dbmetadata=new DBMetadata(projectPath,javaPackage,dbcfg);		
	}	 
	  
	protected Writer getResourceWriter(){		
		try{			 					
			FileObject res = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, resourcePackage, CreateTable.FILE_NAME, typeElement);
			OutputStream out=res.openOutputStream();
			
			Writer w = new OutputStreamWriter(out,"UTF-8");
			return w;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	protected Writer getJavaWriter(MetaTable table){		
		try{			 
			String className=table.getJavaName();
			JavaFileObject java = processingEnv.getFiler().createSourceFile(javaPackage+"."+className, typeElement);
			Writer os = java.openWriter();			
			return new JavaWriter(os);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	
	protected String getModelClassValue(MetaTable table){
		String modelClass=null;
		try{
			modelClass=super.getModelClassValue(table);
			
		}catch(MirroredTypeException mte ){
			TypeMirror typeMirror=mte.getTypeMirror();
			Types TypeUtils = processingEnv.getTypeUtils();
			TypeElement te= (TypeElement)TypeUtils.asElement(typeMirror);
			modelClass=te.getQualifiedName().toString();
		}
		
		if(modelClass==null || modelClass.trim().length()==0){
			modelClass=Model.class.getName(); 
		}
		
		return modelClass;
	}
	
	@SuppressWarnings("unchecked")
	public static Class<? extends ConfigClass> getDBConfigClass(DB db){
		Class<? extends ConfigClass> clazz=null;
		try{
			clazz=db.configClass();
		}catch(MirroredTypeException mte){
			MirroredTypeException x=(MirroredTypeException)mte;
			DeclaredType classTypeMirror = (DeclaredType) x.getTypeMirror();
			TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
			
			String className=classTypeElement.getQualifiedName().toString();
			try{
				clazz=(Class<? extends ConfigClass>)Class.forName(className);
			}catch(ClassNotFoundException e){
				logger.info("Class not found try load class: "+className+" from project path.");
				
				return loadClassFromProject(className);
				
			}
		}
		
		return clazz;
	}
	
	private static Class<? extends ConfigClass> loadClassFromProject(String className){
		throw new RuntimeException("Class not found: "+className);
	}
}
