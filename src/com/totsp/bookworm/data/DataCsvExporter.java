package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;
import com.totsp.bookworm.util.ExternalStorageUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

// the file handling stuff here certainly isn't the best
// but files are small and this is rarely used, so not a bottleneck anyway
public class DataCsvExporter {

   public static final String EXPORT_FILENAME = "bookworm.csv";

   public DataCsvExporter() {
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
            sb.append(DateUtil.parse(String.valueOf(b.datePubStamp)) + ",");
            if (b.bookUserData != null) {
               sb.append(b.bookUserData.rating + ",");
               sb.append(b.bookUserData.read + ",");
            } else {
               sb.append(", , ");
            }
            sb.append("\n");
         }
      }
      return sb.toString();
   }

}
