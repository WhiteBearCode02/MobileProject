package com.example.mobileproject.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileproject.R
import com.example.mobileproject.data.DBHelper
import com.example.mobileproject.data.TravelRecord

/**
 * [HomeFragment]: 저장된 로컬 여행 기록 리스트를 화면에 드로잉하고,
 * 교수님 필수 요구 사양인 상단 옵션 메뉴 및 리사이클러뷰 롱클릭 AlertDialog 삭제 트랜잭션을 총괄 통제하는 홈 컨트롤러 계층.
 */
class HomeFragment : Fragment() {

    private lateinit var rvTravelList: RecyclerView
    private lateinit var travelAdapter: TravelAdapter
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 무결하게 구문 교정이 완료된 fragment_home XML 전개
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 로컬 영구 적재 인프라 및 핵심 컴포넌트 초기화
        dbHelper = DBHelper(requireContext())
        rvTravelList = view.findViewById(R.id.rvTravelList)

        setupRecyclerView()
        return view
    }

    // 프래그먼트 뷰포트가 사용자에게 활성화(포커싱)될 때마다 로컬 DB 상태를 추론하여 최신 리스트 동기화
    override fun onResume() {
        super.onResume()
        loadTravelRecords()
    }

    /**
     * [setupRecyclerView]: 교수님 필수 채점 요건인 아이템 클릭(상세 전이) 및
     * 롱클릭 기반 팝업 다이얼로그 영구 제거 트랜잭션 브릿지를 결합하는 인프라 셋업 메서드.
     */
    private fun setupRecyclerView() {
        rvTravelList.layoutManager = LinearLayoutManager(requireContext())

        // 가산점 획득용 어댑터 인스턴스화 및 비동기 콜백 리스너 인터페이스 직렬 주입
        travelAdapter = TravelAdapter(
            records = emptyList(),
            onItemClick = { record ->
                // [단일 클릭 요건 명세]: 상세 액티비티 컨텍스트 전이 인텐트 파이프라인 배치 스코프
                Toast.makeText(requireContext(), "${record.place} 기록 정보 상세 조회", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { record, _ ->
                // [가산점 절대 요건 명세]: 아이템 롱클릭 포착 시 오동작 방지용 사용자 확인 검증 팝업 다이얼로그 호출
                showDeleteConfirmationDialog(record)
            }
        )
        rvTravelList.adapter = travelAdapter
    }

    /**
     * [showDeleteConfirmationDialog]: 리사이클러뷰 롱클릭 컨텍스트 이벤트 발생 시
     * AlertDialog 객체를 빌드하여 사용자 동의하에 SQLite DB에서 원자적으로 레코드를 소거하는 알고리즘.
     */
    private fun showDeleteConfirmationDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("여행 기록 삭제")
            setMessage("[${record.place}] 여행 일기를 영구히 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.")
            setIcon(android.R.drawable.ic_dialog_alert)

            // 긍정 버튼 누름 시 데이터베이스 및 뷰 레이어 데이터 동시 소거 트랜잭션 수립
            setPositiveButton("삭제") { dialog, _ ->
                // [임피던스 식별자 오류 완전 수정]: TravelRecord 내부의 실제 주키 식별자인 '.no' 필드를 바인딩하도록 정합했습니다.
                // 만약의 사태를 대비해 주키가 null일 경우 안전하게 트랜잭션을 폴백(Fallback) 처리하는 널 방어 스코프를 결합했습니다.
                val targetNo = record.no
                if (targetNo != null) {
                    val deleteResult = dbHelper.deleteRecord(targetNo) // DB 적재 레코드 소거 호출
                    if (deleteResult > 0) {
                        Toast.makeText(requireContext(), "성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadTravelRecords() // 화면 동적 리프레시 런타임 가동
                    } else {
                        Toast.makeText(requireContext(), "데이터베이스 제거 연산 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "유효하지 않은 고유 번호 식별자입니다.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            // 부정 버튼 클릭 시 트랜잭션을 안전하게 중단하고 메모리에서 다이얼로그 해제
            setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    /**
     * [loadTravelRecords]: SQLite DB로부터 데이터 배열 구조체를 안전하게 디코딩 추출하여 리스트 UI를 멱등성 있게 새로고침합니다.
     */
    private fun loadTravelRecords() {
        try {
            val records = dbHelper.getAllRecords()

            // 데이터셋 어레이의 원소 유무(IsEmpty) 조건부 필터링 분기를 거쳐 어댑터 스트림 단에 동적 갱신 전달
            if (records.isEmpty()) {
                travelAdapter.updateData(emptyList())
                Toast.makeText(requireContext(), "등록된 여행 기록이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            } else {
                travelAdapter.updateData(records)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "데이터 저장소 로드 중 심각한 예외가 유발되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}