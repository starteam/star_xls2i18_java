package xls;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class MessagesBuilder 
{
	static String xls = null;
	static String defaultMsgs = null;
	
	public static void main(String[] args) 
	{
		xls = args[0];
		defaultMsgs = args[1];
		
		new MessagesBuilder(xls, defaultMsgs);
	}
	
	public MessagesBuilder(String xlsFilePath, String msgsFilePath) 
	{
		super();
		InputStream isXls = lanXls(xlsFilePath);
		InputStream isDefault = defaultMsgsFile(msgsFilePath);
		load(isXls, isDefault);
	}
	
	private InputStream lanXls(String xlsFilePath)
	{
		FileInputStream is = null;
		try
		{
			is = new FileInputStream(xlsFilePath);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		return is;
	}
	
	private InputStream defaultMsgsFile(String msgsFilePath)
	{
		FileInputStream is = null;
		try
		{
			is = new FileInputStream(msgsFilePath);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		return is;
	}
	
	private void load(InputStream xlsIS, InputStream msgsIS) 
	{
		try
		{
			POIFSFileSystem fs = new POIFSFileSystem(xlsIS);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheet("Sheet1");
			Iterator<HSSFRow> rows = sheet.rowIterator();
			String[] lanArray = null;
			Properties[] props = null;
			
			if(rows.hasNext())
			{
				HSSFRow r = rows.next();
				lanArray = headerArray(r);
				props = createProps(lanArray);
				while(rows.hasNext())
				{
					r = rows.next();
					mapBuilder(r, props);
				}
			}
			writeMessageFiles(props, msgsIS, lanArray);
			
		}
		catch (Exception e) 
		{
			System.err.println(e);
		}
	}
	
	private void writeMessageFiles(Properties[] props, InputStream defaultProp, String[] lanArray)
	{
		if(props != null && props.length != 0)
		{
			if(defaultProp != null)
			{				
				Properties myProp = new Properties();
				try
				{
					myProp.load(defaultProp);
					
					boolean hasDefault = hasKeys(props[1], myProp);
					boolean hasXls = hasKeys(myProp, props[1]);
					if(!hasXls)
					{
						System.err.println("WARNING: ONE OR MORE KEYS NOT FOUND IN XLS FILE AND WILL NOT BE TRANSLATED. " +  missingKeys(myProp, props[1]));
					}
					if(hasDefault)
					{
						for(int i = 0; props.length != i; i++)
						{
							if(props[i] != null)
							{
								createMsgsFile(props[i], lanArray[i]);
							}
						}
					}
					else if(!hasDefault)
					{
						System.err.println("Default property file src/app/messages.properties is missing one or more keys " + missingKeys(props[1], myProp));
						throw new Exception();
					}
				}
				catch(Exception e)
				{
					System.out.println(e);
				}
			}			
		}
	}
	private boolean hasKeys(Properties prop1, Properties prop0)
	{
		boolean hasKeys = true;
		if(prop0 != null && prop1 != null)
		{
			Enumeration<Object> keys = prop1.keys();
			while(keys.hasMoreElements())
			{
				Object key = keys.nextElement();
				if(!prop0.containsKey(key))
				{
					hasKeys = false;
					System.err.println("Missing Key: " + key);
					return hasKeys;
				}
			}
		}
		return hasKeys;
	}
	
	private Object missingKeys(Properties prop1, Properties prop0)
	{
		Object key = null;
		if(prop0 != null && prop1 != null)
		{
			Enumeration<Object> keys = prop1.keys();
			while(keys.hasMoreElements())
			{
				key = keys.nextElement();
				if(!prop0.containsKey(key))
				{
					return key.toString();
				}
			}
		}
		return key.toString();
	}
	
	private void createMsgsFile(Properties props, String lan) 
	{	
		String[] defaultMsgsPath = defaultMsgs.split("\\.");
		try
		{
			FileOutputStream msgs = new FileOutputStream(defaultMsgsPath[0] + "_"+ lan + "." + defaultMsgsPath[1]);
			props.store(msgs, lan);
		}
		catch (Exception e)
		{
			System.err.println(e);
		}
	}
	
	private static Properties[] createProps(String[] propsArray)
	{
		Properties[] props = new Properties[propsArray.length];
		for(int i = 1; i != props.length; i++)
		{
			props[i] = new Properties();
		}
		return props;
	}

	private void mapBuilder(HSSFRow r, Properties[] props)
	{
		if(r != null && props.length !=0)
		{
			Iterator<HSSFCell> cells = r.cellIterator();
			if(cells.hasNext())
			{	
				String key = String.valueOf(cells.next());		
				while(cells.hasNext())
				{
					HSSFCell value = cells.next();
					String val = String.valueOf(value);
					if(key != null && (value != null && !val.equals("")))
					{
						int colId = value.getColumnIndex();
						props[colId].setProperty(key, val);
					}
				}
			}
		}
	}
	
	private String[] headerArray(HSSFRow r)
	{
		String[] myStringArray = null;
		Iterator<HSSFCell> cells = r.cellIterator();
		ArrayList<String> lan = new ArrayList<String>();
		while(cells.hasNext())
		{
			HSSFCell cell = cells.next();
			String c = String.valueOf(cell);
			lan.add(c);
		}
		if(lan.size() != 0)
		{
			myStringArray = lan.toArray(new String[lan.size()]);
		}
		return myStringArray;
	}

}
