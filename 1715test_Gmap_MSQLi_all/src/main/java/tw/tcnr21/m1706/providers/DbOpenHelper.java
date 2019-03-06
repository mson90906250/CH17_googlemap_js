package tw.tcnr21.m1706.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/*** Created by vpoint88 on 2017/7/20.*/

public class DbOpenHelper extends SQLiteOpenHelper {

	public DbOpenHelper(Context context, String name,
						CursorFactory factory, int version) {
		super(context, name, factory, version);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {


	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


	}

}