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

public class MyTopFieldCollector extends SimpleCollector {

    private int docBase;
    private Scorer scorer;
    private List<FieldDoc> list = new ArrayList<>(16);
    private float maxScores = 0.0f;
    private SortField[] sortFields;
    private MultiLeafFieldComparator multiLeafFieldComparator;

    int totalHits;

    public MyTopFieldCollector(Sort sort, int totalHits) {
        this.sortFields = sort.getSort();
        this.totalHits = totalHits;
    }

    @Override
    public void collect(int i) throws IOException {
        totalHits++;
        list.add(new FieldDoc(docBase + i, scorer.score()));
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

    private static class MultiLeafFieldComparator implements LeafFieldComparator {
        private LeafFieldComparator comparator;
        private Scorer scorer;
        public MultiLeafFieldComparator(LeafFieldComparator[] comparators) {


        }

        @Override
        public void setBottom(int i) throws IOException {

        }

        @Override
        public int compareBottom(int i) throws IOException {
            return 0;
        }

        @Override
        public int compareTop(int i) throws IOException {
            return 0;
        }

        @Override
        public void copy(int i, int i1) throws IOException {

        }

        @Override
        public void setScorer(Scorer scorer) throws IOException {

        }
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
