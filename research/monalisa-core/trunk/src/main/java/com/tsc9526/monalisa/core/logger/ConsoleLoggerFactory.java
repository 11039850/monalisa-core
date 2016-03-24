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
package com.tsc9526.monalisa.core.logger;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
public class ConsoleLoggerFactory implements LoggerFactory {
	static Messager messager=null;
	 
	ConsoleLoggerFactory() {
	}

	public Logger getLogger(String category) {
		return INSTANCE;
	}

	private static final Logger INSTANCE = new Logger() {
		public void debug(String message) {
			println("DEBUG",message);
		}

		public void debug(String message, Throwable t) {
			println("DEBUG",message,t);
		}

		public void error(String message) {
			println("ERROR",message);
		}

		public void error(String message, Throwable t) {
			println("ERROR",message,t);
		}

		public void info(String message) {
			println("INFO",message);
		}

		public void info(String message, Throwable t) {
			println("INFO",message,t);
		}

		public void warn(String message) {
			println("WARN",message);
		}

		public void warn(String message, Throwable t) {
			println("WARN",message,t);
		}

		public boolean isDebugEnabled() {
			return false;
		}

		public boolean isInfoEnabled() {
			return true;
		}

		public boolean isWarnEnabled() {
			return true;
		}

		public boolean isErrorEnabled() {
			return true;
		}

		public boolean isFatalEnabled() {
			return true;
		}
		
		private void println(String level,String message){
			println(level,message,null);
		}
		
		private void println(String level,String message, Throwable t){
			StringBuffer sb=new StringBuffer();
			
			sb.append("[").append(level).append("] ").append(message);
			
			if(t!=null){
				StringWriter writer=new StringWriter(4*1024);
				t.printStackTrace(new PrintWriter(writer));
				sb.append("\r\n").append(writer.getBuffer().toString());
			}
			
			if(messager!=null){
				if(isError(level)){
					messager.printMessage(Kind.ERROR, sb.toString());
				}else{
					messager.printMessage(Kind.NOTE, sb.toString());
				}
			}else{
				if(isError(level)){
					System.err.println(sb.toString());
				}else{
					System.out.println(sb.toString());
				}
			}
		}
		
		private boolean isError(String level) {
			return level.equals("ERROR") || level.equals("FATAL");
		}
	};
}
