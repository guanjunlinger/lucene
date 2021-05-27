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
        Document document = new Document();
        document.add(new TextField("name", "张三", Field.Store.NO));
        document.add(new LongPoint("id", 1234));
        document.add(new StringField("nickname", "冷太阳", Field.Store.YES));
        indexWriter.addDocument(document);
        indexWriter.close();
    }


}
