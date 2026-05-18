package com.example.mobileproject.data

/**
 * 여행 기록 데이터를 표현하는 데이터 클래스
 * * @property no SQLite의 PRIMARY KEY로 자동 증가하는 고유 번호 (새 기록 추가 시에는 null 허용)
 * @property place 사용자가 입력한 여행지명
 * @property visitDate 방문 날짜 (형식: YYYY-MM-DD)
 * @property memo 여행 관련 메모 및 특징
 * @property photoUri 카메라 촬영 또는 갤러리에서 선택한 사진의 기기 내 내부 경로/URI
 * @property hashtag 온디바이스 AI 모델이 사진을 분석하여 자동으로 생성한 음식/테마 해시태그
 */
data class TravelRecord(
    val no: Int? = null,
    val place: String,
    val visitDate: String,
    val memo: String,
    val photoUri: String,
    val hashtag: String // AI가 추론한 해시태그 결과를 저장하기 위해 확장한 컬럼 데이터
)