package com.example.mobileproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileproject.R
import com.example.mobileproject.data.DBHelper

/**
 * 저장된 여행 기록 목록을 화면에 출력하는 홈 프래그먼트
 */
class HomeFragment : Fragment() {

    private lateinit var rvTravelList: RecyclerView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var travelAdapter: TravelAdapter
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 데이터베이스 헬퍼 및 컴포넌트 초기화
        dbHelper = DBHelper(requireContext())
        rvTravelList = view.findViewById(R.id.rvTravelList)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)

        setupRecyclerView()
        return view
    }

    // 프래그먼트가 사용자에게 보일 때마다 로컬 DB 데이터를 최신화하기 위해onResume 활용
    override fun onResume() {
        super.onResume()
        loadTravelRecords()
    }

    private fun setupRecyclerView() {
        rvTravelList.layoutManager = LinearLayoutManager(requireContext())

        // 어댑터 직접 구현 및 클릭 이벤트 리스너 인터페이스 전달 (교수님 필수 요구사항)
        travelAdapter = TravelAdapter(
            records = emptyList(),
            onItemClick = { record ->
                // [추후 구현] 상세 액티비티로 이동하는 Intent 로직 정의 예정
                Toast.makeText(requireContext(), "${record.place} 상세 화면으로 이동", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { record, view ->
                // [추후 구현] 길게 눌렀을 때 수정/삭제 컨텍스트 메뉴 컨텍스트 바인딩 예정
                Toast.makeText(requireContext(), "${record.place} 롱클릭 감지", Toast.LENGTH_SHORT).show()
            }
        )
        rvTravelList.adapter = travelAdapter
    }

    /**
     * SQLite DB로부터 데이터를 안전하게 읽어와 리스트 UI를 새로고침합니다.
     */
    private fun loadTravelRecords() {
        try {
            val records = dbHelper.getAllRecords()
            if (records.isEmpty()) {
                tvEmptyMessage.visibility = View.VISIBLE
                rvTravelList.visibility = View.GONE
            } else {
                tvEmptyMessage.visibility = View.GONE
                rvTravelList.visibility = View.VISIBLE
                travelAdapter.updateData(records)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}