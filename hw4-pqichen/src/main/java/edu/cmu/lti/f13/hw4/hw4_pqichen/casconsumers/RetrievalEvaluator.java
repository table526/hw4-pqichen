/**
 * Qichen Pan, AndrewId: pqichen
 * 
 * Filename: RetrievalEvaluator.java
 * 
 * In this file, our UIMA pipeline will calculate the similarities between questions and all the answers.
 * 
 * All the answers will be ranked based on their scores.
 * 
 * UIMA pipeline will also compute MRR to measure the performance of the whole process.
 * 
 * EXTRA: I applied 3 methods for computing similarities: CosineSimilarity, DiceCoefficiency and JaccardCoefficiency.
 * 
 * EXTRA: I used stopwords.txt to optimize the pipeline to achieve higher MRR.
 * 
 */
package edu.cmu.lti.f13.hw4.hw4_pqichen.casconsumers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_pqichen.VectorSpaceRetrieval;
import edu.cmu.lti.f13.hw4.hw4_pqichen.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_pqichen.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_pqichen.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;
	
	/** textList contains text of doc **/
	public ArrayList<String> textList;
	
	/** TokenDict stores Tokens in all doc **/
	public List<Token> TokenDict;
	
	/** Index stores Token numbers in all doc **/
	public ArrayList<Integer> Index;
	
	/** Our pipeline loads all the stopwords in memory to do optimize **/
	public ArrayList<String> StopWords;
		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();

		textList = new ArrayList<String>();
		
		TokenDict = new ArrayList<Token>();
		
		Index = new ArrayList<Integer>();
		
		/**
		 * Initialize StopWords list by reading through "/data/stopwords.txt".
		 */
		StopWords = new ArrayList<String>();
    URL docUrl = RetrievalEvaluator.class.getResource("/data/stopwords.txt");
    if (docUrl == null) {
       throw new IllegalArgumentException("Error opening data/stopwords.txt");
    }
    String tmpStr = new String();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(docUrl.openStream()));
      while ((tmpStr = br.readLine()) != null)
      { 
        StopWords.add(tmpStr);
      }
    } catch (IOException e2) {
      e2.printStackTrace();
	  }
	}

	/**
	 * Get global information from all doc.
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}
		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			//ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);

			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
			textList.add(doc.getText());
			
			//Do something useful here
			//ArrayList<Token> DocToken = new ArrayList<Token>();
			//DocToken = (ArrayList<Token>)Utils.fromFSListToCollection(fsTokenList, Token.class);
			//TokenDict.addAll(DocToken);
			//Index.add(DocToken.size());
		}
		
	}

	/**
	 * In this method, all the computation task will be done.
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		
		/**
		 * Store similarity information of all the 3 methods.
		 */
	  double [] CosineSim = new double[qIdList.size()];
	  double [] DiceCoef = new double[qIdList.size()];
	  double [] JacCoef = new double[qIdList.size()];
		int queryNum = 0;		
		Map<String, Integer> QuestionMap = new HashMap<String, Integer>();
		/**
		 * Compute similarities using 3 methods.
		 */
		for(int i = 0; i < qIdList.size(); i++)
		{
		  if(qIdList.get(i) == queryNum)
		  {
		    /**
		     * Initialize AnswerMap.
		     */
		    Map<String, Integer> AnswerMap = fromTexttoMap(textList.get(i));

        CosineSim[i] = computeCosineSimilarity(QuestionMap, AnswerMap);
        DiceCoef[i] = computeDiceCoefficiency(QuestionMap, AnswerMap);
        JacCoef[i] = computeJaccardCoefficiency(QuestionMap, AnswerMap);
		  }else
		  {
		    /**
		     * Initialize QuestionMap.
		     */
		    QuestionMap = fromTexttoMap(textList.get(i));
		    
		    queryNum = qIdList.get(i);
		    CosineSim[i] = 1.0;
		    DiceCoef[i] = 1.0;
        JacCoef[i] = 1.0;
		  }
		  
		}

   /**
    * Compute MRR for all the 3 methods, and output all the MRRs.
    */
		System.out.println("-------------------------------------------------------------------\nUsing Cosine Similarity:");
    int [] rankOftrue = ComputeRank(queryNum, CosineSim);
    double metric_mrr = compute_mrr(rankOftrue);
    System.out.println(" (MRR) Mean Reciprocal Rank with Cosine Similarity::" + metric_mrr);
    
    System.out.println("\n\n-------------------------------------------------------------------\nUsing Dice Coefficiency:");
    rankOftrue = ComputeRank(queryNum, DiceCoef);
    metric_mrr = compute_mrr(rankOftrue);
    System.out.println(" (MRR) Mean Reciprocal Rank with Dice Coefficiency::" + metric_mrr);

    System.out.println("\n\n-------------------------------------------------------------------\nUsing Jaccard Coefficiency:");
    rankOftrue = ComputeRank(queryNum, JacCoef);
    metric_mrr = compute_mrr(rankOftrue);
    System.out.println(" (MRR) Mean Reciprocal Rank with Jaccard Coefficiency::" + metric_mrr);
	}

	/**
	 * This method compute CosineSimilarity between a certain answer and its question.
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;

		double QuestionMapSqt = 0.0, AnswerMapSqt = 0.0;
		double Product = 0.0;
		Iterator Qiter = queryVector.keySet().iterator();
		while(Qiter.hasNext())
		{
		  double tmpDouble = (double)(queryVector.get(Qiter.next()));
		  QuestionMapSqt += tmpDouble * tmpDouble;
		}
		Iterator Aiter = docVector.keySet().iterator();
    while(Aiter.hasNext())
    {
      double tmpDouble = (double)(docVector.get(Aiter.next()));
      AnswerMapSqt += tmpDouble * tmpDouble;
    }
    
    Aiter = docVector.keySet().iterator();
    while(Aiter.hasNext())
    {
      String TkText = Aiter.next().toString();
      if(queryVector.containsKey(TkText))
      {
        double A_Freq = (double)(docVector.get(TkText));
        double Q_Freq = (double)(queryVector.get(TkText));
        Product += A_Freq * Q_Freq;
      }else continue;
    }
		
    cosine_similarity = Product / (Math.sqrt(QuestionMapSqt) * Math.sqrt(AnswerMapSqt));
		return cosine_similarity;
	}

	/**
   * 
   * This method compute DiceCoefficiency between a certain answer and its question.
   */
  private double computeDiceCoefficiency(Map<String, Integer> queryVector,
      Map<String, Integer> docVector) {
    double DiceCoef = 0.0;

    double QuestionMapSqt = 0.0, AnswerMapSqt = 0.0;
    double Product = 0.0;
    Iterator Qiter = queryVector.keySet().iterator();
    while(Qiter.hasNext())
    {
      double tmpDouble = (double)(queryVector.get(Qiter.next()));
      QuestionMapSqt += tmpDouble * tmpDouble;
    }
    Iterator Aiter = docVector.keySet().iterator();
    while(Aiter.hasNext())
    {
      double tmpDouble = (double)(docVector.get(Aiter.next()));
      AnswerMapSqt += tmpDouble * tmpDouble;
    }
    
    Aiter = docVector.keySet().iterator();
    while(Aiter.hasNext())
    {
      String TkText = Aiter.next().toString();
      if(queryVector.containsKey(TkText))
      {
        double A_Freq = (double)(docVector.get(TkText));
        double Q_Freq = (double)(queryVector.get(TkText));
        Product += A_Freq * Q_Freq;
      }else continue;
    }
    
    DiceCoef = 2 * Product / (QuestionMapSqt + AnswerMapSqt);
    return DiceCoef;
  }
  
  /**
   * 
   * This method compute JaccardCoefficiency between a certain answer and its question.
   */
  private double computeJaccardCoefficiency(Map<String, Integer> queryVector,
      Map<String, Integer> docVector) {
    double JacCoef = 0.0;

    double QuestionMapSqt = 0.0, AnswerMapSqt = 0.0;
    double Intersection = 0.0, Union = 0.0;
    Iterator Qiter = queryVector.keySet().iterator();
    while(Qiter.hasNext())
    {
      double tmpDouble = (double)(queryVector.get(Qiter.next()));
      QuestionMapSqt += tmpDouble * tmpDouble;
    }
    Iterator Aiter = docVector.keySet().iterator();
    while(Aiter.hasNext())
    {
      double tmpDouble = (double)(docVector.get(Aiter.next()));
      AnswerMapSqt += tmpDouble * tmpDouble;
    }
    
    Aiter = docVector.keySet().iterator();
    while(Aiter.hasNext())
    {
      String TkText = Aiter.next().toString();
      if(queryVector.containsKey(TkText))
      {
        double A_Freq = (double)(docVector.get(TkText));
        double Q_Freq = (double)(queryVector.get(TkText));
        Intersection += A_Freq * Q_Freq;
      }else continue;
    }
    
    Union = QuestionMapSqt + AnswerMapSqt - Intersection;
    JacCoef = Intersection / Union;
    return JacCoef;
  }
  
	/**
	 * 
   * This method compute MRR for the prediction of a certain similarity method.
	 */
	private double compute_mrr(int [] RankOfTrueAnswer) {
		double metric_mrr=0.0;

		for(int i = 0; i < RankOfTrueAnswer.length; i++)
		{
		  metric_mrr += 1 / (double)(RankOfTrueAnswer[i]);
		}
		metric_mrr = metric_mrr / RankOfTrueAnswer.length;
		return metric_mrr;
	}
	
	/**
	 * This method generate a Map using Token List information.
	 */
	private Map<String, Integer> fromTexttoMap(String docText)
	{
	  Map<String, Integer> dstMap = new HashMap<String, Integer>();
	  ArrayList<String> TokenList = new ArrayList<String>();
	  ArrayList<Integer> Freq = new ArrayList<Integer>();
	   /**
     *  Make sure that all the words are in LowerCase.
     */
	  docText = docText.toLowerCase();
    /**
     *  Make sure that any characters other than alphabets and white space will be eliminated. 
     */
	  docText = docText.replaceAll("[^a-z&&\\S]", "");
    
    String [] Tks = docText.split(" ");
    List<String> TokenText = Arrays.asList(Tks);
    /**
     *  Sort the TokenList to make it convenient for further use.
     */
    Collections.sort(TokenText);
    TokenText = new ArrayList<String>(TokenText);
    /**
     * Delete all meaningless words in the TOkenList.
     */
    deleteStopWords(TokenText);
    Iterator<String> i = TokenText.iterator();
    while (i.hasNext()) 
    {
      String tmpStr = i.next();
      if(TokenList.size() > 0)
      {
        /**
         * Calculate freqency of Token.
         */
        if(TokenList.get(TokenList.size() - 1).equals(tmpStr))
        {
          Freq.set(TokenList.size() - 1, Freq.get(TokenList.size() - 1) + 1);
        }else
        {
          TokenList.add(tmpStr);
          Freq.add(1);
        }
      }else
      {
        TokenList.add(tmpStr);
        Freq.add(1);
      }
    }
    for(int j = 0; j < TokenList.size(); j++)
    {
      dstMap.put(TokenList.get(j), Freq.get(j));
    }
    return dstMap;
	}
	
	/**
	 * ComputeRank can compute the rank of TRUE answer in each query.
	 * @param queryNum
	 * @param Similarity
	 * @return
	 */
	public int [] ComputeRank(int queryNum, double [] Similarity)
	{
	  int [] trueAnswerPos = new int[queryNum];
	  int [] rankOftrue = new int[queryNum];

	  /**
	   * Get the position of true answers.
	   */
	  for(int i = 0; i < relList.size(); i++)
	  {
	    if(relList.get(i) == 1)
	    {
	      trueAnswerPos[qIdList.get(i) - 1] = i;
	    }
	  }
	  /**
	   * Update the rank of true answers.
	   */
	  for(int i = 0; i < trueAnswerPos.length; i++)
	  {
	    double trueScore = Similarity[trueAnswerPos[i]];
	    for(int j = 0; j < Similarity.length; j++)
	    {
	      if(qIdList.get(j) != qIdList.get(trueAnswerPos[i]));
	      else
	      {
	        if(Similarity[j] > trueScore)  rankOftrue[i]++;
	      }
	    }
	    System.out.println("Score: " + Double.toString(Similarity[trueAnswerPos[i]]) + 
	            " rank=" + Integer.toString(rankOftrue[i]) +
	            " rel=1 qid=" + Integer.toString(i + 1) + 
	            " " + textList.get(trueAnswerPos[i]) + "\n");
	  }
	  return rankOftrue;
	}
	
	/**
	 * deleteStopWords can delete all meaningless words in a TokenArray.
	 * @param TokenArray
	 */
	public void deleteStopWords(List<String> TokenArray)
	{
	  for(int i = 0; i < TokenArray.size(); i++)
	  {
	    for(int j = 0; j < StopWords.size(); j++)
	    {
	      /**
	       * In case both lists are sorted, we can reduce time consumption by this little trick.
	       */
	      if(TokenArray.get(i).compareTo(StopWords.get(j)) < 0) break;
	      if(TokenArray.get(i).equals(StopWords.get(j)))
	      {
	        TokenArray.remove(i);
	        break;
	      }
	    }
	  }
	}
}

