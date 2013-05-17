

package vn.com.mobifone.mtracker.common;

import android.content.ContentValues;
import android.location.Location;

public interface IFileLogger
{

    void Write(Location loc) throws Exception;

    void Annotate(String description, Location loc) throws Exception;

    String getName();

	//void Write(Location loc, ContentValues cv) throws Exception;

}
