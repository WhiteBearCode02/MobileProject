package com.example.mobileproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mobileproject.R
import com.example.mobileproject.data.DBHelper

/**
 * [DashboardFragment]: 영구 저장소(SQLite)에 누적된 여행 데이터를 역추적하여
 * 실시간 통계 수치를 연산하고, 핵심 분석 런타임을 통제하는 고도화 가산점 탭 컨트롤러 계층.
 */
class DashboardFragment : Fragment() {

    private lateinit var tvTotalCount: TextView
    private lateinit var btnRefreshStats: Button
    private lateinit var btnAiSummary: Button
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

        // 2. AI 분석 요약 버튼 기능 구현: TFLite 및 캡스톤 프로젝트 연계용 추가 비즈니스 로직 입구 확보
        btnAiSummary.setOnClickListener {
            // [추후 고도화 확장 영역]: 리사이클러뷰 전체 데이터셋을 풀링하여 AI 인사이트 칩을 도출하는 루틴 배치 스코프
            Toast.makeText(requireContext(), "온디바이스 TFLite 트렌드 분석 엔진을 가동합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * [calculateAndRefreshStatistics]: SQLite 레코드 배열 구조체의 카운트 개수를 추론하여
     * 텍스트 레이어 캔버스에 실시간 투사하는 집계 알고리즘.
     */
    private fun calculateAndRefreshStatistics() {
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