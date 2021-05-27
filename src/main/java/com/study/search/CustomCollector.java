package com.study.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CustomCollector extends SimpleCollector {
    private int docBase;
    private Scorer scorer;
    private List<ScoreDoc> list = new ArrayList<>(16);

    @Override
    public void collect(int i) throws IOException {
        list.add(new ScoreDoc(docBase + i, scorer.score()));
    }

    @Override
    public void setScorer(Scorer scorer) {
        this.scorer = scorer;
    }

    @Override
    protected void doSetNextReader(LeafReaderContext context) {
        this.docBase = context.docBase;
    }

    @Override
    public boolean needsScores() {
        return true;
    }

    public TopFieldDocs getTopDocs() {
        return new TopFieldDocs(list.size(), list.toArray(new ScoreDoc[0]), null, 1.0f);
    }

    public static void main(String[] args) throws IOException {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        CustomCollector customCollector = new CustomCollector();
        indexSearcher.search(LongPoint.newExactQuery("id",1234),customCollector);
        for(ScoreDoc scoreDoc :customCollector.getTopDocs().scoreDocs){
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println(document.get("nickname"));
        }
    }
}
