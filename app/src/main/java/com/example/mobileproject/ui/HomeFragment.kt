package com.example.mobileproject.ui

import android.app.AlertDialog
import android.content.Intent
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
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * [HomeFragment]: 저장된 로컬 여행 기록 리스트를 화면에 드로잉하고,
 * 신규 추가 플로팅 버튼(FAB) 인텐트 화면 전환 및 롱클릭 삭제 트랜잭션을 통제하는 홈 컨트롤러 계층.
 */
class HomeFragment : Fragment() {

    private lateinit var rvTravelList: RecyclerView
    private lateinit var fabAddRecord: FloatingActionButton
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

        // [버튼 컴포넌트 바인딩]: XML에 구축한 플로팅 액션 버튼 포인터 확보
        fabAddRecord = view.findViewById(R.id.fabAddRecord)

        setupRecyclerView()
        setupButtons() // 버튼 클릭 리스너 셋업 파이프라인 가동

        return view
    }

    /**
     * [onResume]: 프래그먼트 뷰포트가 사용자에게 활성화(포커싱)될 때마다 상위 액티비티로부터
     * 포커스를 돌려받는 포그라운드(Foreground) 전이 타이밍을 옵저버 패턴 형태로 인터셉트합니다.
     * EditActivity에서 저장을 완료하고 복귀하는 순간, 컴파일러가 이 루틴을 가동하여 리스트 데이터를 멱등성 있게 실시간 자동 최신화합니다.
     */
    override fun onResume() {
        super.onResume()
        loadTravelRecords() // [실시간 화면 동기화 기능 핵심 지점]
    }

    /**
     * [setupButtons]: 화면 내에 배치된 인터랙션 버튼들의 클릭 이벤트 루틴을 바인딩하는 메서드.
     */
    private fun setupButtons() {
        // [화면 전환 기능 연동]: FAB 클릭 시 명시적 인텐트를 생성하여 여행 추가 및 지도 화면(EditActivity)으로 스왑 전이
        fabAddRecord.setOnClickListener {
            Toast.makeText(requireContext(), "새로운 여행 기록 작성 화면으로 전이합니다.", Toast.LENGTH_SHORT).show()

            // 프래그먼트 콘텍스트 환경이므로 requireContext()를 기점으로 인텐트 스트림 수립
            val intent = Intent(requireContext(), EditActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * [setupRecyclerView]: 아이템 클릭(상세 전이) 및 롱클릭 기반 팝업 다이얼로그 영구 제거 트랜잭션 브릿지 결합 메서드.
     */
    private fun setupRecyclerView() {
        rvTravelList.layoutManager = LinearLayoutManager(requireContext())

        travelAdapter = TravelAdapter(
            records = emptyList(),
            onItemClick = { record ->
                // 단일 아이템 클릭 시 작동할 액션 명세 스코프
                Toast.makeText(requireContext(), "[${record.place}] 기록 정보 상세 조회", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { record, _ ->
                // 롱클릭 포착 시 팝업 다이얼로그를 트리거하여 삭제 트랜잭션 안전 가동
                showDeleteConfirmationDialog(record)
            }
        )
        rvTravelList.adapter = travelAdapter
    }

    /**
     * [showDeleteConfirmationDialog]: 리사이클러뷰 롱클릭 컨텍스트 이벤트 발생 시 AlertDialog로 원자적 삭제 행위를 제어하는 알고리즘.
     */
    private fun showDeleteConfirmationDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("여행 기록 삭제")
            setMessage("[${record.place}] 여행 일기를 영구히 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.")
            setIcon(android.R.drawable.ic_dialog_alert)

            setPositiveButton("삭제") { dialog, _ ->
                val targetNo = record.no
                if (targetNo != null) {
                    val deleteResult = dbHelper.deleteRecord(targetNo)
                    if (deleteResult > 0) {
                        Toast.makeText(requireContext(), "성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadTravelRecords() // [삭제 데이터 실시간 동적 새로고침 구현]
                    } else {
                        Toast.makeText(requireContext(), "데이터베이스 제거 연산 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "유효하지 않은 고유 번호 식별자입니다.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    /**
     * [loadTravelRecords]: SQLite DB로부터 데이터 배열 구조체를 읽어와 리스트 UI를 멱등성 있게 새로고침합니다.
     */
    private fun loadTravelRecords() {
        try {
            // SQLite 커널로부터 테이블 내부 데이터 로드
            val records = dbHelper.getAllRecords()

            // 데이터 셋의 유무와 상관없이 어댑터 내부에 구조체를 전이시키고 뷰포트를 실시간 동적 리프레시 처리합니다.
            if (records.isEmpty()) {
                travelAdapter.updateData(emptyList())
            } else {
                travelAdapter.updateData(records)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "데이터 저장소 로드 중 심각한 예외가 유발되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}