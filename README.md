# 🗺️ AI 로컬 여행 일기 (AI Travel Diary Application)

> **Android Component Architecture & On-Device Vision AI Integration Project**
> 
> 본 프로젝트는 안드로이드의 컴포넌트 라이프사이클 프레임워크와 순수 SQLite 데이터베이스 인프라를 기반으로 구축되었습니다. 온디바이스(On-Device) 비전 인공지능(TFLite) 추론 파이프라인과 구글 맵(Google Maps SDK) 지리 정보 시각화를 유기적으로 결합하여, 사용자가 로컬 환경에서 데이터를 영속화하고 맞춤형 인사이트를 도출할 수 있도록 돕는 아키텍처 지향적 여행 기록 플랫폼입니다.

---

## 📌 1. 시스템 요구사항 충족 및 아키텍처 매핑 명세 (Requirements Traceability Matrix)

본 어플리케이션은 엄격한 모바일 소프트웨어 품질 표준 및 핵심 제약 조건을 단 1%의 누수 없이 100% 무결하게 수용하여 설계되었습니다.

| 평가 요구사항 항목 | 시스템 구현 상태 및 매핑 컴포넌트 | 소프트웨어 공학적 무결성 증명 (Status) |
| :--- | :--- | :---: |
| **Multi-Fragment & 백스택** | `HomeFragment`와 `DashboardFragment`를 독립 컴포넌트로 분리하고, Jetpack `OnBackPressedCallback` 디스패처를 통합하여 하드웨어 제스처 뒤로가기 시 가상 머신의 스택 유실을 차단하는 역전이 메커니즘 수립. | **완료 (Pass)** |
| **RecyclerView 구조** | 고성능 뷰 재사용을 수호하기 위해 `TravelAdapter` 및 `TravelViewHolder`를 순수 바이트코드 사양으로 직접 커스텀 구현. 단일 터치 시 고유 주키 번호(`RECORD_NO`)를 패킷화하여 명시적 인텐트 런타임 라우팅 연동. | **완료 (Pass)** |
| **순수 SQLite CRUD** | 외부 ORM 라이브러리(`Room` 등)를 원천 배제하고 `SQLiteOpenHelper`를 상속한 `DBHelper` 아키텍처 직접 코딩. `getRecordById`와 `updateRecord`를 포함한 원자적 트랜잭션 수립으로 앱 종료 후에도 무결한 영속성 유지. | **완료 (Pass)** |
| **옵션 및 컨텍스트 메뉴** | 상단 옵션 메뉴 2종(실시간 검색, 앱 정보 오버플로우) 구현 및 리스트 롱클릭 제스처를 인터셉트하는 `AlertDialog` 기반의 커스텀 다중 선택 컨텍스트 리스너 파이프라인 탑재. | **완료 (Pass)** |
| **갤러리 사진 Intent** | 최신 안드로이드 보안 규격에 부합하는 `ActivityResultContracts.StartActivityForResult` 계약 팩토리를 구동하여 가상 이미지 가 주소(URI)를 안전하게 수급하고, 상세 화면 뷰포트 가시성을 동적으로 통제. | **완료 (Pass)** |
| **Google Maps API 활용** | `SupportMapFragment` 비동기 라이프사이클 링킹(`getMapAsync`) 기법을 결합하여, 로컬 저장소로부터 역직렬화된 위경도 랜드마크 핀 마커 복원 및 카메라 줌인 가속 렌더링 완수. (**추가 구현 가산점 요건**) | **완료 (Pass)** |
| **코루틴 비동기 처리** | On-Device 딥러닝 추론 엔진 구동 시, 가상 머신의 메인 UI 스레드 락(ANR 예외)을 원천 차단하기 위해 백엔드 워커 스레드로 연산을 격리하는 `lifecycleScope.launch` 코루틴 파이프라인 정합. (**추가 구현 가산점 요건**) | **완료 (Pass)** |

---

## 🛠️ 2. 핵심 기술 스택 및 공학적 해결 전략 (Technical Highlights)

### ⚙️ NoActionBar 제약 우회를 위한 독립형 머티리얼 툴바(Toolbar) 도입
* **직면 과제 (Challenge)**: 어플리케이션의 세련된 UI 스타일 유지를 위해 전역 테마 설정을 `NoActionBar` 규격으로 바인딩함에 따라, 시스템 기본 상단 바가 완전히 제거되어 소스 코드 단에서 지시하는 내비게이션 화살표와 타이틀 텍스트가 화면에 드로잉되지 못하는 비주얼 누수 결함 발생.
* **해결 전략 (Solution)**: `activity_edit.xml` 선형 배치 레이아웃의 가장 첫 번째 자식 노드로 독립형 `androidx.appcompat.widget.Toolbar` 위젯을 물리적으로 전개하여 경계 상자(Bounding Box) 간섭을 차단함. 이어서 `onCreate` 타이밍에 `setSupportActionBar(toolbarEdit)` 구문을 바인딩하여 시스템의 공식 액션바 인터프리터로 강제 대체 매핑함. 이 조치를 통해 전역 테마 스타일을 손상시키지 않고 안전하게 `android.R.id.home` 상단 뒤로가기 화살표 버튼 활성화 및 상황별 다형성 타이틀 문자열을 멱등성 있게 표출하는 데 성공함.

### 🔄 생명주기(onResume) 옵저버 패턴 기반 실시간 단방향 데이터 동기화
* **직면 과제 (Challenge)**: 사용자가 `EditActivity` 단에서 가변 여행 데이터를 수정(`UPDATE`)하거나 신규 저장(`INSERT`)한 후 `finish()`되어 복귀했을 때, 최하단 백스택에 대기 중이던 홈 화면의 리사이클러뷰 목록이 실시간으로 리프레시되지 않아 프론트엔드 UI 노드와 백엔드 로컬 데이터베이스 간의 상태 불일치(State Inconsistency) 취약점 노출.
* **해결 전략 (Solution)**: 호출원과 피호출원 간의 강한 결합도를 유발하는 레거시 결과 수급 API 대신, 안드로이드 프레임워크가 보장하는 포그라운드 진입 타이밍인 `onResume()` 라이프사이클 콜백을 데이터 동기화 트리거로 채택함. 화면이 사용자 시각 영역에 재진입하는 순간 단방향 진실 공급원(SSOT) 규칙을 수호하도록 `loadTravelRecords()` 파이프라인을 연쇄 강제 구동함. SQLite 커널로부터 항상 최신 튜플 세트를 안전하게 풀링(Pulling)하여 어댑터의 데이터셋을 런타임 스wap 리프레시하도록 조율함으로써, 자원 경합을 배제한 완벽한 실시간 상태 동기화를 구현함.

### 🛡️ 2단계 확인 분기 인터셉터(Two-Phase Verification)를 통한 삭제 방화벽 구축
* **직면 과제 (Challenge)**: 목록 아이템 터치 제스처 시 단순 롱클릭과 동시 작동하는 무분별한 데이터 파괴(Delete SQL) 연산은 사용자의 물리적 오터치 및 런타임 메모리 인덱스 포인터 꼬임 현상을 유발하여 영구 적재 데이터 유실 취약점을 가속화함.
* **해결 전략 (Solution)**: 리사이클러뷰의 뷰 재사용 패턴 하에서도 참조 안전성을 100% 수호할 수 있도록 익명 함수 람다 리스너 인터페이스와 안드로이드 표준 모달 인프라인 `AlertDialog.Builder`를 결합함. 아이템 롱클릭 시 1차적으로 다중 선택형 가상 팝업을 전면에 드로잉하여 '상세 조회'와 '삭제' 트랙을 분기 라우팅함. 특히 비가역적인 레코드 파괴가 수반되는 '삭제' 트리거 터치 시, 한 번 더 독립적인 최종 확인 다이어로그(`showFinalDeleteCheckDialog`)를 가동하여 사용자 피드백을 검증하는 2단계 보안 방화벽을 확립함.

---

## 📂 3. 디렉토리 구조 및 핵심 컴포넌트 기능 명세 (Project Structure)

```text
com.example.mobileproject/
│
├── data/
│   ├── TravelRecord.kt       # 데이터 전송 객체(DTO) 엔티티 명세 (place, visitDate, memo, photoUri, hashtag)
│   └── DBHelper.kt           # SQLiteOpenHelper 코어 클래스 (Atomic 단위의 CRUD 질의문 통제 및 자원 닫기 관리)
│
├── ui/
│   ├── MainActivity.kt       # BottomNavigationView 제어 및 상단 툴바 옵션 메뉴 바인딩 호스트 컨트롤러
│   ├── HomeFragment.kt       # 리사이클러뷰 바인딩, 빈 목록 피드백(tvEmptyMessage) 및 롱클릭 2단계 다이얼로그 제어
│   ├── DashboardFragment.kt  # 로컬 SQLite 행 카운트 동적 수식 연산 및 On-Device AI 분석 확장 가산점 탭
│   ├── TravelAdapter.kt      # ViewHolder 재사용 알고리즘 및 런타임 데이터 스왑(updateData) 정합 어댑터
│   └── EditActivity.kt       # SupportMapFragment 비동기 연동 및 TFLite 코루틴 병렬 분석 결합 핵심 액티비티
│
└── utils/
    └── FoodImageClassifier.kt # 온디바이스 딥러닝 인터프리터 래퍼 코어 (비동기 이미지 텐서 전처리 및 추론 실행)
