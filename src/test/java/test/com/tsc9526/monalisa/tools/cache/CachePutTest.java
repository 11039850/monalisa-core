/*******************************************************************************************
 *	Copyright (c) 2016, zzg.zhou(11039850@qq.com)
 * 
 *  Monalisa is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.

 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.

 *	You should have received a copy of the GNU Lesser General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************************/
package test.com.tsc9526.monalisa.tools.cache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.tsc9526.monalisa.orm.Query;
import com.tsc9526.monalisa.orm.executor.CacheableExecute;
import com.tsc9526.monalisa.orm.executor.Execute;
import com.tsc9526.monalisa.tools.misc.MelpMisc;

import test.com.tsc9526.monalisa.orm.dialect.mysql.MysqlDB;

/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
@Test
public class CachePutTest {
	public void testPutCache() {
		Query q = MysqlDB.DB.createQuery();
		q.setCacheTime(5000);
		 
		CacheObject<Integer> value = q.execute(getLongExecute());
		Assert.assertEquals(value, new CacheObject<Integer>(9526));
		
		cachedLongValue =new CacheObject<Integer>(9527);
		
		for(int i=0;i<10;i++) {
			MelpMisc.sleep(10);
			Assert.assertEquals(q.execute(getLongExecute()).getValue(), new Integer(9526));
		}
		 
		Query.setPutCacheMode(true); 
		Assert.assertEquals(q.execute(getLongExecute()).getValue(), new Integer(9527));
		
		Query.setPutCacheMode(false); 
		cachedLongValue = new CacheObject<Integer>(9528);
		Assert.assertEquals(q.execute(getLongExecute()).getValue(), new Integer(9527));
	}
	
	private CacheObject<Integer> cachedLongValue = new CacheObject<Integer>(9526);
	
	private Execute<CacheObject<Integer>> getLongExecute(){
		return new CacheableExecute<CacheObject<Integer>>() {
			@Override
			public CacheObject<Integer> execute(Connection conn, String sql, List<?> parameters) throws SQLException {
				return cachedLongValue;
			}

			@Override
			public String getCacheExtraTag() {
				return "test-put-cache";
			}
		};
	}
	
	
}
