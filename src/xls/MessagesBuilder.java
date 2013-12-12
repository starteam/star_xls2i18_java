package xls;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class MessagesBuilder 
{
	boolean json = false;
	String defaultMsgs = null;
	
	public static void main(String[] args) 
	{
		String xls = null;
		String defaultMsgs = null;
		if( args.length == 2 )
		{
		xls = args[0];
		defaultMsgs = args[1];
		new MessagesBuilder(xls, defaultMsgs,false);
		}
		else if( args.length == 3 )
		{
			if( "-json".equalsIgnoreCase(args[0])) {
				xls = args[1];
				defaultMsgs = args[2];
				new MessagesBuilder(xls, defaultMsgs,true);
			}
			else
			{
				help();
			}
		}
		else
		{
			help();
		}
	}
	
	static void help() 
	{
		System.out.println( "For more info visit: https://github.com/starteam/star_xls2i18_java ");
	}
	
	public MessagesBuilder(String xlsFilePath, String msgsFilePath, boolean json) 
	{
		super();
		this.json = json;
		this.defaultMsgs = msgsFilePath;
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
								if( json )
								{
									createMsgsJsonFile(props[i],lanArray[i],myProp);
								}
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
	

	private void createMsgsJsonFile(Properties props, String lan, Properties myProp) 
	{	
		String[] defaultMsgsPath = defaultMsgs.split("\\.");
		TreeMap<String, String> propMap = new TreeMap<String, String>();
		for( java.util.Map.Entry<Object,Object> e : myProp.entrySet())
		{
			propMap.put( e.getKey().toString() , e.getValue().toString() );
		}
		for( java.util.Map.Entry<Object,Object> e : props.entrySet())
		{
			propMap.put( e.getKey().toString() , e.getValue().toString() );
		}
		try
		{
			FileOutputStream msgs = new FileOutputStream(defaultMsgsPath[0] + "_"+ lan + ".json");
			StringBuffer sb = new StringBuffer();
			sb.append( "{");
			for( java.util.Map.Entry<String,String> e : propMap.entrySet())
			{
				sb.append( MessageFormat.format( "{0}:{1},", quote(e.getKey().toString()) , quote(e.getValue().toString()) ));
//				sb.append( MessageFormat.format( "\"{0}\":\"{1}\",", URLEncoder.encode(e.getKey().toString()) , URLEncoder.encode(e.getValue().toString()) ));
				
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append( "}");
			msgs.write( sb.toString().getBytes());
			msgs.flush();
			msgs.close();
		}
		catch (Exception e)
		{
			System.err.println(e);
		}
	}
	
	public static String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char         c = 0;
        int          i;
        int          len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String       t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
//                if (b == '<') {
                    sb.append('\\');
//                }
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
               sb.append("\\r");
               break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
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
