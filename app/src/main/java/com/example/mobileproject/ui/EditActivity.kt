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
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.launch

/**
 * 여행 기록을 신규 작성하거나 수정하며, 카카오 지도 v2 시각화 및 AI 해시태그 분석을 통제하는 액티비티
 */
class EditActivity : AppCompatActivity() {

    private lateinit var etPlace: EditText
    private lateinit var etDate: EditText
    private lateinit var etMemo: EditText
    private lateinit var ivSelectedPhoto: ImageView
    private lateinit var tvAiHashtagResult: TextView
    private lateinit var kakaoMapView: MapView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSave: Button

    private lateinit var dbHelper: DBHelper
    private lateinit var imageClassifier: FoodImageClassifier

    private var selectedPhotoUri: Uri? = null
    private var generatedHashtag: String = ""
    private var currentKakaoMap: KakaoMap? = null

    // 최신 Activity Result API를 이용한 갤러리/카메라 이미지 풀링 계약 구조
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
        setupKakaoMap()
        setupListeners()
    }

    private fun initViews() {
        etPlace = findViewById(R.id.etPlace)
        etDate = findViewById(R.id.etDate)
        etMemo = findViewById(R.id.etMemo)
        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto)
        tvAiHashtagResult = findViewById(R.id.tvAiHashtagResult)
        kakaoMapView = findViewById(R.id.kakaoMapView)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnSave = findViewById(R.id.btnSave)
    }

    /**
     * 카카오 맵 v2 정식 패키지 규격에 따른 렌더링 컨텍스트 가동
     */
    private fun setupKakaoMap() {
        kakaoMapView.start(object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                currentKakaoMap = kakaoMap

                // 기본 뷰포트를 학부 연구실 및 캡스톤 무대인 '순천향대학교' 좌표로 정밀 셋업
                val soonchunhyang = LatLng.from(36.7691, 126.9348)
                moveMapToLocation(soonchunhyang, "순천향대학교")
            }
        })
    }

    /**
     * 특정 지리 좌표로 지도를 천천히 전이시키고 가산점용 단일 마커를 렌더링하는 메서드
     */
    private fun moveMapToLocation(latLng: LatLng, title: String) {
        currentKakaoMap?.let { map ->
            // [오류 수정]: 카카오 지도 v2 명세상 줌 레벨 파라미터는 Float 형식이 아닌 정수형(Int)인 15를 취해야 컴파일러 에러가 해결됩니다.
            map.moveCamera(CameraUpdateFactory.newCenterPosition(latLng, 15))

            // 디폴트 핀 아이콘 스타일 빌드업
            // [오류 수정]: 카카오 맵 v2의 스타일 레이어 매커니즘 구조 정합성을 위해 호환 핀 리소스를 생성 바인딩합니다.
            val style = map.labelManager?.addLabelStyles(
                LabelStyles.from(LabelStyle.from(android.R.drawable.ic_dialog_map))
            )
            val options = LabelOptions.from(latLng).setStyles(style).setTexts(title)

            // 지도 최상단 기본 레이어에 랜드마크 라벨(마커) 추가
            map.labelManager?.layer?.addLabel(options)
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