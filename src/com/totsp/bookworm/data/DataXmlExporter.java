package com.totsp.bookworm.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.totsp.bookworm.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Android DataExporter that allows the passed in SQLiteDatabase 
 * to be exported to external storage (SD card) in an XML format.
 * 
 * To backup a SQLite database you need only copy the database file itself
 * (on Android /data/data/APP_PACKAGE/databases/DB_NAME.db) -- you *don't* need this
 * export to XML step.
 * 
 * XML export is useful so that the data can be more easily transformed into
 * other formats and imported/exported with other tools (not for backup per se).  
 * 
 * The kernel of inspiration for this came from: 
 * http://mgmblog.com/2009/02/06/export-an-android-sqlite-db-to-an-xml-file-on-the-sd-card/. 
 * (Though I have made many changes/updates here, I did initially start from that article.)
 * 
 * @author ccollins
 *
 */
public class DataXmlExporter {

   private static final String DATASUBDIRECTORY = "bookwormdata";

   private final SQLiteDatabase db;
   private XmlBuilder xmlBuilder;

   public DataXmlExporter(final SQLiteDatabase db) {
      this.db = db;
   }

   public void export(final String dbName, final String exportFileNamePrefix) throws IOException {
      Log.i(Constants.LOG_TAG, "exporting database - " + dbName + " exportFileNamePrefix=" + exportFileNamePrefix);

      xmlBuilder = new XmlBuilder();
      xmlBuilder.start(dbName);

      // get the tables
      String sql = "select * from sqlite_master";
      Cursor c = db.rawQuery(sql, new String[0]);
      if (c.moveToFirst()) {
         do {
            String tableName = c.getString(c.getColumnIndex("name"));

            // skip metadata, sequence, and uidx (unique indexes)
            if (!tableName.equals("android_metadata") && !tableName.equals("sqlite_sequence")
                     && !tableName.startsWith("uidx")) {
               exportTable(tableName);
            }
         } while (c.moveToNext());
      }
      String xmlString = xmlBuilder.end();
      writeToFile(xmlString, exportFileNamePrefix + ".xml");
      Log.i(Constants.LOG_TAG, "exporting database complete");
   }

   private void exportTable(final String tableName) throws IOException {
      xmlBuilder.openTable(tableName);
      String sql = "select * from " + tableName;
      Cursor c = db.rawQuery(sql, new String[0]);
      if (c.moveToFirst()) {
         int cols = c.getColumnCount();
         do {
            xmlBuilder.openRow();
            for (int i = 0; i < cols; i++) {
               xmlBuilder.addColumn(c.getColumnName(i), c.getString(i));
            }
            xmlBuilder.closeRow();
         } while (c.moveToNext());
      }
      c.close();
      xmlBuilder.closeTable();
   }

   private void writeToFile(final String xmlString, final String exportFileName) throws IOException {
      File dir = new File(Environment.getExternalStorageDirectory(), DataXmlExporter.DATASUBDIRECTORY);
      if (!dir.exists()) {
         dir.mkdirs();
      }
      File file = new File(dir, exportFileName);
      file.createNewFile();

      ByteBuffer buff = ByteBuffer.wrap(xmlString.getBytes());
      FileChannel channel = new FileOutputStream(file).getChannel();
      try {
         channel.write(buff);
      } finally {
         if (channel != null) {
            channel.close();
         }
      }
   }

   /**
    * XmlBuilder is used to write XML tags (open and close, and a few attributes)
    * to a StringBuilder. Here we have nothing to do with IO or SQL, just a fancy StringBuilder. 
    * 
    * @author ccollins
    *
    */
   static class XmlBuilder {
      private static final String OPEN_XML_STANZA = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
      private static final String CLOSE_WITH_TICK = "'>";
      private static final String DB_OPEN = "<database name='";
      private static final String DB_CLOSE = "</database>";
      private static final String TABLE_OPEN = "<table name='";
      private static final String TABLE_CLOSE = "</table>";
      private static final String ROW_OPEN = "<row>";
      private static final String ROW_CLOSE = "</row>";
      private static final String COL_OPEN = "<col name='";
      private static final String COL_CLOSE = "</col>";

      private final StringBuilder sb;

      public XmlBuilder() throws IOException {
         sb = new StringBuilder();
      }

      void start(final String dbName) {
         sb.append(XmlBuilder.OPEN_XML_STANZA);
         sb.append(XmlBuilder.DB_OPEN + dbName + XmlBuilder.CLOSE_WITH_TICK);
      }

      String end() throws IOException {
         sb.append(XmlBuilder.DB_CLOSE);
         return sb.toString();
      }

      void openTable(final String tableName) {
         sb.append(XmlBuilder.TABLE_OPEN + tableName + XmlBuilder.CLOSE_WITH_TICK);
      }

      void closeTable() {
         sb.append(XmlBuilder.TABLE_CLOSE);
      }

      void openRow() {
         sb.append(XmlBuilder.ROW_OPEN);
      }

      void closeRow() {
         sb.append(XmlBuilder.ROW_CLOSE);
      }

      void addColumn(final String name, final String val) throws IOException {
         sb.append(XmlBuilder.COL_OPEN + name + XmlBuilder.CLOSE_WITH_TICK + val + XmlBuilder.COL_CLOSE);
      }
   }

}
