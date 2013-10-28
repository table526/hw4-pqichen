/**
 * Qichen Pan, AndrewId: pqichen
 * 
 * Filename: DocumentVectorAnnotator.java
 * 
 * In this file, Tokenlist of each doc will be generated.
 * 
 * Also, each Token in the TokenList will have its exact appearance as its frequency.
 * 
 */

package edu.cmu.lti.f13.hw4.hw4_pqichen.annotators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f13.hw4.hw4_pqichen.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_pqichen.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_pqichen.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		/**
		 *  Make sure that all the words are in LowerCase.
		 */
    docText = docText.toLowerCase();
    /**
     *  Make sure that any characters other than alphabets and white space will be eliminated. 
     */
    docText = docText.replaceAll("[^a-z&&\\S]", "");
    //TO DO: construct a vector of tokens and update the tokenList in CAS
    
    String [] Tks = docText.split(" ");
    List<String> TokenText = Arrays.asList(Tks);
    /**
     *  Sort the TokenList to make it convenient for further use.
     */
    Collections.sort(TokenText);
    Iterator<String> i = TokenText.iterator();
    ArrayList<Token> TokenList = new ArrayList<Token>();
    while (i.hasNext()) 
    {
      String tmpStr = i.next();
      if(TokenList.size() > 0)
      {
        /**
         * Calculate freqency of Token.
         */
        if(TokenList.get(TokenList.size() - 1).getText().equals(tmpStr))
        {
          TokenList.get(TokenList.size() - 1).setFrequency(
                  TokenList.get(TokenList.size() - 1).getFrequency() + 1);
        }else
        {
          Token tmpTk = new Token(jcas);
          tmpTk.setText(tmpStr);
          tmpTk.setFrequency(1);
          TokenList.add(tmpTk);
        }
      }else
      {
        Token tmpTk = new Token(jcas);
        tmpTk.setText(tmpStr);
        tmpTk.setFrequency(1);
        TokenList.add(tmpTk);
      }
    }
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, TokenList));
    
    doc.addToIndexes();    
		

	}

}
