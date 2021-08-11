package com.study.sort;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


import java.io.IOException;
import java.nio.file.Paths;


public class FloatValuesComparatorSource extends FieldComparatorSource {

    @Override
    public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
        return new MyFloatComparator(numHits, fieldname, 0.0f);
    }


    static class MyFloatComparator extends FieldComparator.NumericComparator<Float> {

        private float[] values;
        private float bottom;



        private float topValue;

        public MyFloatComparator(int numHits, String field, Float missingValue) {
            super(field, missingValue);
            values = new float[numHits];
        }

        @Override
        public int compare(int slot1, int slot2) {
            return Float.compare(values[slot1], values[slot2]);
        }

        @Override
        public void setTopValue(Float value) {
            topValue = value;
        }

        @Override
        public Float value(int slot) {
            return Float.valueOf(values[slot]);
        }

        @Override
        public void setBottom(int bottom) throws IOException {
            this.bottom = values[bottom];
        }

        private float getValueForDoc(int doc) throws IOException {
            if (currentReaderValues.advanceExact(doc)) {
                return Float.intBitsToFloat((int) currentReaderValues.longValue());
            } else {
                return missingValue;
            }
        }

        @Override
        public int compareBottom(int doc) throws IOException {
            return Float.compare(bottom, getValueForDoc(doc));
        }

        @Override
        public int compareTop(int doc) throws IOException {
            return Float.compare(topValue, getValueForDoc(doc));
        }

        @Override
        public void copy(int slot, int doc) throws IOException {
            values[slot] = getValueForDoc(doc);
        }
    }


    public static void main(String[] args) throws IOException {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Sort sort = new Sort();
        sort.setSort(new SortField("price", new FloatValuesComparatorSource()));
        TopDocs topDocs = indexSearcher.search(FloatPoint.newRangeQuery("price", 50f, 400f), 10, sort);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = indexSearcher.doc(scoreDoc.doc);
            System.out.println(document.get("nickname"));
        }
    }

}
