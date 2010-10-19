package com.totsp.bookworm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.StringUtil;

import java.util.ArrayList;

/**
 * Simple Book adapter that displays two TextViews when building views. 
 * This is NOT used by the "Main" screen (which is a more complicated adapter)
 * but is used in other simplified areas (such as search and CSV import).
 * 
 * @author ccollins
 *
 */
public class BookListAdapter extends ArrayAdapter<Book> {

   // static and package access as an Android optimization (used in inner class)
   static class ViewHolder {
      TextView text1;
      TextView text2;
   }

   private LayoutInflater layoutInflater;

   BookListAdapter(final Context context, final ArrayList<Book> books) {
      super(context, R.layout.book_simple_list_item, books);
      this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
   }

   @Override
   public View getView(int position, View convertView, ViewGroup parent) {

      View item = convertView;
      ViewHolder holder = null;

      if (item == null) {
         item = layoutInflater.inflate(R.layout.book_simple_list_item, parent, false);
         // use ViewHolder pattern to avoid extra trips to findViewById         
         holder = new ViewHolder();
         holder.text1 = (TextView) item.findViewById(R.id.book_item_text_1);
         holder.text2 = (TextView) item.findViewById(R.id.book_item_text_2);
         item.setTag(holder);
      }
      
      Book book = getItem(position);

      holder = (ViewHolder) item.getTag();
      holder.text1.setText(book.title);
      holder.text2.setText(StringUtil.contractAuthors(book.authors));
      return item;
   }
}
