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

// the file handling stuff here certainly isn't the best
// but files are small and this is rarely used, so not a bottleneck anyway
public class CsvManager {

   public static final String EXPORT_FILENAME = "bookworm.csv";

   public CsvManager() {
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
            File file = new File(DataConstants.EXTERNAL_DATA_PATH + File.separator + EXPORT_FILENAME);
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
      sb
               .append("Title,Subtitle,Authors(pipe|separated),ISBN10,ISBN13,Description,Format,Subject,Publisher,Published Date,User Rating,User Read Status\n");
      if (books != null && !books.isEmpty()) {
         for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            sb.append(cleanString(b.title) + ",");
            sb.append(b.subTitle != null ? cleanString(b.subTitle) + "," : ",");
            if (b.authors != null && !b.authors.isEmpty()) {
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
               sb.append(b.bookUserData.read);
            } else {
               sb.append(" , ");
            }
            sb.append("\n");
         }
      }
      return sb.toString();
   }

   // this is VERY brittle and primitive, but small -- only supports specific BookWorm format files
   public ArrayList<Book> parseCSVFile(File f) {
      // TODO protect against SQL injection attacks? risk very minor, it's your own DB, but still
      ArrayList<Book> books = new ArrayList<Book>();
      if (f.exists() && f.canRead()) {
         Log.i(Constants.LOG_TAG, "Parsing file:" + f.getAbsolutePath() + " for import into BookWorm database.");
         // "Title,Subtitle,Authors(pipe|separated),ISBN10,ISBN13,Description,Format,
         //    Subject,Publisher,Published Date,User Rating,User Read Status\n"
         Scanner scanner = null;
         int count = 0;
         try {
            scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
               count++;
               String line = scanner.nextLine();
               System.out.println("*** LINE - " + line);
               if (line != null && count > 1) {
                  String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                  if (parts != null && (parts.length == 12 || parts.length == 13)) {
                     Book b = new Book();
                     b.title = parts[0];
                     b.subTitle = parts[1];
                     if (parts[2] != null) {
                        String authors = parts[2].replace('|', '"');
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

                     BookUserData bud = new BookUserData(0L, rating, readStatus == 1 ? true : false, null);
                     b.bookUserData = bud;
                     books.add(b);
                  } else {
                     Log.w(Constants.LOG_TAG, "Warning, not including line " + count
                              + " from import file because it does not parse into 12 or 13 parts (parsed as " + parts.length
                              + ").");
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
      return books;
   }

}
