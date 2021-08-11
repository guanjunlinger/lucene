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
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class MyTopFieldCollector implements Collector {

    private int docBase;
    private PriorityQueue<FieldValueHitQueue.Entry> priorityQueue;
    private float maxScores = 0.0f;
    private SortField[] sortFields;
    FieldComparator[] fieldComparators;

    private int totalHits = 0;
    private int numHits;


    public MyTopFieldCollector(Sort sort, int numHits) {
        this.sortFields = sort.getSort();
        this.numHits = numHits;
        this.fieldComparators = new FieldComparator[sortFields.length];
        for (int i = 0; i < sortFields.length; i++) {
            this.fieldComparators[i] = sortFields[i].getComparator(numHits, i);
        }
        priorityQueue = new PriorityQueue<>(numHits, (a, b) -> {
            int numComparators = fieldComparators.length;
            for (int i = 0; i < numComparators; ++i) {
                final int c = fieldComparators[i].compare(a.slot, b.slot);
                if (c != 0) {
                    return c;
                }
            }
            return a.doc - b.doc;

        });
    }

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext leafReaderContext) throws IOException {
        this.docBase = leafReaderContext.docBase;
        LeafFieldComparator[] comparators = Arrays.stream(this.fieldComparators).map(fieldComparator -> {
            try {
                return fieldComparator.getLeafComparator(leafReaderContext);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList()).toArray(new LeafFieldComparator[0]);

        return new MultiComparatorLeafCollector(comparators) {


            @Override
            public void collect(int doc) throws IOException {
                int slot = totalHits++;
                this.comparator.copy(slot, doc);
                float score = scorer.score();
                maxScores = Math.max(score, maxScores);
                priorityQueue.add(new FieldValueHitQueue.Entry(slot, docBase + doc, score));
            }
        };
    }

    @Override
    public boolean needsScores() {
        return true;
    }

    public TopFieldDocs getTopFieldDocs() {
        FieldDoc[] fieldDocs = new FieldDoc[totalHits];
        int j = 0;
        for (FieldValueHitQueue.Entry entry : priorityQueue) {
            Object[] fields = new Object[sortFields.length];
            for (int i = 0; i < sortFields.length; i++) {
                fields[i] = fieldComparators[i].value(entry.slot);
            }
            fieldDocs[j++] = new FieldDoc(entry.doc, entry.score, fields);
        }
        return new TopFieldDocs(totalHits, fieldDocs, sortFields, maxScores);
    }


    private static abstract class MultiComparatorLeafCollector implements LeafCollector {
        LeafFieldComparator comparator;
        Scorer scorer;

        public MultiComparatorLeafCollector(LeafFieldComparator[] comparators) {
            comparator = new MultiLeafFieldComparator(comparators);
        }

        @Override
        public void setScorer(Scorer scorer) throws IOException {
            comparator.setScorer(scorer);
            this.scorer = scorer;
        }
    }

    private static class MultiLeafFieldComparator implements LeafFieldComparator {
        private LeafFieldComparator[] comparators;
        private LeafFieldComparator firstComparator;

        public MultiLeafFieldComparator(LeafFieldComparator[] comparators) {
            this.firstComparator = comparators[0];
            this.comparators = comparators;

        }

        @Override
        public void setBottom(int slot) throws IOException {
            for (LeafFieldComparator comparator : comparators) {
                comparator.setBottom(slot);
            }

        }

        @Override
        public int compareBottom(int doc) throws IOException {
            int cmp = firstComparator.compareBottom(doc);
            if (cmp != 0) {
                return cmp;
            }
            for (int i = 1; i < comparators.length; ++i) {
                cmp = comparators[i].compareBottom(doc);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }

        @Override
        public int compareTop(int doc) throws IOException {
            int cmp = firstComparator.compareTop(doc);
            if (cmp != 0) {
                return cmp;
            }
            for (int i = 1; i < comparators.length; ++i) {
                cmp = comparators[i].compareTop(doc);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }

        @Override
        public void copy(int slot, int doc) throws IOException {
            for (LeafFieldComparator comparator : comparators) {
                comparator.copy(slot, doc);
            }
        }


        @Override
        public void setScorer(Scorer scorer) throws IOException {
            for (LeafFieldComparator comparator : comparators) {
                comparator.setScorer(scorer);
            }
        }
    }


    public static void main(String[] args) throws IOException {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        Sort sort = new Sort();
        sort.setSort(new SortField("price", SortField.Type.FLOAT));
        MyTopFieldCollector myTopFieldCollector = new MyTopFieldCollector(sort, 10);
        indexSearcher.search(LongPoint.newRangeQuery("id", 5, 10), myTopFieldCollector);
        for (ScoreDoc scoreDoc : myTopFieldCollector.getTopFieldDocs().scoreDocs) {
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println(document.get("nickname"));
        }
    }
}
