import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.StopAnalyzer;
// import lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class EvaluateQueries {
	
	public static void main(String[] args) throws IOException 
	{
		String cacmDocsDir = "data/cacm"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents
		
		String cacmIndexDir = "data/index/cacm"; // the directory where index is written into
		String medIndexDir = "data/index/med"; // the directory where index is written into
		
		String cacmQueryFile = "data/cacm_processed.query";    // CACM query file
		String cacmAnswerFile = "data/cacm_processed.rel";   // CACM relevance judgements file

		String medQueryFile = "data/med_processed.query";    // MED query file
		String medAnswerFile = "data/med_processed.rel";   // MED relevance judgements file
		
		int cacmNumResults = 100;
		int medNumResults = 100;
		
		
		String sim2 = "atc.atc";
		String sim1 = "atn.atn";
		String sim3 = "ann.bpn";
		String sim4 = "bm25";
		
		CharArraySet stopwords = new CharArraySet(0, false);
	    
		System.out.println("My values");
		//System.out.println("Oho "+evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
			//	cacmAnswerFile));
		
		System.out.println("\n");
		
	System.out.println("For atc.atc:");
		System.out.println("*** "+evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
				cacmAnswerFile, sim2));
		
	/*	System.out.println("*** "+evaluate(medIndexDir, medDocsDir, medQueryFile,
				medAnswerFile, sim1));*/
		
		//System.out.println("For atn.atn:");
		//System.out.println("*** "+evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
		//		cacmAnswerFile, sim2));
		
		//System.out.println("*** "+evaluate(medIndexDir, medDocsDir, medQueryFile,
			//	medAnswerFile, sim2));
		
		//System.out.println("For ann.bpn:");
		//System.out.println("*** "+evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
			//	cacmAnswerFile, sim4));
		
		//System.out.println("*** "+evaluate(medIndexDir, medDocsDir, medQueryFile,
			//	medAnswerFile, sim4));
		
		MyIndexFiles.printcount();
		
	}

	public static Map<Integer, String> loadQueries(String filename) {
		HashMap<Integer, String> queryIdMap = new HashMap<Integer, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		String line;
		try {
			while ((line = in.readLine()) != null) {
				int pos = line.indexOf(',');
				queryIdMap.put(Integer.parseInt(line.substring(0, pos)), line
						.substring(pos + 1));
			}
		} catch(IOException e) 
		{
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryIdMap;
	}

	private static Map<Integer, HashSet<String>> loadAnswers(String filename) {
		HashMap<Integer, HashSet<String>> queryAnswerMap = new HashMap<Integer, HashSet<String>>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				HashSet<String> answers = new HashSet<String>();
				for (int i = 1; i < parts.length; i++) {
					answers.add(parts[i]);
				}
				queryAnswerMap.put(Integer.parseInt(parts[0]), answers);
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryAnswerMap;
	}

	public static double MAP(HashSet<String> answers,
			List<String> results) {
		//System.out.println("reults passed "+results);
		double matches = 0;
		double precision_sum = 0;
		for (int i = 0; i < Math.min(answers.size(), results.size()); i++) {
			String result = results.get(i);
			if (answers.contains(result)) {
				matches++;
				precision_sum += matches/(i+1);
			}
		}
		
		return precision_sum / answers.size();
	}

	private static double precision(HashSet<String> answers,
            List<String> results) {
        double matches = 0;
        double sum = 0;
        double position = 0;
        //double precision = 0;
        for (String result : results) {
            position++;
            if (answers.contains(result)){
                matches++;
                sum += matches/position;
            }
        }
        return sum / matches;
    }

	public static double evaluate(String indexDir, String docsDir,
			String queryFile, String answerFile, String simMeasure) throws IOException {

		
		MyIndexFiles in = new MyIndexFiles();
		in.indexBuilder(docsDir, queryFile);
		TfIdf tfidf = new TfIdf();
		tfidf.queryIndexBuilder(queryFile);
		if(simMeasure.contains("bm25"))
		{
			TfIdf.docRanking();
		}
		else
		{
			tfidf.calc_scores(MyIndexFiles.map,MyIndexFiles.allDocCountsMap, simMeasure);
		}
		
		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);

		// Search and evaluate
		double sum = 0;
		Integer resultsIndex=0;
		for (Integer i : queries.keySet()) 
		{
			Double retres;
				if(simMeasure.equals("bm25"))
				{
					retres= MAP(queryAnswers.get(i), TfIdf.bm25queries.get(i.toString()));
				}
				else
				{
					retres= MAP(queryAnswers.get(i), TfIdf.results.get(i.toString()));
				}
				//System.out.println("Average precision returned is "+retres);
				sum += retres;
				resultsIndex += 1;
				//System.out.printf("\nTopic %d  ", i);
				//System.out.println();
				//System.out.println("Sum is " +sum);
		}

		return sum / queries.size();
	}

}
