package com.example.mobileproject.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mobileproject.R
import com.example.mobileproject.data.DBHelper
import com.example.mobileproject.data.TravelRecord
import com.example.mobileproject.utils.FoodImageClassifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

/**
 * [EditActivity]: 여행 기록을 신규 작성하거나 수정하며, Google Maps SDK 시각화 및 On-Device TFLite AI 추론을 통제하는 컨트롤러.
 * XML 레이아웃 구조체 명세 개편에 따라 mapFragment 인스턴스 참조 무결성을 보장하도록 정합함.
 */
class EditActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var etPlace: EditText
    private lateinit var etDate: EditText
    private lateinit var etMemo: EditText
    private lateinit var ivSelectedPhoto: ImageView
    private lateinit var tvAiHashtagResult: TextView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSave: Button

    private lateinit var dbHelper: DBHelper
    private lateinit var imageClassifier: FoodImageClassifier

    private var selectedPhotoUri: Uri? = null
    private var generatedHashtag: String = ""

    // 비동기 콜백을 통해 수급된 구글 맵 핵심 엔진 컨트롤러 인스턴스 격리 참조
    private var currentGoogleMap: GoogleMap? = null

    // 최신 Activity Result API를 이용한 갤러리 이미지 풀링 계약 구조
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedPhotoUri = data?.data
            selectedPhotoUri?.let { uri ->
                ivSelectedPhoto.visibility = View.VISIBLE
                ivSelectedPhoto.setImageURI(uri)

                // 비동기 코루틴 추론 엔진 가동
                analyzeImageWithAI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        dbHelper = DBHelper(this)
        imageClassifier = FoodImageClassifier(this)

        initViews()
        setupGoogleMap()
        setupListeners()
    }

    private fun initViews() {
        etPlace = findViewById(R.id.etPlace)
        etDate = findViewById(R.id.etDate)
        etMemo = findViewById(R.id.etMemo)
        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto)
        tvAiHashtagResult = findViewById(R.id.tvAiHashtagResult)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnSave = findViewById(R.id.btnSave)
    }

    /**
     * [setupGoogleMap]: XML 레이아웃 컨텍스트 내부의 SupportMapFragment 노드를 동적 파싱하여,
     * 구글 클라우드 보안 게이트웨이 인증 스레드 백엔드 파이프라인을 가동하는 인프라 셋업 메서드.
     */
    private fun setupGoogleMap() {
        // [참조 결함 교정 완료]: activity_edit.xml에 새로 정의된 mapFragment ID 자원을 안전하게 추출
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment

        // 비동기 옵저버 바인딩: 구글 인증 및 렌더링 컨텍스트가 가용 상태에 도달하면 이 클래스의 onMapReady 루틴 트리거
        mapFragment.getMapAsync(this)
    }

    /**
     * [onMapReady]: 구글 클라우드 인증 플랫폼을 통과하고 벡터 지오 타일 데이터 수급이 안전하게 완료되면 호출되는 비동기 라이프사이클 콜백.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        this.currentGoogleMap = googleMap

        // 순천향대학교 정밀 위경도 데이터 할당
        val soonchunhyang = LatLng(36.7691, 126.9348)

        // 구글 맵 전용 렌더링 파이프라인으로 지리 좌표 토스
        moveMapToLocation(soonchunhyang, "순천향대학교")
    }

    /**
     * 특정 지리 좌표로 지도를 천천히 전이시키고 가산점용 단일 마커를 렌더링하는 메서드
     */
    private fun moveMapToLocation(latLng: LatLng, title: String) {
        currentGoogleMap?.let { map ->
            // 마커 옵션 빌드: 구글 표준 레이아웃 컴포넌트에 바인딩할 마커 명세 가공
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet("기말 프로젝트 여행 기록 장소")

            // 1. 구글 맵 그래픽 엔진 캔버스 단에 랜드마크 핀 마커 추가
            map.addMarker(markerOptions)

            // 2. 카메라Update 가속 팩토리를 구동하여 지정 위경도로 뷰포트를 이동시키고, 정밀 확대 스펙인 15f를 정합 수행
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun setupListeners() {
        btnSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            photoPickerLauncher.launch(intent)
        }

        btnSave.setOnClickListener {
            val place = etPlace.text.toString().trim()
            val date = etDate.text.toString().trim()
            val memo = etMemo.text.toString().trim()
            val photoUriStr = selectedPhotoUri?.toString() ?: ""

            if (place.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "여행지명과 날짜는 필수 입력 사항입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newRecord = TravelRecord(
                place = place,
                visitDate = date,
                memo = memo,
                photoUri = photoUriStr,
                hashtag = generatedHashtag
            )

            val insertId = dbHelper.insertRecord(newRecord)
            if (insertId != -1L) {
                Toast.makeText(this, "여행 일기가 정상 보존되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "로컬 영구 데이터 적재 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeImageWithAI(uri: Uri) {
        tvAiHashtagResult.text = "On-Device TFLite 분석 엔진 구동 중..."
        lifecycleScope.launch {
            val tags = imageClassifier.classifyImageAsync(uri)
            generatedHashtag = tags
            tvAiHashtagResult.text = "AI 분석 완료 태그: $generatedHashtag"
        }
    }
}