package com.tsc9526.monalisa.core.parser.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tsc9526.monalisa.core.parser.jsp.JspCode;
import com.tsc9526.monalisa.core.parser.jsp.JspElement;
import com.tsc9526.monalisa.core.parser.jsp.JspEval;
import com.tsc9526.monalisa.core.parser.jsp.JspText;
import com.tsc9526.monalisa.core.tools.JavaWriter;

public class QueryStatement {
	private QueryPackage queryPackage;
	
	private String comments;
	private String id;
	private String db;
	
	
	private List<JspElement> elements=new ArrayList<JspElement>();
	 
	private final static String REGX_VAR="\\$[a-zA-Z_]+[a-zA-Z_0-9]*";
	
	private Pattern pattern = Pattern.compile(REGX_VAR);
	
	public void write(JavaWriter writer){
		writer.append("public Query ").append(id).append("(Query q){\r\n");
		writeUseDb(writer);
		writeElements(writer);
		writer.append("}");
	}
	
	public void add(JspElement e){
		elements.add(e);
	}
	
	protected void writeElements(JavaWriter writer){
		boolean append=false;
		
		for(JspElement e:elements){
			if(e instanceof JspText){
				String code=e.getCode();
				System.err.println("!!!"+code);
				
				String[] lines=code.split("\n");
				for(int i=0;i<lines.length;i++){
					String line=lines[i];
					
					if(line.trim().length()==0 && !append){
						continue;
					}
					
					append=true;
					List<String> vars=new ArrayList<String>();
					Matcher m=pattern.matcher(line);
					while(m.find()){
						String var=m.group();
						vars.add(var.substring(1));
					}
					
					String s=line.replaceAll(REGX_VAR, "?");
					writer.append("q.add(\"").append(s).append("\"");
					if(vars.size()>0){
						for(String v:vars){
							writer.append(","+v);
						}
					}
					writer.append(");\r\n");
				}
			}else if(e instanceof JspEval){
				String s=e.getCode();
				if(s.startsWith("\"")){
					writer.append("q.add("+s+");\r\n");
				}else{
					writer.append("q.add(\"?\",").append(s).append(");\r\n");
				}
			}else if(e instanceof JspCode){
				writer.append(e.getCode());
			}
		}
	}
	
	private void writeUseDb(JavaWriter writer){
		String db=this.db;
		if(db==null || db.length()<1){
			db=queryPackage.getDb();
		}
		 
		if(db!=null && db.length()>0){
			if(db.endsWith(".class")){
				writer.append("q.use(DBConfig.fromClass("+db+"));\r\n");
			}else if(db.endsWith(".DB")){
				writer.append("q.use("+db+");\r\n");
			}else{
				int x=db.indexOf(".class.getName");
				if(x>0){
					writer.append("q.use(DBConfig.fromClass("+db.substring(0,x+6)+"));\r\n");
				}else{
					writer.append("q.use(DBConfig.fromClass("+db+".class));\r\n");
				}
			}
		}
	}

	public QueryPackage getQueryPackage() {
		return queryPackage;
	}

	public void setQueryPackage(QueryPackage queryPackage) {
		this.queryPackage = queryPackage;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		if(db!=null)db=db.trim();
		
		this.db = db;
	}
	 
}
