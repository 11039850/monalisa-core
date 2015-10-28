package com.tsc9526.monalisa.core.generator;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.tsc9526.monalisa.core.datasource.DBConfig;
import com.tsc9526.monalisa.core.datasource.DataSourceManager;
import com.tsc9526.monalisa.core.meta.MetaPartition;
import com.tsc9526.monalisa.core.meta.MetaTable;
import com.tsc9526.monalisa.core.query.model.Model;
import com.tsc9526.monalisa.core.query.partition.Partition;
import com.tsc9526.monalisa.core.tools.JavaWriter;

/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
public class DBGenerator {
	public static String PROJECT_TMP_PATH="/target/monalisa";
	
	private ProcessingEnvironment processingEnv;
	 
	private TypeElement typeElement;
	
	private DBConfig dbcfg;
	
	private DBMetadata dbmetadata; 
	
	private String javaPackage;
	
	public DBGenerator(ProcessingEnvironment processingEnv,TypeElement typeElement) {
		super();
		this.processingEnv = processingEnv;		 
		this.typeElement = typeElement;
		
		String projectPath=DBProject.getProject(processingEnv, typeElement).getProjectPath();
		String dbKey=this.typeElement.toString();
		System.setProperty("DB@"+dbKey,projectPath);				 
		
		this.dbcfg=DataSourceManager.getInstance().getDBConfig(typeElement,true);
				
		String name=typeElement.getQualifiedName().toString();
		String pkg=name.toLowerCase();
		int p=name.lastIndexOf(".");
		if(p>0){
			pkg=name.substring(0,p)+name.substring(p).toLowerCase();
		}		
		this.javaPackage=pkg;
		
		this.dbmetadata=new DBMetadata(projectPath,javaPackage,dbcfg);		
	}	 
	
	
	
	public void generatorJavaFiles(){					
		List<MetaTable> tables=dbmetadata.getTables();
		for(MetaTable table:tables){
			generatorJavaFile(table);		 
		}	
		
		generatorResources(tables);
	}
	
	protected void generatorResources(List<MetaTable> tables){		
		try{			 					
			FileObject res = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "resources."+dbcfg.key().toLowerCase(), "create_table.sql", typeElement);
			OutputStream out=res.openOutputStream();
			
			Writer w = new OutputStreamWriter(out,"UTF-8");
			for(MetaTable table:tables){
				if(table.getCreateTable()!=null){
					w.write("/***CREATE TABLE: "+table.getNamePrefix()+" :: "+table.getName()+"***/\r\n");
					w.write(table.getCreateTable().getCreateSQL()); 
					w.write("\r\n\r\n\r\n");
				}
			}
			w.close();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	protected void generatorJavaFile(MetaTable table){		
		try{			 
			MetaTable clone=table.clone();
			clone.setJavaName(null).setName(clone.getNamePrefix());
			
			String className=clone.getJavaName();						
			JavaFileObject java = processingEnv.getFiler().createSourceFile(javaPackage+"."+className, typeElement);
			
			Writer os = java.openWriter();			
			
			String modelClass=getModelClassValue(clone);
			   
			JavaWriter writer=new JavaWriter(os);
			DBTableGeneratorByTpl g2=new DBTableGeneratorByTpl(clone, modelClass, typeElement.getQualifiedName().toString());
			g2.generate(writer);
			
			verifyPartition(table);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	private void verifyPartition(MetaTable table){
		MetaPartition mp=table.getPartition();
		if(mp!=null){			 
			try{ 
				Partition p=mp.getPartition();				 
				String error=p.verify(mp,table);
				if(error!=null){
					processingEnv.getMessager().printMessage(Kind.ERROR,"Partition error: "+error, typeElement);
				}						 				
			}catch(Exception e) {
				if(e instanceof RuntimeException){
					RuntimeException re=(RuntimeException)e;
					if(re.getCause()!=null && re.getCause() instanceof ClassNotFoundException){
						//Ingore this exception
						return;
					} 
				}
				e.printStackTrace(System.out);
				processingEnv.getMessager().printMessage(Kind.ERROR,e.getClass().getName()+":\r\n"+e.getMessage(), typeElement);
			}
		}
	}
	 
	
	
	protected String getModelClassValue(MetaTable table){
		String modelClass=null;
		try{
			modelClass=dbcfg.getProperty("modelClass."+table.getName(), dbcfg.modelClass());
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
	 
  
}