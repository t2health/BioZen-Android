/*****************************************************************
BioZen

Copyright (C) 2011 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.compassionDB;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.t2.compassionMeditation.Global;
import com.t2.compassionMeditation.PreferenceData;

/**
 * Database helper which creates and upgrades the database and provides the DAOs for the app.
 * 
 * @author kevingalligan
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

	/************************************************
	 * Suggested Copy/Paste code. Everything from here to the done block.
	 ************************************************/


	private Dao<BioUser, Integer> bioUserDao = null;
	private Dao<BioSession, Integer> bioSessionDao = null;
	private Dao<PreferenceData, Integer> preferenceDao = null;
	

	public DatabaseHelper(Context context) {
		super(context, Global.Database.name, null, Global.Database.version);
	}

	/************************************************
	 * Suggested Copy/Paste Done
	 ************************************************/

	@Override
	public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
		try {
			TableUtils.createTable(connectionSource, BioUser.class);
			TableUtils.createTable(connectionSource, BioSession.class);
			TableUtils.createTable(connectionSource, PreferenceData.class);
			
		} catch (SQLException e) {
			Log.e(DatabaseHelper.class.getName(), "Unable to create datbases", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource, int oldVer, int newVer) {
		try {
			TableUtils.dropTable(connectionSource, BioUser.class, true);
			TableUtils.dropTable(connectionSource, BioSession.class, true);
			TableUtils.dropTable(connectionSource, PreferenceData.class, true);
			onCreate(sqliteDatabase, connectionSource);
		} catch (SQLException e) {
			Log.e(DatabaseHelper.class.getName(), "Unable to upgrade database from version " + oldVer + " to new "
					+ newVer, e);
		}
	}

	public Dao<BioUser, Integer> getBioUserDao() throws SQLException {
		if (bioUserDao == null) {
			bioUserDao = getDao(BioUser.class);
		}
		return bioUserDao;
	}
	public Dao<BioSession, Integer> getBioSessionDao() throws SQLException {
		if (bioSessionDao == null) {
			bioSessionDao = getDao(BioSession.class);
		}
		return bioSessionDao;
	}
	public Dao<PreferenceData, Integer> getPreferenceDao() throws SQLException {
		if (preferenceDao == null) {
			preferenceDao = getDao(PreferenceData.class);
		}
		return preferenceDao;
	}
	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
		bioUserDao = null;
		bioSessionDao = null;
		preferenceDao = null;
	}	
	
}
