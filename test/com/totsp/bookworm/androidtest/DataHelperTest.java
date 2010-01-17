package com.totsp.bookworm.androidtest;

import android.test.AndroidTestCase;

import com.totsp.bookworm.data.DataHelper;
import com.totsp.bookworm.model.Book;

import java.util.Date;

public class DataHelperTest extends AndroidTestCase {

   public void testInsertBook() {
      DataHelper dataHelper = new DataHelper(this.getContext());
      Book b1 = new Book(0, "1231", "title1", 0, new Date());
      dataHelper.addBook(b1);
      
   }
   
}
