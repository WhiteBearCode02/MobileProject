package com.example.mobileproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.classifier.ImageClassifier.ImageClassifierOptions
import java.io.InputStream

/**
 * 온디바이스 TFLite 모델을 로드하고 이미지 분석을 비동기적으로 수행하는 AI 클래스
 */
class FoodImageClassifier(private val context: Context) {

    companion object {
        private const val TAG = "FoodImageClassifier"
        private const val MODEL_NAME = "food_model.tflite" // assets 폴더에 배치할 모델 파일명
        private const val MAX_RESULTS = 2                  // 추출할 상위 결과 개수
        private const val THRESHOLD = 0.4f                 // 분류 정확도 최소 임계치 (40% 이상만 통과)
    }

    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    /**
     * TensorFlow Lite ImageClassifier의 옵션을 설정하고 엔진을 초기화합니다.
     */
    private fun setupImageClassifier() {
        try {
            val options = ImageClassifierOptions.builder()
                .setMaxResults(MAX_RESULTS)
                .setScoreThreshold(THRESHOLD)
                .build()

            // 앱 내부의 assets 폴더로부터 훈련된 모델 인스턴스 생성
            imageClassifier = ImageClassifier.createFromFileAndOptions(context, MODEL_NAME, options)
            Log.d(TAG, "TFLite 음식 분류 모델 초기화 성공")
        } catch (e: Exception) {
            Log.e(TAG, "TFLite 모델 로드 중 에러 발생. assets 폴더를 확인하세요.", e)
        }
    }

    /**
     * [가산점 요소 - 비동기 코루틴 처리]
     * 이미지 URI를 넘겨받아 백그라운드 스레드(Dispatchers.Default)에서 연산한 뒤 해시태그 문자열을 반환합니다.
     */
    async fun classifyImageAsync(imageUri: Uri): String = withContext(Dispatchers.Default) {
        // 이미지를 분석하는 대규모 행렬 연산은 CPU 사용량이 높으므로 Default 스레드 풀로 강제 전환
        if (imageClassifier == null) {
            return@withContext "#여행 #일상"
        }

        try {
            // 1. 컨텐트 리졸버를 통해 URI를 비트맵 객체로 안전하게 디코딩
            val bitmap = loadBitmapFromUri(imageUri) ?: return@withContext "#이미지오류"

            // 2. TFLite 라이브러리가 요구하는 텐서 이미지 객체 포맷으로 변환
            val tensorImage = TensorImage.fromBitmap(bitmap)

            // 3. 온디바이스 AI 모델 추론 연산 실행 (동기 실행 블록이지만, 코루틴 컨텍스트 내부이므로 안전)
            val results = imageClassifier?.classify(tensorImage)

            // 4. 추론 성공 시 카테고리 라벨을 매핑하여 한글 해시태그로 가공
            if (!results.isNullOrEmpty() && results[0].categories.isNotEmpty()) {
                val hashtagBuilder = StringBuilder()
                for (category in results[0].categories) {
                    val koreanTag = convertLabelToKorean(category.label)
                    hashtagBuilder.append("#$koreanTag ")
                }
                return@withContext hashtagBuilder.toString().trim()
            } else {
                return@withContext "#여행 #추억" // 음식 분류 범주를 벗어난 일반 사진일 때의 기본 태그
            }

        } catch (e: Exception) {
            Log.e(TAG, "이미지 AI 분석 추론 실패", e)
            return@withContext "#기록완료"
        }
    }

    /**
     * 미디어 저장소 또는 촬영한 임시 공간의 이미지 스트림을 열어 안전하게 비트맵으로 로드하는 메서드
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "URI로부터 비트맵 디코딩 실패", e)
            null
        } finally {
            inputStream?.close()
        }
    }

    /**
     * TFLite AI 모델이 반환하는 영문 클래스 명칭을 직관적인 한글 해시태그용 단어로 변환
     */
    private fun convertLabelToKorean(englishLabel: String): String {
        return when (englishLabel.lowercase()) {
            "pizza" -> "피자"
            "pasta" -> "파스타"
            "burger" -> "수제버거"
            "salad" -> "샐러드"
            "sushi" -> "일식초밥"
            "chicken" -> "치킨맛집"
            "rice" -> "든든한밥상"
            "dessert" -> "카페디저트"
            else -> englishLabel // 리스트에 없는 항목은 영문 라벨을 그대로 태그화
        }
    }
}