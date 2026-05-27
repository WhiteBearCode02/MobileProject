package com.example.mobileproject.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileproject.R
import com.example.mobileproject.data.DBHelper
import com.example.mobileproject.data.TravelRecord
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * [HomeFragment]: 저장된 로컬 여행 기록 리스트를 화면에 드로잉하고,
 * 신규 추가 및 상세 조회 인텐트 화면 전환과 롱클릭 삭제 트랜잭션을 통제하는 홈 컨트롤러 계층.
 */
class HomeFragment : Fragment() {

    private lateinit var rvTravelList: RecyclerView
    private lateinit var fabAddRecord: FloatingActionButton
    private lateinit var travelAdapter: TravelAdapter
    private lateinit var dbHelper: DBHelper
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyList: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        dbHelper = DBHelper(requireContext())
        rvTravelList = view.findViewById(R.id.rvTravelList)
        fabAddRecord = view.findViewById(R.id.fabAddRecord)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyList = view.findViewById(R.id.tvEmptyList)

        setupRecyclerView()
        setupButtons()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadTravelRecords()
    }

    private fun setupButtons() {
        // 새 기록 추가 시에는 고유 번호(no)를 인텐트에 넣지 않고 기본형으로 호출 (새로 만들기 모드)
        fabAddRecord.setOnClickListener {
            val intent = Intent(requireContext(), EditActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * [setupRecyclerView]: 아이템 클릭 시 고유 식별자를 첨부하여 EditActivity로 상세 전이하는 파이프라인 정합.
     */
    private fun setupRecyclerView() {
        rvTravelList.layoutManager = LinearLayoutManager(requireContext())

        travelAdapter = TravelAdapter(
            onItemClick = { record ->
                // [상세 보기 화면 전환 기능 핵심 지점]: 클릭한 아이템의 고유 DB 식별자(no)를 인텐트에 바인딩
                val targetNo = record.no
                if (targetNo != null) {
                    val intent = Intent(requireContext(), EditActivity::class.java).apply {
                        putExtra("RECORD_NO", targetNo) // 고유 번호를 엑스트라 패킷에 직렬화 인입
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "유효하지 않은 레코드 식별자입니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onItemLongClick = { record, _ ->
                showDeleteConfirmationDialog(record)
            }
        )
        rvTravelList.adapter = travelAdapter
    }

    private fun showDeleteConfirmationDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("여행 기록 삭제")
            setMessage("[${record.place}] 여행 일기를 영구히 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.")
            setIcon(android.R.drawable.ic_dialog_alert)

            setPositiveButton("삭제") { dialog, _ ->
                val targetNo = record.no
                if (targetNo != null) {
                    // [개선]: 코루틴을 사용하여 백그라운드에서 DB 삭제
                    lifecycleScope.launch {
                        val deleteResult = dbHelper.deleteRecord(targetNo)
                        if (deleteResult > 0) {
                            Toast.makeText(requireContext(), "성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            loadTravelRecords() // 목록 새로고침
                        } else {
                            Toast.makeText(requireContext(), "데이터베이스 제거 연산 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun loadTravelRecords() {
        progressBar.isVisible = true
        rvTravelList.isVisible = false
        tvEmptyList.isVisible = false

        lifecycleScope.launch {
            try {
                val records = dbHelper.getAllRecords()
                if (records.isEmpty()) {
                    tvEmptyList.isVisible = true
                    rvTravelList.isVisible = false
                    travelAdapter.submitList(emptyList())
                } else {
                    tvEmptyList.isVisible = false
                    rvTravelList.isVisible = true
                    travelAdapter.submitList(records)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "데이터 저장소 로드 중 심각한 예외가 유발되었습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.isVisible = false
            }
        }
    }
}