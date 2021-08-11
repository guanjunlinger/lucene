package com.study.collector;

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

public class MyTopScoreCollector extends SimpleCollector {
    private int docBase;
    private Scorer scorer;
    private List<ScoreDoc> list = new ArrayList<>(16);
    private float maxScores = 0.0f;

    @Override
    public void collect(int i) throws IOException {
        list.add(new ScoreDoc(docBase + i, scorer.score()));
        maxScores = Math.max(maxScores, scorer.score());
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

    public TopDocs getTopDocs() {
        return new TopDocs(list.size(), list.toArray(new ScoreDoc[0]), maxScores);
    }

    public static void main(String[] args) throws IOException {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        MyTopScoreCollector myTopScoreCollector = new MyTopScoreCollector();
        indexSearcher.search(LongPoint.newExactQuery("id", 1), myTopScoreCollector);
        for (ScoreDoc scoreDoc : myTopScoreCollector.getTopDocs().scoreDocs) {
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println(document.get("nickname"));
        }
    }
}
