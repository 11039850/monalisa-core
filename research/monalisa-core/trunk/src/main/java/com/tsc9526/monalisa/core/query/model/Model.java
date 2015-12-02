package com.tsc9526.monalisa.core.query.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.tsc9526.monalisa.core.annotation.Column;
import com.tsc9526.monalisa.core.annotation.Table;
import com.tsc9526.monalisa.core.datasource.DBConfig;
import com.tsc9526.monalisa.core.datasource.DataSourceManager;
import com.tsc9526.monalisa.core.datasource.DbProp;
import com.tsc9526.monalisa.core.query.Query;
import com.tsc9526.monalisa.core.query.dao.Delete;
import com.tsc9526.monalisa.core.query.dao.Insert;
import com.tsc9526.monalisa.core.query.dao.Update;
import com.tsc9526.monalisa.core.query.dialect.Dialect;
import com.tsc9526.monalisa.core.query.partition.CreateTableCache;
import com.tsc9526.monalisa.core.tools.ClassHelper.FGS;
import com.tsc9526.monalisa.core.tools.JavaBeansHelper;
import com.tsc9526.monalisa.core.tools.ModelHelper;

import freemarker.log.Logger;

/**
 * 数据库表模型
 * 
 * 
 * @author zzg.zhou(11039850@qq.com)
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class Model<T extends Model> implements Serializable {
	static Logger logger=Logger.getLogger(Model.class.getName());
	
	private static final long serialVersionUID = 703976566431364670L;

	protected static DataSourceManager dsm = DataSourceManager.getInstance();

	protected transient ModelMeta   modelMeta;		
	protected transient ModelHolder modelHolder;
	protected transient DBConfig db;

	protected String   TABLE_NAME;
	protected String[] PRIMARY_KEYS;
	
	public Model() {
	}

	public Model(String tableName, String... primaryKeys) {
		this.TABLE_NAME   = tableName;
		this.PRIMARY_KEYS = primaryKeys;
	}

	protected ModelMeta mm() {
		if (modelMeta==null){
			modelMeta=ModelMeta.getModelMeta(this);		 			 
		}		 		
		return modelMeta;
	}

	protected synchronized ModelHolder holder() {
		if(modelHolder==null){
			modelHolder=new ModelHolder(this);
		}
		return modelHolder;
	}
	
	/**
	 * 设置访问数据库
	 * 
	 * @param db
	 * @return
	 */
	public T use(DBConfig db) {
		this.db=db;
	 
		return (T) this;
	}
	
	/**
	 * Check if the model is dirty
	 * 
	 * @return
	 */
	public boolean dirty() {
		return holder().dirty;
	}

	/**
	 * Set the model dirty
	 * 
	 * @param dirty
	 * @return
	 */
	public T dirty(boolean dirty) {
		holder().dirty = dirty;
		return (T) this;
	}

	/**
	 * Check if the model is db entity
	 * 
	 * @return
	 */
	public boolean entity() {
		return holder().entity;
	}

	/**
	 * Set the model db entity
	 * 
	 * @param entity
	 * @return
	 */
	public T entity(boolean entity) {
		holder().entity = entity;
		return (T) this;
	}

	/**
	 * @see com.tsc9526.monalisa.core.tools.ModelHelper#parseModel(Model,
	 *      Object)
	 * 
	 * @param dataObject
	 *            HttpServletRequest, Map, JsonObject, String(json format),
	 *            JavaBean
	 * @param mappings
	 *            [Options] Translate dataObject field to model field <br>
	 *            For example: <br>
	 *            "user_id=id", ... // Parse dataObject.user_id to Model.id<br>
	 *            Another example:<br>
	 *            "~XXX." //Only parse dataObject's fields with prefix: XXX.
	 * 
	 */
	public T parse(Object dataObject, String... mappings) {
		ModelHelper.parse(this, dataObject, mappings);

		return (T) this;
	}

	/**
	 * Load by primary keys
	 */
	public T load() {
		Query query = dialect().load(this);
		 
		query.use(db());

		Object r = query.getResult();
		return (T) r;
	}

	/**
	 * 存储对象到数据库
	 * 
	 * @return 成功变更的记录数
	 */
	public int save() {
		int r = -1;
		try {
			before(ModelEvent.INSERT);

			doValidate();

			r = new Insert(this).insert(false);

			return r;
		} finally {
			after(ModelEvent.INSERT, r);
		}
	}

	/**
	 * 存储对象到数据库， 如果主键冲突， 则执行更新操作
	 * 
	 * @return 成功变更的记录数
	 */
	public int saveOrUpdate() {
		int r = -1;
		try {
			before(ModelEvent.INSERT_OR_UPDATE);

			doValidate();

			r = new Insert(this).insert(true);

			return r;
		} finally {
			after(ModelEvent.INSERT_OR_UPDATE, r);
		}
	}

	/**
	 * 更新对象到数据库
	 * 
	 * @return 成功变更的记录数
	 */
	public int update() {
		int r = -1;
		try {
			before(ModelEvent.UPDATE);

			doValidate();

			r = new Update(this).update();

			return r;
		} finally {
			after(ModelEvent.UPDATE, r);
		}
	}

	/**
	 * 从数据库删除该记录
	 * 
	 * @return 成功变更的记录数
	 */
	public int delete() {
		int r = -1;
		try {
			before(ModelEvent.DELETE);
			r = new Delete(this).delete();
			return r;
		} finally {
			after(ModelEvent.DELETE, r);
		}
	}

	public void before(ModelEvent event) {
		if (mm().listener != null) {
			mm().listener.before(event, this);
		}

		if (event == ModelEvent.INSERT || event == ModelEvent.UPDATE) {
			Date now = new Date();
			
			String createTimeColumn=DbProp.PROP_TABLE_AUTO_SET_CREATE_TIME.getValue(mm().db,mm().tableName);
			String updateTimeColumn=DbProp.PROP_TABLE_AUTO_SET_UPDATE_TIME.getValue(mm().db,mm().tableName);
			
			if (event == ModelEvent.INSERT && createTimeColumn!=null && createTimeColumn.trim().length()>0) {
				FGS createTime = field(createTimeColumn.trim());
				if (createTime != null && createTime.getObject(this)==null) {
					createTime.setObject(this, now);
				}
			}
			
			if(updateTimeColumn!=null && updateTimeColumn.trim().length()>0){
				FGS updateTime = field(updateTimeColumn.trim());
				if (updateTime!= null && updateTime.getObject(this)==null) {
					updateTime.setObject(this, now);
				}
			}
		}
	}

	public void after(ModelEvent event, int r) {
		if (r >= 0) {
			dirty(false);

			switch (event) {
			case INSERT:
				entity(true);
				break;
			case DELETE:
				entity(false);
				break;
			case UPDATE:
				entity(true);
				break;
			case INSERT_OR_UPDATE:
				entity(true);
				break;
			case LOAD:
				entity(true);
				break;
			}
		}

		if (mm().listener != null) {
			mm().listener.after(event, this, r);
		}
	}

	/**
	 * 设置所有字段为null, 并清除改变字段集合
	 * 
	 * @return this
	 */
	public T clear() {
		for (FGS fgs : fields()) {
			fgs.setObject(this, null);
		}

		holder().clearChanges();

		entity(false);

		return (T) this;
	}

	/**
	 * 设置所有字段为缺省值
	 * 
	 * @return this
	 */
	public T defaults() {
		for (FGS fgs : fields()) {
			Column c = fgs.getAnnotation(Column.class);
			String v = c.value();
			if("NULL".equalsIgnoreCase(v)) {
				if(c.notnull()){
					Object x=JavaBeansHelper.getDefaultValue(c.jdbcType(),fgs.getType());
					fgs.setObject(this, x);
				}
			}else {
				fgs.setObject(this, v);
			}
		}

		entity(false);

		return (T) this;
	}

	/**
	 * 
	 * @return 返回数据库方言
	 */
	public Dialect dialect() {
		return mm().dialect;
	}

	/**
	 * 
	 * @return 返回表的字段列表
	 */
	public Collection<FGS> fields() {
		return mm().fields();
	}

	/**
	 * 
	 * @return fields if called set("xxx",value) or setXxx()
	 */
	public Collection<FGS> changedFields() {
		return holder().changedFields();
	}

	protected void fieldChanged(String fieldJavaName) {
		holder().fieldChanged(fieldJavaName);
	}

	public FGS field(String name) {
		return mm().findFieldByName(name);
	}

	public boolean updateKey() {
		return holder().updateKey;
	}

	/**
	 * How to update the model's primary key:<br>
	 * <code>
	 *   Model model=new Model(oldId).load(); <br>
	 *   model.updateKey(true); //Set enable update primary key! <br>
	 *   model.setId(newId);    //Set new primary key <br>
	 *   model.setXxx ...       //Set other fields ...<br>
	 *   <br>
	 *   Update update=new Update(model); <br>
	 *   update.update("id=?",oldId);<br>
	 * </code>
	 * 
	 * @return Enable update the model's primary key, default is: false
	 */
	public T updateKey(boolean updateKey) {
		return (T) this;
	}

	public T set(String name, Object value) {
		FGS fgs = field(name);
		if (fgs != null) {
			fgs.setObject(this, value);

			if (fgs.getSetMethod() == null) {
				holder().fieldChanged(fgs.getFieldName());
			}
		}else{
			String throwException=DbProp.PROP_TABLE_EXCEPTION_IF_SET_FIELD_NOT_FOUND.getValue(mm().db,mm().tableName);
			if("true".equalsIgnoreCase(throwException)){
				throw new RuntimeException("Field not found: "+name);
			}
		}
		
		return (T) this;
	}

	public Object get(String name) {
		FGS fgs = field(name);
		if (fgs != null) {
			return fgs.getObject(this);
		}
		return null;
	}

	/**
	 * 
	 * @return 返回表名等信息
	 */
	public Table table() {
		if (mm().mp != null) {
			return CreateTableCache.getTable(mm().mp, this, mm().table);
		} else {
			return mm().table;
		}
	}

	/**
	 * 
	 * @return 返回表的自增字段， 如果没有则返回null
	 */
	public FGS autoField() {
		return mm().autoField;
	}

	/**
	 * 
	 * @return 数据库连接信息
	 */
	public DBConfig db() {
		return db==null?mm().db:this.db;
	}
 

	public boolean readonly() {
		return holder().readonly;
	}

	public void readonly(boolean readonly) {
		holder().readonly = readonly;
	}

	/**
	 * 
	 * @return 逗号分隔的字段名， *： 表示返回所有字段
	 */
	public String filterFields() {
		return holder().filterFields();
	}

	/**
	 * 排除表的某些字段。 用于在查询表时， 过滤掉某些不必要的字段
	 * 
	 * @param fields
	 *            要排除的字段名
	 * 
	 * @return 模型本身
	 */
	public T exclude(String... fields) {
		holder().exclude(fields);

		return (T) this;
	}

	/**
	 * 排除大字段（字段长度 大于等于 #Short.MAX_VALUE)
	 * 
	 * @return 模型本身
	 */
	public T excludeBlobs() {
		holder().excludeBlobs();

		return (T) this;
	}

	/**
	 * 排除超过指定长度的字段
	 * 
	 * @param maxLength
	 *            字段长度
	 * 
	 * @return 模型本身
	 */
	public T excludeBlobs(int maxLength) {
		holder().excludeBlobs(maxLength);

		return (T) this;
	}

	/**
	 * 只提取某些字段
	 * 
	 * @param fields
	 *            需要的字段名称
	 * @return 模型本身
	 */
	public T include(String... fields) {
		holder().include(fields);

		return (T) this;
	}

	/**
	 * 复制对象数据
	 */
	public T copy() {
		return (T) mm().copyModel(this);
	}
	
	T create(){
		try{
			Model<T> x=getClass().newInstance();
			 
			x.TABLE_NAME  =this.TABLE_NAME;
			x.PRIMARY_KEYS=this.PRIMARY_KEYS;
			x.db          =this.db;
			
			return (T)x;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	protected void doValidate() {
		mm().doValidate(this);
	}

	/**
	 * 校验字段数据的是否合法.
	 * 
	 * @return 不合法的字段列表{字段名: 错误信息}. 如果没有错误, 则为空列表.
	 */
	public List<String> validate() {
		return mm().validate(this);
	}

	public List<ModelIndex> indexes() {
		return mm().indexes;
	}

	public List<ModelIndex> uniqueIndexes() {
		return mm().uniqueIndexes();
	}

	public ModelListener listener() {
		return mm().listener;
	}

	
	
	public String toString() {
		return ModelHelper.toString(this);
	}

	public String toJson() {		 		 
		return ModelHelper.toJson(this);		 
	}

	public String toXml() {
		return toXml(true, true);
	}

	public String toXml(boolean withXmlHeader, boolean ignoreNullFields) {
		return ModelHelper.toXml(this,withXmlHeader, ignoreNullFields);
	}
}