import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

public class MyIndexFiles {
	
	static HashMap<String, Integer> allDocCountsMap = new HashMap<String, Integer>();
	static HashMap<String, HashMap<String, Integer>> map= new HashMap<String, HashMap<String, Integer>>();
	static HashMap<String, String> stop_words = new HashMap<String, String>();
	static HashMap<String, Integer> lengthmap =new HashMap<String, Integer>();
	static Integer globallength=0;
	
	public void indexBuilder(String dir_name, String queryFile) throws IOException
	{
		CharArraySet carray=null;
		carray= covertToCharArraySet("data/stopwords/stopwords_indri.txt");
		MyAnalyzer analyzer= new MyAnalyzer(carray);
		PorterStemmer stemmer = new PorterStemmer();	
		createStopWords("data/stopwords/stopwords_indri.txt");
		File dir = new File(dir_name);
		Scanner scanner= null;
		
		
		File[] files= dir.listFiles();
		for(File file:files)
		{
			//System.out.println("Came in for file "+file.toString());
			HashMap<String, Integer> tfmap= new HashMap<String, Integer>();
			try {
				FileReader reader = new FileReader(file);
				scanner = new Scanner(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			while(scanner.hasNextLine())
			{
				String line= scanner.nextLine();
				TokenStream tokenStream=analyzer.createComponents("contents",new StringReader(line)).getTokenStream();
				CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
				tokenStream.reset();
				while (tokenStream.incrementToken())
				{
					
					String token = cattr.toString();
					boolean val = stop_words.containsValue(token);

					if (!val) {
						if (tfmap.containsKey(token)) {
							Integer frequency= tfmap.get(token);
							tfmap.put(token, frequency+1);
						} 
						else 
						{
							tfmap.put(token, 1);
							if(allDocCountsMap.containsKey(token))
							{
								Integer freq=allDocCountsMap.get(token);
								allDocCountsMap.put(token, freq+1);
							}
							else
							{
								allDocCountsMap.put(token, 1);
							}
						}
					}
				}
			
				tokenStream.close();
			}
				
			String path=(file.toString());
			path=path.replace('\\', '/');
			String[] fn=path.split("/");
			
			String[] fn1=fn[2].split("\\.");
			//System.out.println("DDDDDDDDDDDDD "+fn1[0]);
			//System.out.println("HHHHHHHHHHHHH "+tfmap);
			map.put(fn1[0], tfmap);
			
			for(Map.Entry<String, HashMap<String, Integer>> m: map.entrySet())
			{
				String dname=m.getKey();
				//System.out.println("map has docnameeeeeeeeeeeeeeeee "+dname);

				HashMap<String,Integer> tfval=m.getValue();
				//System.out.println("tfvaaaaaaaalllllllll "+tfval);
				Integer doclen= 0;
				for(Map.Entry<String, Integer> entry10: tfval.entrySet())
				{
					doclen=doclen+entry10.getValue();
					globallength=globallength+entry10.getValue();
				}
				lengthmap.put(dname, doclen);
			}
		}
		for(Map.Entry<String, Integer> yo:lengthmap.entrySet())
		{
			System.out.println("document name is "+yo.getKey());
			System.out.println("length "+yo.getValue());
		}
	}
	public static void printcount()
	{
		int sumfreq=0;
		for(Map.Entry<String, HashMap<String, Integer>> entry29: map.entrySet())
		{
			HashMap<String, Integer> map30 = entry29.getValue();
			if(map30.containsKey("exhibit"))
			{
				sumfreq=sumfreq+map30.get("exhibit");
			}
		}
		System.out.println("Sum of frequeesncies is "+sumfreq);
	}
	
	public CharArraySet covertToCharArraySet(String file_name) throws IOException
	{
		String line= null;
		CharArraySet charArray = new CharArraySet(10000,true);
		BufferedReader buff = null;
		
		try 
		{
			buff= new BufferedReader(new FileReader(file_name));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while((line=buff.readLine())!=null)
		{
			charArray.add(buff.toString());
		}
		return charArray;
	}
	

	public void createStopWords(String filename) {

		try {
			BufferedReader bbr = new BufferedReader(new FileReader(filename));
			String ln = "";
	
			while ((ln = bbr.readLine()) != null) {
				stop_words.put(ln, ln);
			}
			bbr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}