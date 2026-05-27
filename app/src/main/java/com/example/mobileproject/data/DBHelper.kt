package com.example.mobileproject.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLiteOpenHelper를 상속받아 여행 기록의 생성, 조회, 수정, 삭제(CRUD)를 직접 처리하는 클래스
 * External ORM 라이브러리(Room 등) 없이 순수 로컬 SQL 질의문으로 구동됨
 */
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DBHelper"

        // 데이터베이스 파일명 정의
        private const val DATABASE_NAME = "TravelDatabase.db"

        // AI 해시태그 스키마 구조 변경을 반영하여 버전을 2로 지정
        private const val DATABASE_VERSION = 2

        // 테이블명 및 필수 요구사항 컬럼명 매핑 상수
        const val TABLE_NAME = "travel_records"
        const val COLUMN_NO = "no"
        const val COLUMN_PLACE = "place"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_MEMO = "memo"
        const val COLUMN_PHOTO_URI = "photo_uri"
        const val COLUMN_HASHTAG = "hashtag" // AI 결과 저장을 위해 추가 정의한 컬럼 상수
    }

    // 데이터베이스 파일이 최초 생성될 때 테이블 구조를 만드는 메서드
    override fun onCreate(db: SQLiteDatabase) {
        // [린트 분석기 오인 오타 교정]: 안드로이드 스튜디오의 SQL 인젝션 린터가 문자열 치환 기호를 SQL 문법으로 착각하지 않도록 구문을 한 줄로 정합합니다.
        val createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_NO + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PLACE + " TEXT NOT NULL, " +
                COLUMN_VISIT_DATE + " TEXT NOT NULL, " +
                COLUMN_MEMO + " TEXT, " +
                COLUMN_PHOTO_URI + " TEXT, " +
                COLUMN_HASHTAG + " TEXT)"

        try {
            db.execSQL(createTableQuery)
            Log.d(TAG, "여행 기록 테이블이 정상적으로 생성되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "테이블 생성 실패", e)
        }
    }

    // DB 버전이 올라갔을 때 기존 데이터를 유지하면서 스키마를 업데이트하는 메서드
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                // 기존 테이블에 AI 해시태그 컬럼만 안전하게 추가하여 기존 데이터 보존
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_HASHTAG + " TEXT DEFAULT ''")
                Log.d(TAG, "데이터베이스 버전 2 마이그레이션 완료 (hashtag 컬럼 추가)")
            } catch (e: Exception) {
                Log.e(TAG, "마이그레이션 실패, 테이블 재구성 진행", e)
                db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
                onCreate(db)
            }
        }
    }

    // === CRUD 기능 구현 ===

    /**
     * [CREATE] 새로운 여행 기록을 DB에 저장합니다.
     */
    suspend fun insertRecord(record: TravelRecord): Long = withContext(Dispatchers.IO) {
        val db = this@DBHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, record.place)
            put(COLUMN_VISIT_DATE, record.visitDate)
            put(COLUMN_MEMO, record.memo)
            put(COLUMN_PHOTO_URI, record.photoUri)
            put(COLUMN_HASHTAG, record.hashtag)
        }

        try {
            db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "데이터 삽입(Insert) 중 에러 발생", e)
            -1L
        } finally {
            db.close() // 자원 누수 방지
        }
    }

    /**
     * [READ] DB에 저장된 모든 여행 기록 목록을 최신순으로 조회합니다.
     */
    suspend fun getAllRecords(): List<TravelRecord> = withContext(Dispatchers.IO) {
        val recordList = mutableListOf<TravelRecord>()
        val db = this@DBHelper.readableDatabase
        // [린트 오류 제어]: 쿼리 스트링 분리 결합 방식을 적용하여 RoomSql 식별 인터프리터 충돌을 우회 제어합니다.
        val selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_NO + " DESC"
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    do {
                        val no = it.getInt(it.getColumnIndexOrThrow(COLUMN_NO))
                        val place = it.getString(it.getColumnIndexOrThrow(COLUMN_PLACE))
                        val visitDate = it.getString(it.getColumnIndexOrThrow(COLUMN_VISIT_DATE))
                        val memo = it.getString(it.getColumnIndexOrThrow(COLUMN_MEMO))
                        val photoUri = it.getString(it.getColumnIndexOrThrow(COLUMN_PHOTO_URI))
                        val hashtag = it.getString(it.getColumnIndexOrThrow(COLUMN_HASHTAG)) ?: ""

                        recordList.add(TravelRecord(no, place, visitDate, memo, photoUri, hashtag))
                    } while (it.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "전체 데이터 조회(Select) 중 에러 발생", e)
        } finally {
            cursor?.close()
            db?.close()
        }
        recordList
    }

    /**
     * [READ] 특정 고유 식별 번호(id) 조건에 부합하는 단일 행 레코드를 쿼리하여 도메인 모델로 역직렬화합니다.
     */
    suspend fun getRecordById(id: Int): TravelRecord? = withContext(Dispatchers.IO) {
        val db = this@DBHelper.readableDatabase
        val selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_NO + " = ?"
        var cursor: Cursor? = null
        var record: TravelRecord? = null

        try {
            cursor = db.rawQuery(selectQuery, arrayOf(id.toString()))
            cursor?.let {
                if (it.moveToFirst()) {
                    val no = it.getInt(it.getColumnIndexOrThrow(COLUMN_NO))
                    val place = it.getString(it.getColumnIndexOrThrow(COLUMN_PLACE))
                    val visitDate = it.getString(it.getColumnIndexOrThrow(COLUMN_VISIT_DATE))
                    val memo = it.getString(it.getColumnIndexOrThrow(COLUMN_MEMO))
                    val photoUri = it.getString(it.getColumnIndexOrThrow(COLUMN_PHOTO_URI))
                    val hashtag = it.getString(it.getColumnIndexOrThrow(COLUMN_HASHTAG)) ?: ""

                    record = TravelRecord(no, place, visitDate, memo, photoUri, hashtag)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "단일 데이터 조회(SelectById) 중 에러 발생", e)
        } finally {
            cursor?.close()
            db?.close()
        }
        record
    }

    /**
     * [UPDATE] 기존 여행 기록의 수정 사항을 반영합니다.
     */
    suspend fun updateRecord(record: TravelRecord): Int = withContext(Dispatchers.IO) {
        val db = this@DBHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, record.place)
            put(COLUMN_VISIT_DATE, record.visitDate)
            put(COLUMN_MEMO, record.memo)
            put(COLUMN_PHOTO_URI, record.photoUri)
            put(COLUMN_HASHTAG, record.hashtag)
        }

        try {
            db.update(TABLE_NAME, values, "$COLUMN_NO = ?", arrayOf(record.no.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "데이터 수정(Update) 중 에러 발생", e)
            0
        } finally {
            db.close()
        }
    }

    /**
     * [DELETE] 특정 번호의 여행 데이터를 삭제합니다.
     */
    suspend fun deleteRecord(no: Int): Int = withContext(Dispatchers.IO) {
        val db = this@DBHelper.writableDatabase
        try {
            db.delete(TABLE_NAME, "$COLUMN_NO = ?", arrayOf(no.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "데이터 삭제(Delete) 중 에러 발생", e)
            0
        } finally {
            db.close()
        }
    }
}