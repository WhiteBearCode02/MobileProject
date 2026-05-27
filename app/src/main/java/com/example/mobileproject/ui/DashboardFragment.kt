package com.example.mobileproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobileproject.R
import com.example.mobileproject.data.DBHelper
import kotlinx.coroutines.launch

/**
 * [DashboardFragment]: 영구 저장소(SQLite)에 누적된 여행 데이터를 역추적하여
 * 실시간 통계 수치를 연산하고, 핵심 분석 런타임을 통제하는 고도화 가산점 탭 컨트롤러 계층.
 */
class DashboardFragment : Fragment() {

    private lateinit var tvTotalCount: TextView
    private lateinit var btnRefreshStats: Button
    private lateinit var btnAiSummary: Button
    private lateinit var tvAiAnalysisResult: TextView // [추가] 분석 결과 TextView
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 정의된 fragment_dashboard XML 레이아웃 리소스 전개
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // 로컬 데이터 인프라 및 UI 컴포넌트 메모리 포인터 확보
        dbHelper = DBHelper(requireContext())
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        btnRefreshStats = view.findViewById(R.id.btnRefreshStats)
        btnAiSummary = view.findViewById(R.id.btnAiSummary)
        tvAiAnalysisResult = view.findViewById(R.id.tvAiAnalysisResult) // [추가] ID 바인딩

        setupListeners()
        return view
    }

    // 사용자가 하단 네비게이션바를 통해 대시보드 탭을 터치할 때마다 실시간으로 최신 데이터 집계 연산 수행
    override fun onResume() {
        super.onResume()
        calculateAndRefreshStatistics()
    }

    /**
     * [setupListeners]: 대시보드 제어판 내 인터랙션 버튼들의 클릭 이벤트 루틴을 바인딩하는 메서드.
     */
    private fun setupListeners() {
        // 1. 통계 새로고침 버튼 기능 구현: 수동으로 DB 커널을 재조회하여 프론트엔드 동기화 트리거
        btnRefreshStats.setOnClickListener {
            calculateAndRefreshStatistics()
            Toast.makeText(requireContext(), "최신 여행 데이터 통계가 동기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // [개선] "여행분석" 버튼 기능 구현
        btnAiSummary.setOnClickListener {
            analyzeTravelTrends()
        }
    }

    /**
     * [추가] 여행 기록의 해시태그를 분석하여 트렌드를 보여주는 기능
     */
    private fun analyzeTravelTrends() {
        tvAiAnalysisResult.text = "AI가 여행 기록을 분석하고 있습니다..."

        lifecycleScope.launch {
            try {
                val allRecords = dbHelper.getAllRecords()
                if (allRecords.isEmpty()) {
                    tvAiAnalysisResult.text = "분석할 여행 기록이 없습니다."
                    return@launch
                }

                // 모든 해시태그를 분리하고, 각 태그의 빈도수를 계산
                val tagCounts = allRecords
                    .map { it.hashtag }
                    .filter { it.isNotEmpty() }
                    .flatMap { it.split(" ") }
                    .groupingBy { it }
                    .eachCount()

                if (tagCounts.isEmpty()) {
                    tvAiAnalysisResult.text = "분석할 AI 해시태그가 없습니다."
                    return@launch
                }

                // 가장 많이 등장한 상위 3개 태그를 추출
                val top3Tags = tagCounts.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .joinToString(separator = ", ") { it.key }

                tvAiAnalysisResult.text = "나의 여행 키워드는 [ ${top3Tags} ] 입니다."

            } catch (e: Exception) {
                tvAiAnalysisResult.text = "트렌드 분석 중 오류가 발생했습니다."
                Toast.makeText(requireContext(), "데이터 분석 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * [calculateAndRefreshStatistics]: SQLite 레코드 배열 구조체의 카운트 개수를 추론하여
     * 텍스트 레이어 캔버스에 실시간 투사하는 집계 알고리즘.
     */
    private fun calculateAndRefreshStatistics() {
        // [개선]: 코루틴을 사용하여 백그라운드에서 DB 조회
        lifecycleScope.launch {
            try {
                // DB 헬퍼 인터페이스를 통해 전체 적재된 행(Row) 어레이 추출
                val allRecords = dbHelper.getAllRecords()
                val totalSize = allRecords.size

                // [데이터 바인딩]: 뷰 객체의 문자열 속성에 정형 연산 수치 직렬 투사
                tvTotalCount.text = "총 ${totalSize}개의 기록이 보존 중입니다."

            } catch (e: Exception) {
                tvTotalCount.text = "통계 연산 엔진 가동 실패"
                Toast.makeText(requireContext(), "영속 데이터셋 디코딩 중 예외가 유발되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}