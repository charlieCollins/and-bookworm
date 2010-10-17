package com.totsp.bookworm.data;

import android.util.Log;

import com.totsp.bookworm.Constants;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.model.BookUserData;
import com.totsp.bookworm.util.DateUtil;
import com.totsp.bookworm.util.ExternalStorageUtil;
import com.totsp.bookworm.util.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class CsvManager {

   public static final String EXPORT_FILENAME = "bookworm.csv";

   private BookDataSource bookDataSource;

   public CsvManager(final BookDataSource bookDataSource) {
      this.bookDataSource = bookDataSource;
   }

   public void export(final ArrayList<Book> books) {
      String csv = this.getCSVString(books);
      if (saveCSVStringAsFile(csv)) {
         return;
      } else {
         throw new RuntimeException("Error, unable to save data contents as CSV file.");
      }
   }

   private boolean saveCSVStringAsFile(final String csv) {
      if (ExternalStorageUtil.isExternalStorageAvail()) {
         try {
            File file = new File(DataConstants.EXTERNAL_DATA_PATH + File.separator + CsvManager.EXPORT_FILENAME);
            file.createNewFile(); // ok if returns false, overwrite         
            FileWriter out = new FileWriter(file);
            out.write(csv);
            out.close();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
         return true;
      } else {
         return false;
      }
   }

   private String cleanString(String in) {
      String result = in;
      in = in.replaceAll("\"", "");
      if (in.contains(",") || in.contains("\n")) {
         result = "\"" + in + "\"";
      }
      return result;
   }

   private String getCSVString(final ArrayList<Book> books) {
      StringBuilder sb = new StringBuilder();
      sb.append("Title,Subtitle,Authors(pipe|separated),ISBN10,ISBN13,Description,");
      sb.append("Format,Subject,Publisher,Published Date,User Rating,User Read Status, User Note [optional]\n");
      if ((books != null) && !books.isEmpty()) {
         for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            sb.append(cleanString(b.title) + ",");
            sb.append(b.subTitle != null ? cleanString(b.subTitle) + "," : ",");
            if ((b.authors != null) && !b.authors.isEmpty()) {
               for (int j = 0; j < b.authors.size(); j++) {
                  Author a = b.authors.get(j);
                  if (j == 0) {
                     sb.append(cleanString(a.name));
                  } else {
                     sb.append("|" + cleanString(a.name));
                  }
               }
               sb.append(",");
            } else {
               sb.append(",");
            }
            sb.append(b.isbn10 != null ? cleanString(b.isbn10) + "," : ",");
            sb.append(b.isbn13 != null ? cleanString(b.isbn13) + "," : ",");
            sb.append(b.description != null ? cleanString(b.description) + "," : ",");
            sb.append(b.format != null ? cleanString(b.format) + "," : ",");
            sb.append(b.subject != null ? cleanString(b.subject) + "," : ",");
            sb.append(b.publisher != null ? cleanString(b.publisher) + "," : ",");
            sb.append(DateUtil.format(new Date(b.datePubStamp)) + ",");
            if (b.bookUserData != null) {
               sb.append(b.bookUserData.rating + ",");
               sb.append(b.bookUserData.read + ",");
               sb.append(b.bookUserData.blurb != null ? b.bookUserData.blurb : "");
            } else {
               sb.append(" , ,");
            }
            sb.append("\n");
         }
      }
      return sb.toString();
   }

   public ArrayList<Book> parseCSVFile_ISBNOnly(File f) {
      ArrayList<Book> books = new ArrayList<Book>();

      return books;
   }

   // this is VERY brittle and primitive, but small -- only supports specific BookWorm format files
   public ArrayList<Book> parseCSVFile(File f) {
      // TODO protect against SQL injection attacks? risk very minor, it's your own DB, but still
      ArrayList<Book> books = new ArrayList<Book>();
      if (f.exists() && f.canRead()) {
         Log.i(Constants.LOG_TAG, "Parsing file:" + f.getAbsolutePath() + " for import into BookWorm database.");
         // "Title,Subtitle,Authors(pipe|separated),ISBN10,ISBN13,Description,Format,
         //    Subject,Publisher,Published Date,User Rating,User Read Status, User Note\n"
         Scanner scanner = null;
         int count = 0;
         try {
            scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
               count++;
               String line = scanner.nextLine();
               
               Log.i(Constants.LOG_TAG, "Processing line for import:" + line);
               
               if ((line != null) && (count > 1)) {
                  String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                  // FULL type CSV file for import
                  if ((parts != null) && ((parts.length == 12) || (parts.length == 13))) {
                     Book b = new Book();
                     b.title = parts[0];
                     b.subTitle = parts[1];
                     if (parts[2] != null) {
                        String authors = parts[2].replace('|', ',');
                        System.out.println("Authors before expanded - " + authors);
                        b.authors = StringUtil.expandAuthors(authors);
                     }
                     b.isbn10 = parts[3];
                     b.isbn13 = parts[4];
                     b.description = parts[5];
                     b.format = parts[6];
                     b.subject = parts[7];
                     b.publisher = parts[8];
                     Date date = DateUtil.parse(parts[9]);
                     if (date != null) {
                        b.datePubStamp = date.getTime();
                     }

                     long rating = 0;
                     if (parts[10] != null) {
                        try {
                           rating = Integer.valueOf(parts[10]);
                        } catch (NumberFormatException e) {
                           // ignore
                        }
                     }
                     long readStatus = 0;
                     if (parts[11] != null) {
                        readStatus = Boolean.valueOf(parts[11]) ? 1 : 0;
                     }

                     String note = null;
                     if (parts.length > 12) {
                        note = parts[12];
                     }

                     BookUserData bud = new BookUserData(0L, rating, readStatus == 1 ? true : false, note);
                     b.bookUserData = bud;
                     if (b.title != null) {
                        books.add(b);
                     }
                  } else if ((parts != null) && (parts.length == 1)) {
                     // ISBN ONLY type, 1 element per file line 
                     if (bookDataSource != null) {                        
                        ArrayList<Book> searchBooks = bookDataSource.getBooks(parts[0], 0);
                        if (searchBooks != null && !searchBooks.isEmpty()) {
                           books.add(searchBooks.get(0));
                        }                        
                     } else {
                        Log.w(Constants.LOG_TAG,
                                 "BookDataSource null, not importing book from CSV based on ISBN alone.");
                     }
                  } else {
                     Log.w(Constants.LOG_TAG, "Warning, not including line " + count
                              + " from import file because it does not parse into correct number of parts,"
                              + " 1 (ISBN only), or 13 (full BookWorm format). (Parsed as " + parts.length + ").");
                  }
               }
            }
         } catch (FileNotFoundException e) {

         } finally {
            if (scanner != null) {
               scanner.close();
            }
         }
      }
      Log.i(Constants.LOG_TAG, "Parsed " + books.size() + " books from CSV file.");
      return books;
   }
}