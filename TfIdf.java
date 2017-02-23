import java.io.*;
import java.util.*;
import java.math.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.PorterStemmer;
import java.util.Date;
import java.sql.Timestamp;

public class TfIdf {
	
	static HashMap<String, HashMap<String, Integer>> globalQueryMap= new HashMap<String, HashMap<String, Integer>>();
	static HashMap<String, ArrayList<String>> bm25queries= new HashMap<String, ArrayList<String>>(); 
	String cacmAnswerFile = "data/med_processed.rel";
	String queryFile = "data/med_processed.query"; 
	static HashMap<String, List<String>> results = new HashMap<String, List<String>>();
	HashMap<String, Double> qdenomMap= new HashMap<String, Double>();
	HashMap<String, Double> ddenomMap= new HashMap<String, Double>();

	
	int sum_avg_precision=0, queryno=0;
	
	public void cal_denom()
	{
		for(Map.Entry<String, HashMap<String,Integer>> entry : globalQueryMap.entrySet())
		{
			String query4 = entry.getKey();
			System.out.println("....... "+query4);
			HashMap<String, Integer> map3= entry.getValue();
			
			Double sum=0.0;
			
			for(Map.Entry<String, Integer> entry1:map3.entrySet())
			{
				String term = entry1.getKey();
				Integer qtermfreq = entry1.getValue();
				Double idf;
				
				double numDocs=MyIndexFiles.map.size();
				
				if(MyIndexFiles.allDocCountsMap.containsKey(term))
				{	
					idf=Math.log(numDocs/MyIndexFiles.allDocCountsMap.get(term));
				}
				else
				{
					idf=0.0;
				}
				
				Double tfidfVal=qtermfreq*idf*qtermfreq*idf;
				sum+=tfidfVal;
			}
			sum =Math.sqrt(sum);
			qdenomMap.put(query4, sum);	
		}
		
		for(Map.Entry<String, HashMap<String, Integer>> entry: MyIndexFiles.map.entrySet())
		{
			HashMap<String, Integer> map1= entry.getValue();
			
			Double sum=0.0;
			String document=entry.getKey();
			
			for(Map.Entry<String, Integer> entry1:map1.entrySet())
			{
				String term = entry1.getKey();
				Integer dtermfreq = entry1.getValue();
				Double idf;
				
				double numDocs=MyIndexFiles.map.size();
				
				if(MyIndexFiles.allDocCountsMap.containsKey(term))
				{	
					idf=Math.log(numDocs/MyIndexFiles.allDocCountsMap.get(term));
				}
				else
				{
					idf=0.0;
				}
				
				Double tfidfVal=dtermfreq*idf*dtermfreq*idf;
				sum+=tfidfVal;
			}
			sum =Math.sqrt(sum);
			ddenomMap.put(document, sum);	
		}
		
		
	}
	public void calc_scores(HashMap<String, HashMap<String, Integer>> indexMap, HashMap<String, Integer> allDocsMap, String simMeasure) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter("yoo.txt"));
		cal_denom();
		
		System.out.println("SimMeasure is "+simMeasure);
		for(Map.Entry<String, HashMap<String, Integer>> entry: globalQueryMap.entrySet())
		{
			HashMap<String, Double> scoresMap = new HashMap<String, Double>();
			Map<String, Double> sortedScoresMap = new LinkedHashMap<String, Double>();
			
			java.util.Date date1= new java.util.Date();
			long oldTime;
			//System.out.println("Printing value of time");
			oldTime=date1.getTime();
			
			String query11 = entry.getKey();
			HashMap<String, Integer> map1= entry.getValue();
			
			for(Map.Entry<String, Integer> entry1:map1.entrySet())
			{
				
				//System.out.println("came inside for the terms of the query");
				
				String term= entry1.getKey();
				Integer termfreq = entry1.getValue();
				
				double numDocs=MyIndexFiles.map.size();
				Double idf;
				if(allDocsMap.containsKey(term))
				{	
					idf=Math.log(numDocs/allDocsMap.get(term));
				}
				else
				{
					idf=0.0;
				}
				
				for(Map.Entry<String, HashMap<String,Integer>> entry2: indexMap.entrySet())
				{
					String docname= entry2.getKey();
					HashMap<String, Integer> map2= entry2.getValue();
					
					Double tfQuery=0.0, tfDoc=0.0, tfidf=0.0;
					
					if(map2.containsKey(term))
					{
						Integer doctermfreq= map2.get(term);
						
						//tfQuery=termfreq
						
						//System.out.println("before atn");
						if(simMeasure.equals("atn.atn"))
						{
							System.out.println("came inside atn");
							tfDoc = 0.5 + 0.5 * (doctermfreq / getMaxFromHash(map1)); 
							tfQuery = 0.5 + 0.5 * (termfreq / getMaxFromHash(map2)); 
							tfidf = (double)tfDoc *idf*tfQuery*idf;
						}
						
						if(simMeasure.equals("atc.atc"))
						{
							//System.out.println("came inside atc");
							tfDoc = 0.5 + 0.5 * (doctermfreq / getMaxFromHash(map1)); 
							tfQuery = 0.5 + 0.5 * (termfreq / getMaxFromHash(map2)); 
							tfidf = (double)tfDoc *idf*tfQuery*idf*(1/qdenomMap.get(query11))*(1/ddenomMap.get(docname));
						}
						if(simMeasure.equals("ann.bpn"))
						{
							System.out.println("came inside ann");
							if(map1.containsKey(term))
							{	
								tfQuery=1.0;
							}
							else
							{
								tfQuery=0.0;
							}
							idf=Math.max(0.0, Math.log(numDocs-(allDocsMap.get(term))/allDocsMap.get(term)));
							
							
							tfDoc = 0.5 + 0.5 * (doctermfreq / getMaxFromHash(map1)); 
							tfidf = (double)tfDoc *idf*tfQuery;
						}
						
						if(scoresMap.containsKey(docname))
						{
							Double value= scoresMap.get(docname);
							scoresMap.put(docname, (value+tfidf));
						}
						else
						{
							scoresMap.put(docname, tfidf);
						}
					}
				}
			}
			
			sortedScoresMap= sortByValue(scoresMap);
			List<String> topp= new ArrayList<String>();
			int b=0;
			for(Map.Entry<String, Double> entry9: sortedScoresMap.entrySet())
			{
				//System.out.println("came in for "+entry9.getKey());
				//System.out.println(" value is "+entry9.getValue());
				if(b<100)
				{
					topp.add(entry9.getKey());
					b += 1;
				}
				else{
					break;
				}
			}
			//System.out.println("Put in for query "+query11);
			//System.out.println("Put in list "+topp);
			results.put(query11, topp);
			queryno+=1;
			long newTime;
			java.util.Date date2= new java.util.Date();
			//System.out.println("Printing value of time");
			newTime= date2.getTime();
			long diff;
			diff=newTime-oldTime;
			System.out.println("Time Query"+query11+"ran is:"+diff);
		}

		out.close();
	}
	
	public Integer getMaxFromHash(Map<String, Integer> hash)
	{
		Integer max =0;
		for(Map.Entry<String, Integer> entry20: hash.entrySet())
		{
			if(entry20.getValue()>max)
			{
				max=entry20.getValue();
			}
		}
		return max;
	}
	
	public void queryIndexBuilder(String queryFile_name) throws IOException
	{
		CharArraySet charArray=null;
		MyAnalyzer analyzer= new MyAnalyzer(charArray);
		
		PorterStemmer stemmer = new PorterStemmer();	
		 
		Scanner scanner= null;
		try {
			FileReader reader = new FileReader(queryFile_name);
			scanner = new Scanner(reader);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(scanner.hasNextLine())
		{
			HashMap<String, Integer> queryIndexMap= new HashMap<String, Integer>();
			String line= scanner.nextLine();
			String[] query = line.split(",",2);
			
			TokenStream tokenStream=analyzer.tokenStream("contents",new StringReader(query[1]));
			CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
			tokenStream.reset();
			while (tokenStream.incrementToken())
			{
				String token = cattr.toString();
				boolean val = MyIndexFiles.stop_words.containsValue(token);
				
				if(!val)
				{
					if (queryIndexMap.containsKey(token)) 
					{
						Integer frequency= queryIndexMap.get(token);
						queryIndexMap.put(token, frequency+1);
					} 
					else 
					{
						queryIndexMap.put(token, 1);
					}
				}
			}
			tokenStream.close();
			//System.out.println("QQQQQQQQQQQQQQQQQQQ "+query[0]);
			//System.out.println("MMMMMMMMMMMMMMMMMMM "+queryIndexMap);
			globalQueryMap.put(query[0], queryIndexMap);
		}
		
	}
		
	
	public CharArraySet covertToCharArraySet(String file_name) throws IOException
	{
		String line= null;
		CharArraySet charArray = new CharArraySet(10000,true);
		BufferedReader buff = null;
		try {
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
	
	public static Map sortByValue(HashMap scoresMap) {	 
		//System.out.println("Came inside sortBy value");
		List list = new LinkedList(scoresMap.entrySet());
	 
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
							.compareTo(((Map.Entry) (o1)).getValue());
			}
		});
	 
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		//System.out.println("returning sortedmap");
		return sortedMap;
	}
	
	public static void docRanking()
	{
		System.out.println("entering docRanking");
		for(Map.Entry<String, HashMap<String, Integer>> entry7:globalQueryMap.entrySet())
		{
			ArrayList<String> tophundred = new ArrayList<String>();
			Map<String,Double> BM25rankingsmap = new HashMap<String, Double>();
			String queryString=entry7.getKey();
			HashMap<String, Integer> querytfsmap=entry7.getValue();
			
			for(Map.Entry<String, Integer> entry8: querytfsmap.entrySet())
			{
				String term1= entry8.getKey();
				Integer termfreq1 = entry8.getValue();
				//System.out.println("term 1 is "+term1);
				for(Map.Entry<String, HashMap<String, Integer>> entry121 : MyIndexFiles.map.entrySet())
				{
					String dnamee= entry121.getKey();
					HashMap<String, Integer> mapHash = entry121.getValue();
					//System.out.println("dnam is "+dnamee);
					double dl= MyIndexFiles.lengthmap.get(dnamee);
					
					//System.out.println("dl retrieved value is "+dl);
					//System.out.println("globalLength:"+MyIndexFiles.globallength);
					double avgdl= (double) (MyIndexFiles.globallength)/(double)(MyIndexFiles.map.size());
					double mul_fac=dl/avgdl;
					double K=  (1.2 *( 0.25 + 0.75 *mul_fac));
					//System.out.println("K is "+K);
					//R=0, ri=0
					Integer ni=0, fi=0;
					if(MyIndexFiles.allDocCountsMap.containsKey(term1))
							{
								ni= MyIndexFiles.allDocCountsMap.get(term1);
							}
				
					Integer N=MyIndexFiles.map.size();
					if(mapHash.containsKey(term1))
					{
						fi= mapHash.get(term1);
					}
					
					Integer qfi= termfreq1;
					Double first=(Math.log((double)(N-ni+0.5)/(double)(ni+0.5)));
					Double second=((double)(2.2*fi)/(double)(K+fi));
					Double third=(double) ((101*qfi)/(double)(100+qfi));
					//System.out.println("first is "+first);
					//System.out.println("second is "+second);
					//System.out.println("third is "+third);
					double result= first*second*third;
					//System.out.println("results is "+result);
					if(BM25rankingsmap.containsKey(dnamee))
					{
						Double val= BM25rankingsmap.get(dnamee);
						BM25rankingsmap.put(dnamee, (val+result));
					}
					else
					{
						BM25rankingsmap.put(dnamee, result);
					}
						
				}
				
			}
			
			for(Map.Entry<String, Double> entry27:BM25rankingsmap.entrySet())
			{
				System.out.println("Document NAME "+entry27.getKey());
				System.out.println("Document BM25 score is "+entry27.getValue());
			}
			
			Map<String, Double> sortedScoresMap = new LinkedHashMap<String, Double>();
			sortedScoresMap= sortByValue((HashMap) BM25rankingsmap);
			int c=1;
			for(Map.Entry<String, Double> entry13 : sortedScoresMap.entrySet() )
			{
				if(c<=100)
				{
					String dn = entry13.getKey();
					tophundred.add(dn);
					c=c+1;
				}
			}
			bm25queries.put(queryString, tophundred);
		}
	}
	
}
