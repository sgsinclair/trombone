/**
 * 
 */
package org.voyanttools.trombone.tool.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.lucene.CorpusMapper;
import org.voyanttools.trombone.lucene.analysis.LemmaAnalyzer;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.model.DocumentToken;
import org.voyanttools.trombone.model.Keywords;
import org.voyanttools.trombone.model.TermInfo;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.nlp.PosLemmas;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.Storage.Location;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.Stripper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("documentTokens")
@XStreamConverter(DocumentTokens.DocumentTokensConverter.class)
public class DocumentTokens extends AbstractCorpusTool implements ConsumptiveTool {

	private List<DocumentToken> documentTokens = new ArrayList<DocumentToken>();
	
	private Pattern otherTokensPattern = Pattern.compile("<[/?]?\\w.*?>", Pattern.DOTALL);
	
	private int total = 0;
	
	@XStreamOmitField
	private List<String> ids = new ArrayList<String>();

	@XStreamOmitField
	private int start;
	
	@XStreamOmitField
	private int limit;
	
	@XStreamOmitField
	private int perDocLimit;
	
	@XStreamOmitField
	private TokenType tokenType;
	
	@XStreamOmitField
	private Keywords stopwords = null;
	
	@XStreamOmitField
	private Keywords whitelist = null;
	
	/**
	 * @param storage
	 * @param parameters
	 */
	public DocumentTokens(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		start = parameters.getParameterIntValue("start", 0);
		limit = parameters.getParameterIntValue("limit", 50);
		if (limit==0) {limit=Integer.MAX_VALUE;}
		tokenType = TokenType.getTokenTypeForgivingly(parameters.getParameterValue("tokenType", "lexical"));
		perDocLimit = parameters.getParameterIntValue("perDocLimit", Integer.MAX_VALUE);
		if (perDocLimit==0) {perDocLimit = Integer.MAX_VALUE;}
	}


	@Override
	public void run(CorpusMapper corpusMapper) throws IOException {
		stopwords = getStopwords(corpusMapper.getCorpus());
		if (parameters.containsKey("whitelist")) {
			whitelist = new Keywords();
			whitelist.load(storage, parameters.getParameterValues("whitelist"));
		}
		boolean noOthers = parameters.getParameterBooleanValue("noOthers");
		total = Integer.MAX_VALUE;
		Corpus corpus = corpusMapper.getCorpus();
		ids = this.getCorpusStoredDocumentIdsFromParameters(corpus);
		List<TermInfo> termInfos = new ArrayList<TermInfo>();
		TermInfo termInfo;
		int documentStart = start;
		Stripper stripper = new Stripper(parameters.getParameterValue("stripTags"));
		String skipToDocId = parameters.getParameterValue("skipToDocId", "");
		boolean isSkipping = true;
		int tokensCounter = 0;
		int[] lastTokenPositions = corpus.getLastTokenPositions(tokenType);
		boolean needPosLemma = parameters.getParameterBooleanValue("withPosLemmas");
		PosLemmas lemmas;
		for (String id : ids) {
			if (skipToDocId.isEmpty()==false && isSkipping==true) {
				if (isSkipping && skipToDocId.equals(id)) {
					isSkipping=false;
				}
				else {
					continue;
				}
			}
			int maxPos = documentStart+limit;
			int currentDocTokensCount = 0;

			int luceneDoc = corpusMapper.getLuceneIdFromDocumentId(id);
			Terms terms = corpusMapper.getIndexReader().getTermVector(luceneDoc, tokenType.name());
			if (terms==null) {continue;}
			TermsEnum termsEnum = terms.iterator();
			Map<String, Integer> docFreqs = new HashMap<String, Integer>();
			while(true) {
				BytesRef term = termsEnum.next();
				if (term!=null) {
					String termString = term.utf8ToString();
					if (stopwords.isKeyword(termString)) {continue;}
					PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
					postingsEnum.nextDoc();
					for (int i=0, len = postingsEnum.freq(); i<len; i++) {
						int pos = postingsEnum.nextPosition();
						if (pos >= documentStart && pos<maxPos) { // out of range
							if (!docFreqs.containsKey(termString)) {
								docFreqs.put(termString, len);
							}
							termInfos.add(new TermInfo(termString, postingsEnum.startOffset(), postingsEnum.endOffset(), pos, 1));
						}
					}
				}
				else {break;}
			}
			Collections.sort(termInfos);
			List<DocumentToken> tokens = new ArrayList<DocumentToken>();
			String document = corpus.getDocument(id).getDocumentString();
//			String document = reader.document(luceneDoc).get(tokenType.name());
			
			
			lemmas = null;
			if (needPosLemma) {
				String name = id+"-poslemmas-"+PosLemmas.serialVersionUID;
				if (storage.isStored(name, Location.cache)) {
					try {
						lemmas = (PosLemmas) storage.retrieve(name, Location.cache);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException("Unable to retrieve PosLemmas", e);
					}
				}
				if (lemmas == null) {
					String lang = corpusMapper.getCorpus().getDocument(id).getMetadata().getLanguageCode();
					LemmaAnalyzer analyzer = new LemmaAnalyzer(storage.getNlpAnnotatorFactory());
					TokenStream tokenStream = analyzer.tokenStream(TokenType.lemma.name(), document+"<!-- language="+lang+" -->");
					tokenStream.reset();
					while (tokenStream.incrementToken()) {}
					// no need to increment
					tokenStream.end();
					tokenStream.close();
					analyzer.close();
					lemmas = analyzer.getPostStreamPosLemmas();
					storage.store(lemmas, name, Location.cache);
				}
			}

			
			
			String string;
			int lastEndOffset = 0;
			int corpusDocumentIndexPosition = corpus.getDocumentPosition(id);
			int lastDocumentTokenPositionIndex = lastTokenPositions[corpusDocumentIndexPosition];
			for (int i=0, len=termInfos.size(); i<len; i++) {
				termInfo = termInfos.get(i);
				if (!noOthers && ((i==0 && start==0 )|| i > 0)) { // get content before first token or filler between tokens
					string = StringUtils.substring(document, lastEndOffset, termInfo.getStartOffset());
					List<DocumentToken> documentOtherTokens = getDocumentOtherTokens(stripper.strip(string), id, corpusDocumentIndexPosition, lastEndOffset);
					documentTokens.addAll(documentOtherTokens);
					// TODO: does this counter need to change?
					// if (len+1<termInfos.size()) {len++;} // extend loop by one
				}
				string = StringUtils.substring(document, termInfo.getStartOffset(), termInfo.getEndOffset());
				DocumentToken dt = new DocumentToken(id, corpusDocumentIndexPosition, string, tokenType, termInfo.getPosition(), termInfo.getStartOffset(), termInfo.getEndOffset(), docFreqs.get(termInfo.getText()));

				// try to inject lemmas and pos if needed and available
				if (lemmas!=null) {
					lemmas.setCurrentByStart(termInfo.getStartOffset());
					String lemma = lemmas.getCurrentLemma();
					if (lemma!=null) {dt.setLemma(lemma);}
					String pos = lemmas.getCurrentPos();
					if (pos!=null) {dt.setPos(pos);}
				}
				
				if (whitelist!=null) {
					if (whitelist.isKeyword(string.toLowerCase())) {
						documentTokens.add(dt);
						currentDocTokensCount++;
					}
				} else {
					documentTokens.add(dt);
					currentDocTokensCount++;
				}
				lastEndOffset = termInfo.getEndOffset();
				tokensCounter++;
				if (!noOthers && (i+1==len && termInfo.getPosition()==lastDocumentTokenPositionIndex)) {
					string = StringUtils.substring(document, lastEndOffset);
					List<DocumentToken> documentOtherTokens = getDocumentOtherTokens(stripper.strip(string), id, corpusDocumentIndexPosition, lastEndOffset);
					documentTokens.addAll(documentOtherTokens);
				}
			}
			
			if (tokensCounter>=limit || currentDocTokensCount>=this.perDocLimit) {break;}
			termInfos.clear();
			documentStart = 0; // reset to the start of next document
		}
	}

	@Override
	public float getVersion() {
		return super.getVersion()+7;
	}
	
	private List<DocumentToken> getDocumentOtherTokens(String string, String documentId, int documentIndex, int startoffset) {
		// <doc>this <b>is very<i><c>important </c></i> </b>test</doc>
		List<DocumentToken> documentTokens = new ArrayList<DocumentToken>();
		Matcher matcher = otherTokensPattern.matcher(string);
		start = 0;
		while (matcher.find()) {
			if (matcher.start()>start) {
				String text = string.substring(start, matcher.start());
				documentTokens.add(new DocumentToken(documentId, documentIndex, text, TokenType.other, -1, startoffset+start, startoffset+start+text.length(), -1)); // -1 position
			}
			start = matcher.start();
			String tag =  matcher.group();
			TokenType tokenType;
			if (tag.charAt(1)=='/') {tokenType=TokenType.closetag;}
			else if (tag.substring(0, tag.length()-1).trim().endsWith("/")) {tokenType = TokenType.emptytag;}
			else {tokenType = TokenType.opentag;}
			if (tag.charAt(1)!='?') { // skip tag declarations
				documentTokens.add(new DocumentToken(documentId, documentIndex, tag, tokenType, -1, startoffset+start, startoffset+start+tag.length(), -1)); // -1 position
			} else if (!tag.startsWith("?xml ", 1)){ // omit xml version declaration
				documentTokens.add(new DocumentToken(documentId, documentIndex, tag, TokenType.processinginstruction, -1, startoffset+start, startoffset+start+tag.length(), -1));
			}
			start += tag.length();
		}
		if (start<string.length()) {
			String text = string.substring(start, string.length());
			documentTokens.add(new DocumentToken(documentId, documentIndex, text, TokenType.other, -1, startoffset+start, startoffset+start+text.length(), -1)); // -1 position
		}
		return documentTokens;
	}

	public List<DocumentToken> getDocumentTokens() {
		return documentTokens;
	}

	public static class DocumentTokensConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return DocumentTokens.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			
			
			DocumentTokens documentTokens = (DocumentTokens) source;
			
			writer.startNode("total");
			writer.setValue(String.valueOf(documentTokens.total));
			writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "tokens", List.class);
	        for (DocumentToken documentToken :  documentTokens.getDocumentTokens()) {
		        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "token", String.class);
		        
		        context.convertAnother(documentToken);
		        
		        writer.endNode();
	        }
	        writer.endNode();

		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			return null;
		}

	}

}
