package com.totsp.bookworm.model;

/**
 * Markup tag class.
 * Provides a simple text tag that can be applied to  multiple items.
 * @author Jason Liick
 */
public final class Tag {

	// NOTE - no accessors/mutators by design, Android optimization
	public long id;
	public String text;


	 public Tag() {
	 }

	 public Tag(final String name) {
		 id = 0L;
		 this.text = name;
	 }

	 @Override
	 public String toString() {
		 StringBuilder sb = new StringBuilder();
		 sb.append("Tag-");
		 sb.append(" name:" + text);
		 return sb.toString();
	 }

	 @Override
	 public boolean equals(final Object obj) {
		 if (obj == this) {
			 return true;
		 }
		 if (obj instanceof Tag) {
			 Tag lhs = (Tag) obj;
			 if ((lhs.id == id) && (lhs.text.equals(this.text))) {
				 return true;
			 }
		 }
		 return false;
	 }

	 @Override
	 public int hashCode() {
		 int result = 31;
		 result += id;
	     if (text != null) {
	    	 result += text.hashCode();
	     }
	     return result;
	 }

}
