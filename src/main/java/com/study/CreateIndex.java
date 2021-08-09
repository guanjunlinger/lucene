package com.study;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class CreateIndex {

    public static void main(String[] args) throws IOException {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig());

        for (int i = 0; i < 10; i++) {
            Document document = new Document();
            document.add(new TextField("name", "张三" + i, Field.Store.NO));
            document.add(new LongPoint("id", i));
            document.add(new StringField("nickname", "冷太阳" + i, Field.Store.YES));
            document.add(new FloatDocValuesField("price", 50.1f * i));
            document.add(new FloatPoint("price", 50.1f * i));
            indexWriter.addDocument(document);
        }
        indexWriter.close();
    }


}
