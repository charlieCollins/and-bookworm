package com.totsp.bookworm.model;

/**
 * Item group data model.
 * Used to collect items in an ordered group (eg. a group of books).
 * @author Jason Liick
 */
public final class Group {

	// NOTE - no accessors/mutators by design, Android optimization
	public long id;
	public String name;
	public String description;


	 public Group() {
	 }

	 public Group(final String name) {
		 id = 0L;
		 this.name = name;
	 }

	 @Override
	 public String toString() {
		 StringBuilder sb = new StringBuilder();
		 sb.append("Group-");
		 sb.append(" name:" + name);
		 return sb.toString();
	 }

	 @Override
	 public boolean equals(final Object obj) {
		 if (obj == this) {
			 return true;
		 }
		 if (obj instanceof Group) {
			 Group lhs = (Group) obj;
			 if ((lhs.id == id) && (lhs.name.equals(this.name))) {
				 return true;
			 }
		 }
		 return false;
	 }

	 @Override
	 public int hashCode() {
		 int result = 31;
		 result += id;
	     if (name != null) {
	    	 result += name.hashCode();
	     }
	     return result;
	 }

}
