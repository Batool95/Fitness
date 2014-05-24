package com.nikhu.fitness.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Date;

/**
 * Created by bujji on 09-05-2014.
 */
public class WorkoutDBAdapter {

    public static final String TAG = "WorkoutDBAdapter";

    private WorkoutHelper workoutHelper;

    public WorkoutDBAdapter(Context context) {
        workoutHelper = new WorkoutHelper(context);
    }

    public long saveCurrentSessionSteps(Date sessionDate, int steps, float calories, float distance) {
        SQLiteDatabase db = workoutHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutHelper.SESSION_DATE, sessionDate.getTime());
        contentValues.put(WorkoutHelper.STEPS, steps);
        contentValues.put(WorkoutHelper.CALORIES, calories);
        contentValues.put(WorkoutHelper.DISTANCE, distance);
        long id = db.insert(WorkoutHelper.STEPS_TB_NAME, null, contentValues);
        Log.i(TAG, "Saved session steps information to database (" + id + ")");
        return id;
    }

    public Cursor getStepsData() {
        SQLiteDatabase db = workoutHelper.getWritableDatabase();
        String[] columns = {WorkoutHelper.UID, WorkoutHelper.SESSION_DATE, WorkoutHelper.STEPS, WorkoutHelper.CALORIES, workoutHelper.DISTANCE};
        Cursor cursor = db.query(WorkoutHelper.STEPS_TB_NAME, columns, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    static class WorkoutHelper extends SQLiteOpenHelper {

        private static final String DB_NAME = "workoutdb";
        private static final String STEPS_TB_NAME = "steps";
        private static final int DB_VERSION = 1;
        private static final String UID = "_id";
        private static final String SESSION_DATE = "workout_date";
        public static final String STEPS = "steps";
        public static final String SPEED = "speed";
        public static final String CALORIES = "calories";
        public static final String DISTANCE = "distance";

        private static final String CREATE_STEPS_TABLE = "CREATE TABLE " + STEPS_TB_NAME
                + "(" + UID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + SESSION_DATE + " INTEGER NOT NULL, " + STEPS + " INTEGER NOT NULL, "
                + CALORIES + " REAL, " + DISTANCE + " REAL)";

        private static final String DROP_STEPS_TABLE = "DROP TABLE IF EXISTS " + STEPS_TB_NAME;

        private Context context;

        public WorkoutHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            this.context = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_STEPS_TABLE);
            Log.i(TAG, CREATE_STEPS_TABLE + " table created.");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(DROP_STEPS_TABLE);
            Log.i(TAG, "Dropped " + STEPS_TB_NAME + "table.");
            onCreate(db);
        }
    }
}
